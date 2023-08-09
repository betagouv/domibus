package eu.domibus.web.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.domibus.AbstractIT;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.common.CsvUtil;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.party.PartyResponseRo;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.test.common.SoapSampleUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.transaction.Transactional;
import javax.xml.soap.SOAPMessage;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PartyResourceIT extends AbstractIT {
    private MockMvc mockMvc;

    @Configuration
    static class ContextConfiguration {
        @Primary
        @Bean
        public BackendConnectorService backendConnectorProvider() {
            return Mockito.mock(BackendConnectorService.class);
        }
    }

    @Autowired
    private PartyResource partyResource;

    @Autowired
    BackendConnectorProvider backendConnectorProvider;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    MSHWebservice mshWebservice;

    @Autowired
    protected PayloadFileStorageProvider payloadFileStorageProvider;

    @Autowired
    CsvUtil csvUtil;

    private final String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.standaloneSetup(partyResource)
                .build();

        uploadPMode();
        payloadFileStorageProvider.initialize();
        BackendConnector backendConnector = Mockito.mock(BackendConnector.class);
        Mockito.when(backendConnectorProvider.getBackendConnector(Mockito.any(String.class))).thenReturn(backendConnector);

        String filename = "SOAPMessage2.xml";
        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        mshWebservice.invoke(soapMessage);
    }

    @Test
    void listParties_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/party/list")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("orderBy", "name")
                        .param("asc", "true")
                        .param("page", "0")
                        .param("pageSize", "10")

                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        List<PartyResponseRo> partyResponseRoList = objectMapper.readValue(content, new TypeReference<List<PartyResponseRo>>(){});
        Assertions.assertEquals(2, partyResponseRoList.size());
        PartyResponseRo redParty  = partyResponseRoList.get(0);
        Assertions.assertEquals("red_gw", redParty.getName());
        Assertions.assertEquals("domibus-red", redParty.getJoinedIdentifiers());
        PartyResponseRo blueParty  = partyResponseRoList.get(1);
        Assertions.assertEquals("blue_gw", blueParty.getName());
        Assertions.assertEquals("domibus-blue", blueParty.getJoinedIdentifiers());
    }

    @Test
    void getCsv_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/party/csv")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("orderBy", "name")
                        .param("asc", "true")
                        .param("page", "0")
                        .param("pageSize", "10")

                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        Assertions.assertNotNull(csv);

        List<List<String>> csvRecords = csvUtil.getCsvRecords(csv);
        Assertions.assertEquals(3, csvRecords.size());
        List<String> header = csvRecords.get(0);
        List<String> row1 = csvRecords.get(1);
        List<String> row2 = csvRecords.get(2);
        Assertions.assertEquals("Party name", header.get(0));
        Assertions.assertEquals("red_gw", row1.get(0));
        Assertions.assertEquals("blue_gw", row2.get(0));
        Assertions.assertEquals("Party id", header.get(2));
        Assertions.assertEquals("domibus-red", row1.get(2));
        Assertions.assertEquals("domibus-blue", row2.get(2));
    }
}
