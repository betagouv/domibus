package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.common.MSHRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class MessageResourceIT extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    private MessageResource messageResource;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(messageResource)
                .build();
    }

    @Test
    void checkCanDownload_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/message/exists")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageId", "msg_ack_100")
                        .param("mshRole", MSHRole.RECEIVING.name())

                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // then
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

        // then
        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
        Assertions.assertTrue(content.contains("[DOM_001]"));
        Assertions.assertTrue(content.contains("No message found for message id"));
        Assertions.assertTrue(content.contains(nonexistentMessageId));

    }

    @Test
    void downloadUserMessage_OK() throws Exception {
        String messageId = "msg_ack_100";
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

    //TODO needs data
    @Test
    @Disabled
    void downloadEnvelopes_OK() throws Exception {
        String messageId = "msg_ack_100";
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

    //TODO IB !!!! write test for resend

    //TODO IB !!!! write test for restoreSelectedFailedMessages

    //TODO IB !!!! write test for restoreFilteredFailedMessages

}
