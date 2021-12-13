package eu.domibus.ext.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.AbstractIT;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.model.DomibusDatePrefixedSequenceIdGeneratorGenerator;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.earchive.*;
import eu.domibus.ext.domain.archive.BatchDTO;
import eu.domibus.ext.domain.archive.ExportedBatchResultDTO;
import eu.domibus.ext.domain.archive.ExportedBatchStatusType;
import eu.domibus.ext.domain.archive.QueuedBatchResultDTO;
import org.apache.commons.lang3.time.DateUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The complete rest endpoint integration tests
 */
public class DomibusEArchiveExtResourceCompleteIT extends AbstractIT {

    // The endpoints to test
    public static final String TEST_ENDPOINT_RESOURCE = "/ext/archive";
    public static final String TEST_ENDPOINT_QUEUED = TEST_ENDPOINT_RESOURCE + "/batches/queued";
    public static final String TEST_ENDPOINT_EXPORTED = TEST_ENDPOINT_RESOURCE + "/batches/exported";
    public static final String TEST_ENDPOINT_BATCH = TEST_ENDPOINT_RESOURCE + "/batches/{batchId}";
    public static final String TEST_ENDPOINT_SANITY_DATE = TEST_ENDPOINT_RESOURCE + "/sanity-mechanism/start-date";
    public static final String TEST_ENDPOINT_CONTINUOUS_DATE = TEST_ENDPOINT_RESOURCE + "/continuous-mechanism/start-date";
    public static final String TEST_PLUGIN_USERNAME = "admin";
    public static final String TEST_PLUGIN_PASSWORD = "123456";

    public ObjectMapper objectMapper = new ObjectMapper();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @Autowired
    EArchiveBatchDao eArchiveBatchDao;

    @Autowired
    EArchiveBatchUserMessageDao eArchiveBatchUserMessageDao;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    EArchiveBatchEntity batch1;
    EArchiveBatchEntity batch2;
    EArchiveBatchEntity batch3;
    UserMessageLog uml1;
    UserMessageLog uml2;
    UserMessageLog uml3;
    UserMessageLog uml4;
    UserMessageLog uml5;
    UserMessageLog uml6;
    UserMessageLog uml7_not_archived;
    UserMessageLog uml8_not_archived;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
        // Do not use @Transactional on Class because it adds "false" transactions also to services.
        // Note here you can not use @Transactional annotation with the following code force commit on data preparation level!!
        Date currentDate = Calendar.getInstance().getTime();

