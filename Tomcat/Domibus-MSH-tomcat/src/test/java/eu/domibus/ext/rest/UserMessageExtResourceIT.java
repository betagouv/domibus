package eu.domibus.ext.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.AbstractIT;
import eu.domibus.common.MSHRole;
import eu.domibus.ext.domain.UserMessageDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class UserMessageExtResourceIT extends AbstractIT {

    public static final String TEST_ENDPOINT_BASE = "/ext/messages/usermessages";
    public static final String TEST_ENDPOINT_MESSAGE_ID = TEST_ENDPOINT_BASE + "/{messageId}";
    public static final String TEST_ENDPOINT_MESSAGE_ID_ENVELOPE = TEST_ENDPOINT_MESSAGE_ID + "/envelope";
    public static final String TEST_ENDPOINT_MESSAGE_ID_SIGNALENVELOP = TEST_ENDPOINT_MESSAGE_ID + "/signalEnvelope";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webAppContext;

    public ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
    }

    @Test
    @Transactional
    public void testDownloadMsg_messageFound() throws Exception {

        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_MESSAGE_ID, "msg_ack_100")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        UserMessageDTO response = objectMapper.readValue(content, UserMessageDTO.class);

        Assert.assertNotNull(response);
    }

    @Test
    @Transactional
    public void testDownloadMsg_messageNotFound_role() throws Exception {

         mockMvc.perform(get(TEST_ENDPOINT_MESSAGE_ID, "msg_ack_100")
                        .param("mshRole", MSHRole.SENDING.name())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                )
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @Transactional
    public void testDownloadEnvelop_messageFound() throws Exception {

        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_MESSAGE_ID_ENVELOPE, "msg_ack_100")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = result.getResponse().getContentAsString();

        Assert.assertNotNull(content);
    }

    @Test
    @Transactional
    public void testDownloadEnvelop_messageNotFound_role() throws Exception {

         mockMvc.perform(get(TEST_ENDPOINT_MESSAGE_ID_ENVELOPE, "msg_ack_100")
                        .param("mshRole", MSHRole.SENDING.name())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                )
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @Transactional
    public void testDownloadSignalEnvelop_messageFound() throws Exception {

        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_MESSAGE_ID_SIGNALENVELOP, "msg_ack_100")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = result.getResponse().getContentAsString();

        Assert.assertNotNull(content);
    }

    @Test
    @Transactional
    public void testDownloadSignalEnvelop_messageNotFound_role() throws Exception {

         mockMvc.perform(get(TEST_ENDPOINT_MESSAGE_ID_SIGNALENVELOP, "msg_ack_100")
                        .param("mshRole", MSHRole.SENDING.name())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                )
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

}
