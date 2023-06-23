package eu.domibus.ext.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.ext.domain.FailedMessagesCriteriaRO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The complete rest endpoint integration tests
 */
@Transactional
public class MessageMonitoringExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageMonitoringExtResourceIT.class);

    // The endpoints to test
    public static final String TEST_ENDPOINT_RESOURCE = "/ext/monitoring/messages";
    public static final String TEST_ENDPOINT_DELETE = TEST_ENDPOINT_RESOURCE + "/delete";
    public static final String TEST_ENDPOINT_DELETE_ID = TEST_ENDPOINT_RESOURCE + "/delete/{messageId}";
    public static final String TEST_ENDPOINT_FINAL_STATUS_DELETE = TEST_ENDPOINT_RESOURCE + "/finalstatus/delete";
    public static final String TEST_ENDPOINT_FINAL_STATUS_DELETE_ID = TEST_ENDPOINT_RESOURCE + "/finalstatus/delete/{messageId}";

    public static final String TEST_ENDPOINT_FAILED = TEST_ENDPOINT_RESOURCE + "/failed";
    public static final String TEST_ENDPOINT_FAILED_DELETE = TEST_ENDPOINT_FAILED + "/{messageId}";
    public static final String TEST_ENDPOINT_FAILED_ELAPSED = TEST_ENDPOINT_FAILED + "/{messageId}/elapsedtime";
    public static final String TEST_ENDPOINT_ENQUEUED_SEND = TEST_ENDPOINT_RESOURCE + "/enqueued/{messageId}/send";
    public static final String TEST_ENDPOINT_RESTORE = TEST_ENDPOINT_RESOURCE + "/failed/restore";
    public static final String TEST_ENDPOINT_ATTEMPTS = TEST_ENDPOINT_RESOURCE + "/{messageId}/attempts";



    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    MshRoleDao mshRoleDao;

    UserMessageLog uml1_failed;
    UserMessageLog uml2_enqueued;
    UserMessageLog uml3_failed;
    UserMessageLog uml4_received;

    @BeforeEach
    public void setUp() throws XmlProcessingException, IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
        // Do not use @Transactional on Class because it adds "false" transactions also to services.
        // Note here you can not use @Transactional annotation with the following code force commit on data preparation level!!
        Date currentDate = Calendar.getInstance().getTime();

        uml1_failed = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate, MSHRole.SENDING, MessageStatus.SEND_FAILURE, true, MessageDaoTestUtil.DEFAULT_MPC, null);
        uml2_enqueued = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant()), MSHRole.SENDING, MessageStatus.SEND_ENQUEUED, true, MessageDaoTestUtil.DEFAULT_MPC, null);
        uml3_failed = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate, MSHRole.SENDING, MessageStatus.SEND_FAILURE, true, MessageDaoTestUtil.DEFAULT_MPC, null);
        uml4_received = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate, MSHRole.SENDING, MessageStatus.RECEIVED, true, MessageDaoTestUtil.DEFAULT_MPC, null);
        uploadPMode(SERVICE_PORT);
    }

    @Test
    public void getAttempt_notFound() throws Exception {
        // when
        uml1_failed.getUserMessage().setMshRole(mshRoleDao.findOrCreate(MSHRole.SENDING));
        userMessageLogDao.update(uml1_failed);
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_ATTEMPTS, uml1_failed.getUserMessage().getMessageId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<?> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(0, resultList.size());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void delete_ok() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        failedMessagesCriteriaRO.setToDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId()) + 1));
        // when
        MvcResult result = mockMvc.perform(delete(TEST_ENDPOINT_DELETE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<String> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(3, resultList.size());
        assertThat(resultList, CoreMatchers.hasItems(uml1_failed.getUserMessage().getMessageId()));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void delete_toDateTooEarly() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        failedMessagesCriteriaRO.setToDate("2000-01-01");
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_DELETE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void delete_sameDate() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        failedMessagesCriteriaRO.setToDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_DELETE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is4xxClientError())
                .andReturn();

    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void delete_finalStatus_ok() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml4_received.getEntityId(), getHour(uml4_received.getEntityId())));
        failedMessagesCriteriaRO.setToDate("2999-01-01");
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_FINAL_STATUS_DELETE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(MessageStatus.DELETED, uml4_received.getMessageStatus());
    }

    @Test
    public void delete_finalStatus_id() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_FINAL_STATUS_DELETE_ID, uml4_received.getUserMessage().getMessageId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(MessageStatus.DELETED, uml4_received.getMessageStatus());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void restoreFailedMessages_ok() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        uml1_failed.setMshRole(mshRoleDao.findOrCreate(MSHRole.SENDING));
        userMessageLogDao.update(uml1_failed);
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        failedMessagesCriteriaRO.setToDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId()) + 1));
        // when
        MvcResult result = mockMvc.perform(post(TEST_ENDPOINT_RESTORE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<String> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(2, resultList.size());
        assertThat(resultList,
                CoreMatchers.hasItems(
                        uml1_failed.getUserMessage().getMessageId(),
                uml3_failed.getUserMessage().getMessageId()
                ));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void restoreFailedMessages_toDateTooEarly() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        failedMessagesCriteriaRO.setToDate("2000-01-01");
        // when
        mockMvc.perform(post(TEST_ENDPOINT_RESTORE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void restoreFailedMessages_sameDate() throws Exception {
        FailedMessagesCriteriaRO failedMessagesCriteriaRO = new FailedMessagesCriteriaRO();
        failedMessagesCriteriaRO.setFromDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        failedMessagesCriteriaRO.setToDate(getDateFrom(uml1_failed.getEntityId(), getHour(uml1_failed.getEntityId())));
        // when
        mockMvc.perform(post(TEST_ENDPOINT_RESTORE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(failedMessagesCriteriaRO)))
                .andExpect(status().is4xxClientError())
                .andReturn();

    }

    @Test
    public void failed_id_ok() throws Exception {

        // when
        mockMvc.perform(delete(TEST_ENDPOINT_DELETE_ID, uml1_failed.getUserMessage().getMessageId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        UserMessageLog byMessageId = userMessageLogDao.findByMessageId(uml1_failed.getUserMessage().getMessageId(),
                uml1_failed.getUserMessage().getMshRole().getRole());
        Assertions.assertNotNull(byMessageId.getDeleted());
    }

    @Test
    public void listFailedMessages_finalRecipient_id_ok() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_FAILED)
                        .param("finalRecipient", "finalRecipient2")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<String> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(2, resultList.size());
        assertThat(resultList, CoreMatchers.hasItems(uml1_failed.getUserMessage().getMessageId()));
    }

    @Test
    public void failedMessage_elapsed_ok() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_FAILED_ELAPSED, uml1_failed.getUserMessage().getMessageId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        assertTrue(Long.parseLong(content) > 0L);
    }

    @Test
    public void enqueued_send_ok() throws Exception {
        assertNull(uml2_enqueued.getNextAttempt());

        // when
        mockMvc.perform(put(TEST_ENDPOINT_ENQUEUED_SEND, uml2_enqueued.getUserMessage().getMessageId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        assertNotNull(userMessageLogDao.findByMessageId(uml2_enqueued.getUserMessage().getMessageId()).getNextAttempt());
    }

    @Test
    public void delete_failed_ok() throws Exception {

        // when
        mockMvc.perform(delete(TEST_ENDPOINT_FAILED_DELETE, uml3_failed.getUserMessage().getMessageId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        assertEquals(MessageStatus.DELETED, uml3_failed.getMessageStatus());
    }

    @Test
    public void listFailedMessages_finalRecipient_id_nok() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_FAILED)
                        .param("finalRecipient", "finalRecipient3")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<?> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(0, resultList.size());
    }

    @Test
    public void listFailedMessages_id_ok() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_FAILED)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<String> resultList = objectMapper.readValue(content, List.class);
        Assertions.assertEquals(2, resultList.size());
        assertThat(resultList, CoreMatchers.hasItems(uml1_failed.getUserMessage().getMessageId()));
    }

    @Test
    public void delete_id_notFound() throws Exception {

        // when
        MvcResult result = mockMvc.perform(delete(TEST_ENDPOINT_DELETE_ID, "notFound")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        assertTrue(objectMapper.readValue(content, Exception.class).getMessage()
                .contains("[DOM_009]:Message [notFound] does not exist"));
    }

    private String getDateFrom(long entityId, Long hour) {

        String year = "20" + StringUtils.substring(String.valueOf(entityId), 0, 2);
        String month = StringUtils.substring(String.valueOf(entityId), 2, 4);
        String day = StringUtils.substring(String.valueOf(entityId), 4, 6);
        return String.format("%s-%s-%sT%02dH", year, month, day, hour);
    }

    private Long getHour(long entityId) {
        return Long.parseLong(StringUtils.substring(String.valueOf(entityId), 6, 8));
    }


}
