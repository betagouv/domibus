package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.UserMessage;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.web.rest.ro.MessageLogResultRO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MessageLogResourceIT extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    private MessageLogResource messageLogResource;

    @Autowired
    private UserMessageDao userMessageDao;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(messageLogResource)
                .build();
    }

    @AfterEach
    void tearDown() {
    }

    //TODO needs data
    @Test
    @Disabled
    void getMessageLog() throws Exception {
        List<UserMessage> all = userMessageDao.findAll();

        String messageId = "msg_ack_100";
        MvcResult result = mockMvc.perform(get("/rest/messagelog")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", messageId)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
        MessageLogResultRO errorLogResultRO = objectMapper.readValue(content, MessageLogResultRO.class);
        Assertions.assertNotNull(errorLogResultRO);
    }

    //TODO IB !!!! write test for getCsv
    @Test
    @Disabled
    void getCsv() {
    }

    //TODO needs data
    @Test
    @Disabled
    void getLastTestSent_OK() throws Exception {
        List<UserMessage> all = userMessageDao.findAll();

        MvcResult result = mockMvc.perform(get("/rest/messagelog/test/outgoing/latest")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("partyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1")
                        .param("senderPartyId", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4")
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
    }

    //TODO IB !!!! write test for getLastTestReceived
    @Test
    @Disabled
    void getLastTestReceived() {
    }
}