        uml1 = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml2 = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml3 = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml4 = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml5 = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml6 = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml7_not_archived = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);
        uml8_not_archived = messageDaoTestUtil.createUserMessageLog(UUID.randomUUID().toString(), currentDate);

        batch1 = eArchiveBatchDao.merge(EArchiveTestUtils.createEArchiveBatchEntity(
                UUID.randomUUID().toString(),
                EArchiveRequestType.CONTINUOUS,
                EArchiveBatchStatus.QUEUED,
                DateUtils.addDays(currentDate, -30),
                uml1.getEntityId(),
                uml3.getEntityId(),
                1,
                "/tmp/batch"));
        eArchiveBatchUserMessageDao.create(batch1, Arrays.asList(
                new EArchiveBatchUserMessage(uml1.getEntityId(), uml1.getUserMessage().getMessageId()),
                new EArchiveBatchUserMessage(uml2.getEntityId(), uml2.getUserMessage().getMessageId()),
                new EArchiveBatchUserMessage(uml3.getEntityId(), uml3.getUserMessage().getMessageId())));

        batch2 = eArchiveBatchDao.merge(EArchiveTestUtils.createEArchiveBatchEntity(
                UUID.randomUUID().toString(),
                EArchiveRequestType.CONTINUOUS,
                EArchiveBatchStatus.FAILED,
                DateUtils.addDays(currentDate, -5),
                2110100000000011L,
                2110100000000020L,
                1,
                "/tmp/batch"));

        eArchiveBatchUserMessageDao.create(batch2, Arrays.asList(
                new EArchiveBatchUserMessage(uml4.getEntityId(), uml4.getUserMessage().getMessageId()),
                new EArchiveBatchUserMessage(uml5.getEntityId(), uml5.getUserMessage().getMessageId())
        ));
        batch3 = eArchiveBatchDao.merge(EArchiveTestUtils.createEArchiveBatchEntity(
                UUID.randomUUID().toString(),
                EArchiveRequestType.MANUAL,
                EArchiveBatchStatus.EXPORTED,
                DateUtils.addDays(currentDate, 0),
                2110100000000021L,
                2110110000000001L,
                1,
                "/tmp/batch")); // is copy from 2
        eArchiveBatchUserMessageDao.create(batch3, Arrays.asList(new EArchiveBatchUserMessage(uml6.getEntityId(), uml6.getUserMessage().getMessageId())));
    }


    @Test
    @Transactional
    public void testGetBatch() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_BATCH, batch1.getBatchId())
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        BatchDTO batchDTO = objectMapper.readValue(content, BatchDTO.class);

        Assert.assertNotNull(batchDTO);
        Assert.assertEquals(ExportedBatchStatusType.QUEUED, batchDTO.getStatus());
    }

    @Test
    @Transactional
    public void testGetBatch_notFound() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_BATCH, "unknown")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        Assert.assertEquals("", content);
    }

    @Test
    @Transactional
    public void testGetSanityArchivingStartDate() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_SANITY_DATE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        assertEquals(10100L, Long.parseLong(content));
    }

    @Test
    @Transactional
    public void testResetSanityArchivingStartDate() throws Exception {
        // given
        long resultDate = 21101500L;
        // when
        mockMvc.perform(put(TEST_ENDPOINT_SANITY_DATE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageStartDate", resultDate + "")
                )
                .andExpect(status().is2xxSuccessful());

        // then
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_SANITY_DATE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertEquals(resultDate, Long.parseLong(content));
    }

    @Test
    @Transactional
    public void testGetStartDateContinuousArchive() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_CONTINUOUS_DATE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        assertEquals(10100L, Long.parseLong(content));
    }

    @Test
    @Transactional
    public void testResetStartDateContinuousArchive() throws Exception {
        // given
        long resultDate = 21101500L;
        // when
        mockMvc.perform(put(TEST_ENDPOINT_CONTINUOUS_DATE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageStartDate", resultDate + "")
                )
                .andExpect(status().is2xxSuccessful());

        // then
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_CONTINUOUS_DATE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertEquals(resultDate, Long.parseLong(content));
    }

    @Test
    @Transactional
    public void testGetQueuedBatchRequestsForNoResults() throws Exception {
        // given
        Integer lastCountRequests = 10;
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_QUEUED)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("lastCountRequests", lastCountRequests + "")
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();

        QueuedBatchResultDTO response = objectMapper.readValue(content, QueuedBatchResultDTO.class);

        Assert.assertNotNull(response.getFilter());
        Assert.assertNotNull(response.getPagination());
        assertEquals(Integer.valueOf(1), response.getPagination().getTotal());
        assertEquals(lastCountRequests, response.getFilter().getLastCountRequests());
        //
        assertEquals(1, response.getBatches().size());
        BatchDTO responseBatch = response.getBatches().get(0);
        assertEquals(batch1.getBatchId(), responseBatch.getBatchId());
        assertEquals(batch1.getFirstPkUserMessage() / (DomibusDatePrefixedSequenceIdGeneratorGenerator.MAX_INCREMENT_NUMBER + 1), responseBatch.getMessageStartDate().longValue());
        assertEquals(batch1.getLastPkUserMessage() / (DomibusDatePrefixedSequenceIdGeneratorGenerator.MAX_INCREMENT_NUMBER + 1), responseBatch.getMessageEndDate().longValue());
        assertEquals(batch1.getDateRequested(), responseBatch.getEnqueuedTimestamp());
        assertEquals(batch1.getRequestType().name(), responseBatch.getRequestType().name());
    }

    @Test
    @Transactional
    public void testHistoryOfTheExportedBatches() throws Exception {
// given
        Long messageStartDate = 211005L;
        Long messageEndDate = 211015L;

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_EXPORTED)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("messageStartDate", messageStartDate + "")
                        .param("messageEndDate", messageEndDate + "")
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        ExportedBatchResultDTO response = objectMapper.readValue(content, ExportedBatchResultDTO.class);
        // test filters
        Assert.assertNotNull(response.getFilter());
        Assert.assertNotNull(response.getPagination());
        assertEquals(Integer.valueOf(1), response.getPagination().getTotal());
        assertEquals(messageStartDate, response.getFilter().getMessageStartDate());
        assertEquals(messageEndDate, response.getFilter().getMessageEndDate());
        // test results
        assertEquals(1, response.getBatches().size());
        BatchDTO responseBatch = response.getBatches().get(0);
        assertEquals(batch3.getBatchId(), responseBatch.getBatchId());
        assertEquals(batch3.getFirstPkUserMessage() / (DomibusDatePrefixedSequenceIdGeneratorGenerator.MAX_INCREMENT_NUMBER + 1), responseBatch.getMessageStartDate().longValue());
        assertEquals(batch3.getLastPkUserMessage() / (DomibusDatePrefixedSequenceIdGeneratorGenerator.MAX_INCREMENT_NUMBER + 1), responseBatch.getMessageEndDate().longValue());
        assertEquals(batch3.getDateRequested(), responseBatch.getEnqueuedTimestamp());
        assertEquals(batch3.getRequestType().name(), responseBatch.getRequestType().name());
        // test date formatting
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        assertThat(content, CoreMatchers.containsString("\"enqueuedTimestamp\":\"" + sdf.format(batch3.getDateRequested()) + "\""));
    }
}
