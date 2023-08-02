package eu.domibus.ext.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.ext.domain.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The complete rest endpoint integration tests
 */
@Transactional
public class PartyExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyExtResourceIT.class);

    // The endpoints to test
    public static final String TEST_ENDPOINT_RESOURCE = "/ext/party";
    public static final String TEST_ENDPOINT_RESOURCE_PROCESSES = TEST_ENDPOINT_RESOURCE + "/processes";
    public static final String TEST_ENDPOINT_RESOURCE_CERTIFICATE = TEST_ENDPOINT_RESOURCE + "/{partyName}/certificate";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    DomainTaskExecutor domainTaskExecutor;

    @BeforeEach
    public void setUp() throws XmlProcessingException, IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
        uploadPMode(SERVICE_PORT);
    }

    @Test
    public void getParties() throws Exception {

        PartyFilterRequestDTO partyFilterRequestDTO = new PartyFilterRequestDTO();

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_RESOURCE)
                        .content(asJsonString(partyFilterRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<PartyDTO> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(2, resultList.size());
    }

    @Test
    public void addAndRemoveParty() throws Exception {

        PartyDTO partyDTO = new PartyDTO();
        partyDTO.setName("newParty");
        PartyIdentifierDTO partyIdentifierDTO = new PartyIdentifierDTO();
        partyIdentifierDTO.setPartyId("partyId");
        PartyIdentifierTypeDTO partyIdType = new PartyIdentifierTypeDTO();
        partyIdType.setName("partyTypeName");
        partyIdType.setValue("partyTypeValue");
        partyIdentifierDTO.setPartyIdType(partyIdType);
        partyDTO.setIdentifiers(Arrays.asList(partyIdentifierDTO));
        partyDTO.setEndpoint("endPoint");
        partyDTO.setUserName("userName");

        // when
        MvcResult result = mockMvc.perform(post(TEST_ENDPOINT_RESOURCE)
                        .content(asJsonString(partyDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = objectMapper.readValue(result.getResponse().getContentAsString(), String.class);
        Assertions.assertEquals("Party having partyName=[" + partyDTO.getName() + "] created successfully!", content);

        partyDTO.setEndpoint("endPoint2");
        // when
        MvcResult resultUpdate = mockMvc.perform(put(TEST_ENDPOINT_RESOURCE)
                        .content(asJsonString(partyDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String contentUpdate = objectMapper.readValue(resultUpdate.getResponse().getContentAsString(), String.class);
        Assertions.assertEquals("Party having partyName=[" + partyDTO.getName() + "] has been successfully updated", contentUpdate);

        // when
        MvcResult resultDelete = mockMvc.perform(delete(TEST_ENDPOINT_RESOURCE)
                        .param("partyName", partyDTO.getName())
                        .content(asJsonString(partyDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String contentDelete = objectMapper.readValue(resultDelete.getResponse().getContentAsString(), String.class);
        Assertions.assertEquals("Party having partyName=[" + partyDTO.getName() + "] has been successfully deleted", contentDelete);
    }

    @Test
    public void getProcesses() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_RESOURCE_PROCESSES)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<ProcessDTO> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(3, resultList.size());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getCertificate() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_RESOURCE_CERTIFICATE, "blue_gw")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        TrustStoreDTO resultObject = objectMapper.readValue(content, TrustStoreDTO.class);
        Assertions.assertNotNull(resultObject);
    }

}
