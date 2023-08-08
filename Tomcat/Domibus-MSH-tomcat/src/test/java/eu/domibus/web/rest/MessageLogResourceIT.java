package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.message.MessageStatusDao;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.test.common.CsvUtil;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class MessageLogResourceIT extends AbstractIT {
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
    private MessageLogResource messageLogResource;

    @Autowired
    MessageStatusDao messageStatusDao;

    @Autowired
    MshRoleDao mshRoleDao;

    @Autowired
    BackendConnectorProvider backendConnectorProvider;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    CsvUtil csvUtil;

    @Autowired
    MSHWebservice mshWebservice;

    @Autowired
    protected PayloadFileStorageProvider payloadFileStorageProvider;

    private final String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.standaloneSetup(messageLogResource)
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
    void getMessageLog_OK() throws Exception {
        mockMvc.perform(get("/rest/messagelog")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", messageId)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.messageLogEntries[0].messageId").value(messageId))
                .andExpect(jsonPath("$.messageLogEntries[0].fromPartyId").value("domibus-blue"))
                .andExpect(jsonPath("$.messageLogEntries[0].toPartyId").value("domibus-red"))
                .andExpect(jsonPath("$.messageLogEntries[0].messageStatus").value("RECEIVED"))
                .andExpect(jsonPath("$.messageLogEntries[0].notificationStatus").value("REQUIRED"))
                .andExpect(jsonPath("$.messageLogEntries[0].mshRole").value("RECEIVING"))
                .andExpect(jsonPath("$.messageLogEntries[0].messageType").value("USER_MESSAGE"));
    }

    @Test
    void getCsv_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/messagelog/csv")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", messageId)
                        .param("orderBy", "received")
                        .param("asc", "false")
                        .param("page", "0")
                        .param("pageSize", "10")

                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        Assertions.assertNotNull(csv);

        List<List<String>> csvRecords = csvUtil.getCsvRecords(csv);
        Assertions.assertEquals(2, csvRecords.size());
        List<String> header = csvRecords.get(0);
        List<String> row = csvRecords.get(1);
        Assertions.assertEquals("Message Id", header.get(0));
        Assertions.assertEquals(messageId, row.get(0));
        Assertions.assertEquals("From Party Id", header.get(1));
        Assertions.assertEquals("domibus-blue", row.get(1));
        Assertions.assertEquals("To Party Id", header.get(2));
        Assertions.assertEquals("domibus-red", row.get(2));
        Assertions.assertEquals("Message Status", header.get(3));
        Assertions.assertEquals("RECEIVED", row.get(3));
        Assertions.assertEquals("AP Role", header.get(7));
        Assertions.assertEquals("RECEIVING", row.get(7));
    }

    @Test
    void getLastTestSent_Not_Found() throws Exception {
        Exception exception = Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get("/rest/messagelog/test/outgoing/latest")
                    .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                    .with(csrf())
                    .param("partyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1")
                    .param("senderPartyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4")
                )
        );
        Assertions.assertNotNull(exception);
        Assertions.assertTrue(exception.getCause().getMessage().contains("[DOM_001]:No User message found for party"));
    }

    @Test
    void getLastTestReceived_Not_Found() throws Exception {
        Exception exception = Assertions.assertThrows(Exception.class,
                () -> mockMvc.perform(get("/rest/messagelog/test/incoming/latest")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("partyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1")
                        .param("senderPartyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4")
                )
        );
        Assertions.assertNotNull(exception);
        Assertions.assertEquals("No Signal Message found.", exception.getCause().getMessage());
    }
}