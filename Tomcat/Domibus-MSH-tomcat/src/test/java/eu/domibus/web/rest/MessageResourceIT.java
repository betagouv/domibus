package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.common.MSHRole;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class MessageResourceIT extends AbstractIT {
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
    private MessageResource messageResource;

    @Autowired
    BackendConnectorProvider backendConnectorProvider;

    @Autowired
    SoapSampleUtil soapSampleUtil;


    @Autowired
    MSHWebservice mshWebservice;

    @Autowired
    protected PayloadFileStorageProvider payloadFileStorageProvider;

    private final String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.standaloneSetup(messageResource)
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
    void checkCanDownload_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/message/exists")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", messageId)
                        .param("mshRole", MSHRole.RECEIVING.name())

                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
        Assertions.assertEquals("", content);
    }

    @Test
    void checkCanDownload_non_existing_message() throws Exception {
        String nonexistentMessageId = "msg_ack_100_XXX";
        MvcResult result = mockMvc.perform(get("/rest/message/exists")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", nonexistentMessageId)
                        .param("mshRole", MSHRole.RECEIVING.name())

                )
                .andExpect(status().is4xxClientError())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
        Assertions.assertTrue(content.contains("[DOM_001]"));
        Assertions.assertTrue(content.contains("No message found for message id"));
        Assertions.assertTrue(content.contains(nonexistentMessageId));
    }

    @Test
    void downloadUserMessage_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/message/download")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", messageId)
                        .param("mshRole", MSHRole.RECEIVING.name())

                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().string("Content-Disposition", "attachment; filename=" + messageId + ".zip"))
                .andExpect(content().contentTypeCompatibleWith("application/zip"))
                .andReturn();


        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
    }

    @Test
    void downloadEnvelopes_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/message/envelopes")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", messageId)
                        .param("mshRole", MSHRole.RECEIVING.name())

                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().string("Content-Disposition", "attachment; filename=message_envelopes_" + messageId + ".zip"))
                .andExpect(content().contentTypeCompatibleWith("application/zip"))
                .andReturn();


        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
    }

    //TODO write test for resend

    //TODO write test for restoreSelectedFailedMessages

    //TODO  write test for restoreFilteredFailedMessages
}
