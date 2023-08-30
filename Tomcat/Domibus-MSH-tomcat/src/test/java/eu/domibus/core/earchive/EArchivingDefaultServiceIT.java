package eu.domibus.core.earchive;

import eu.domibus.AbstractIT;
import eu.domibus.api.earchive.EArchiveBatchFilter;
import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.message.UserMessageDefaultService;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import static eu.domibus.core.earchive.EArchivingDefaultService.CONTINUOUS_ID;
import static eu.domibus.core.earchive.EArchivingDefaultService.SANITY_ID;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.ENGLISH;

/**
 * @author François Gautier
 * @since 5.0
 */
@Transactional
//@Disabled("EDELIVERY-6896")
public class EArchivingDefaultServiceIT extends AbstractIT {

    @Autowired
    EArchivingDefaultService eArchivingService;

    @Autowired
    EArchiveBatchStartDao eArchiveBatchStartDao;

    @Autowired
    EArchiveBatchDao eArchiveBatchDao;

    @Autowired
    EArchiveBatchUserMessageDao eArchiveBatchUserMessageDao;

    @Autowired
    EArchiveBatchUtils eArchiveBatchUtils;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Autowired
    UserMessageDefaultService userMessageDefaultService;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @Autowired
    TsidUtil tsidUtil;

    @Autowired
    protected PlatformTransactionManager transactionManager;


    EArchiveBatchEntity batch1;
    EArchiveBatchEntity batch2;
    UserMessageLog uml1;
    UserMessageLog uml2;
    UserMessageLog uml3;
    UserMessageLog uml4;
    UserMessageLog uml5;
    UserMessageLog uml6;
    UserMessageLog uml7_not_archived;
    UserMessageLog uml8_not_archived;

    @BeforeEach
    public void setUp() throws Exception {
        waitUntilDatabaseIsInitialized();
        Assertions.assertEquals(0L, ((long) eArchiveBatchStartDao.findByReference(CONTINUOUS_ID).getLastPkUserMessage()));
        Assertions.assertEquals(0L, ((long) eArchiveBatchStartDao.findByReference(SANITY_ID).getLastPkUserMessage()));
        // prepare
        Date currentDate = Calendar.getInstance().getTime();
        uml1 = messageDaoTestUtil.createUserMessageLog("uml1-" + UUID.randomUUID(), currentDate);
        uml2 = messageDaoTestUtil.createUserMessageLog("uml2-" + UUID.randomUUID(), currentDate);
        uml3 = messageDaoTestUtil.createUserMessageLog("uml3-" + UUID.randomUUID(), currentDate);
        uml4 = messageDaoTestUtil.createUserMessageLog("uml4-" + UUID.randomUUID(), currentDate);
        uml5 = messageDaoTestUtil.createUserMessageLog("uml5-" + UUID.randomUUID(), currentDate);
        uml6 = messageDaoTestUtil.createUserMessageLog("uml6-" + UUID.randomUUID(), currentDate);
        uml7_not_archived = messageDaoTestUtil.createUserMessageLog("uml7-" + UUID.randomUUID(), currentDate);
        uml8_not_archived = messageDaoTestUtil.createUserMessageLog("uml8-" + UUID.randomUUID(), currentDate);

        batch1 = eArchiveBatchDao.merge(EArchiveTestUtils.createEArchiveBatchEntity(
                UUID.randomUUID().toString(),
                EArchiveRequestType.CONTINUOUS,
                EArchiveBatchStatus.EXPORTED,
                DateUtils.addDays(currentDate, -30),
                uml1.getEntityId(),
                uml3.getEntityId(),
                3,
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
                2,
                "/tmp/batch"));

        eArchiveBatchUserMessageDao.create(batch2, Arrays.asList(
                new EArchiveBatchUserMessage(uml4.getEntityId(), uml4.getUserMessage().getMessageId()),
                new EArchiveBatchUserMessage(uml5.getEntityId(), uml5.getUserMessage().getMessageId())
        ));
    }

    @Test
    public void updateStartDateContinuousArchive() {

        Long startMessageDate = 21102610L;
        eArchivingService.updateStartDateContinuousArchive(startMessageDate);

        Assertions.assertEquals(eArchiveBatchUtils.dateToPKUserMessageId(startMessageDate),
                eArchiveBatchStartDao.findByReference(CONTINUOUS_ID).getLastPkUserMessage());

    }

    @Test
    public void getStartDateContinuousArchive() {
        Long startDateContinuousArchive = eArchivingService.getStartDateContinuousArchive();

        Assertions.assertEquals(0L, startDateContinuousArchive.longValue());
    }

    @Test
    public void updateStartDateSanityArchive() {
        Long startMessageDate = 102710L;
        eArchivingService.updateStartDateSanityArchive(startMessageDate);

        Assertions.assertEquals(eArchiveBatchUtils.dateToPKUserMessageId(startMessageDate),
                eArchiveBatchStartDao.findByReference(SANITY_ID).getLastPkUserMessage());
    }

    @Test
    public void getStartDateSanityArchive() {
        Long startDateSanityArchive = eArchivingService.getStartDateSanityArchive();

        Assertions.assertEquals(0L, startDateSanityArchive.longValue());
    }

    @Test
    public void getBatchCount() {
        Long batchRequestsCount = eArchivingService.getBatchRequestListCount(new EArchiveBatchFilter());
        Assertions.assertEquals(2, batchRequestsCount.longValue());
    }

    @Test
    public void getBatchListDefaultFilter() {
        EArchiveBatchFilter filter = new EArchiveBatchFilter();
        List<EArchiveBatchRequestDTO> batchRequestsCount = eArchivingService.getBatchRequestList(filter);
        Assertions.assertEquals(2, batchRequestsCount.size());
        // descending order
        // the batch 1 is last in list
        Assertions.assertEquals(batch1.getBatchId(), batchRequestsCount.get(batchRequestsCount.size() - 1).getBatchId());
        Assertions.assertEquals(uml1.getEntityId(), batchRequestsCount.get(batchRequestsCount.size() - 1).getMessageStartId().longValue());
        Assertions.assertEquals(uml3.getEntityId(), batchRequestsCount.get(batchRequestsCount.size() - 1).getMessageEndId().longValue());
    }

    @Test
    public void getBatchListFilterDates() {
        Date currentDate = Calendar.getInstance().getTime();
        EArchiveBatchFilter filter = new EArchiveBatchFilter(null, DateUtils.addDays(currentDate, -40), DateUtils.addDays(currentDate, -20), null, null);
        List<EArchiveBatchRequestDTO> batchRequestsCount = eArchivingService.getBatchRequestList(filter);
        // second batch2 with only one message
        Assertions.assertEquals(1, batchRequestsCount.size());
    }

    @Test
    public void getBatchListFilterStatus() {
        EArchiveBatchFilter filter = new EArchiveBatchFilter(Collections.singletonList(EArchiveRequestType.CONTINUOUS), null, null, null, null);
        List<EArchiveBatchRequestDTO> batchRequestsCount = eArchivingService.getBatchRequestList(filter);
        // second batch2 with only one message
        Assertions.assertEquals(2, batchRequestsCount.size());
        Assertions.assertEquals(batchRequestsCount.get(0).getMessageEndId(), batchRequestsCount.get(0).getMessageEndId());
    }

    @Test
    public void getBatchUserMessageListExported() {
        // given
        String batchId = batch1.getBatchId();
        // when
        Long messageCount = eArchivingService.getExportedBatchUserMessageListCount(batchId);
        List<String> messageList = eArchivingService.getExportedBatchUserMessageList(batchId, null, null);
        // then
        Assertions.assertNotNull(messageCount);
        Assertions.assertEquals(3, messageList.size());
        Assertions.assertEquals(messageCount.intValue(), messageList.size());
    }


    @Test
    public void getBatchUserMessageListFailed() {
        // given - batch2 has status failed
        String batchId = batch2.getBatchId();
        // when
        Long messageCount = eArchivingService.getExportedBatchUserMessageListCount(batchId);
        List<String> messageList = eArchivingService.getExportedBatchUserMessageList(batchId, null, null);
        // then - no messages are exported!
        Assertions.assertNotNull(messageCount);
        Assertions.assertEquals(0, messageList.size());
        Assertions.assertEquals(messageCount.intValue(), messageList.size());

    }

    @Test
    public void getNotArchivedMessages() {
        Date currentDate = Calendar.getInstance().getTime();
        Long startDate = tsidUtil.zonedTimeDateToMaxTsid(ZonedDateTime.ofInstant(DateUtils.addDays(currentDate, -30).toInstant(), ZoneOffset.UTC));
        Long endDate =tsidUtil.zonedTimeDateToMaxTsid(ZonedDateTime.ofInstant(DateUtils.addDays(currentDate, 1).toInstant(), ZoneOffset.UTC));

        List<String> messages = eArchivingService.getNotArchivedMessages(startDate,
                endDate, null, null);

        // According to the discussion service must return all messages which does not have set archive date!
        int expectedCount = 8;
        Assertions.assertTrue(expectedCount <= messages.size()); // the db may contain messages from other non-transactional tests
        Assertions.assertTrue(messages.contains(uml1.getUserMessage().getMessageId()));
        Assertions.assertTrue(messages.contains(uml8_not_archived.getUserMessage().getMessageId()));
    }

    @Test
    @Transactional
    public void getNotArchivedMessagesCount() {
        Date currentDate = Calendar.getInstance().getTime();
        Long startDate = tsidUtil.zonedTimeDateToMaxTsid(ZonedDateTime.ofInstant(DateUtils.addDays(currentDate, -30).toInstant(), ZoneOffset.UTC));
        Long endDate =tsidUtil.zonedTimeDateToMaxTsid(ZonedDateTime.ofInstant(DateUtils.addDays(currentDate, 1).toInstant(), ZoneOffset.UTC));

        Long count = eArchivingService.getNotArchivedMessagesCount(startDate,
                endDate);

        // According to the discussion service must return all messages which does not have set archive date!
        int expectedCount = 8;
        Assertions.assertTrue(expectedCount <= count); // the db may contain messages from other non-transactional tests
    }

    @Test
    @Transactional
    public void getNotArchivedMessages_noStartnoEnd() {
        List<String> messages = eArchivingService.getNotArchivedMessages(null,
                null, null, null);

        // According to the discussion service must return all messages which does not have set archive date!
        int expectedCount = 8;
        Assertions.assertTrue(expectedCount <= messages.size()); // the db may contain messages from other non-transactional tests
        Assertions.assertTrue(messages.contains(uml1.getUserMessage().getMessageId()));
        Assertions.assertTrue(messages.contains(uml8_not_archived.getUserMessage().getMessageId()));
    }

    @Test
    public void testExecuteBatchIsArchived() {
        // given
        List<EArchiveBatchUserMessage> messageList = eArchiveBatchUserMessageDao.getBatchMessageList(batch1.getBatchId(), null, null);
        Assertions.assertEquals(3, messageList.size());
        for (EArchiveBatchUserMessage eArchiveBatchUserMessage : messageList) {
            Assertions.assertNull(userMessageLogDao.findByMessageId(eArchiveBatchUserMessage.getMessageId()).getArchived());
        }
        Assertions.assertNotEquals(EArchiveBatchStatus.ARCHIVED, batch1.getEArchiveBatchStatus());

        // when
        eArchivingService.executeBatchIsArchived(batch1, messageList);

        em.flush();
        em.clear();

        //then
        EArchiveBatchEntity batchUpdated = eArchiveBatchDao.findEArchiveBatchByBatchId(batch1.getBatchId());
        // messages and
        Assertions.assertEquals(EArchiveBatchStatus.ARCHIVED, batchUpdated.getEArchiveBatchStatus());

        List<EArchiveBatchUserMessage> messageListFinal = eArchiveBatchUserMessageDao.getBatchMessageList(batch1.getBatchId(), null, null);
        Assertions.assertEquals(3, messageListFinal.size());
        for (EArchiveBatchUserMessage eArchiveBatchUserMessage : messageListFinal) {
            Assertions.assertNotNull(userMessageLogDao.findByMessageId(eArchiveBatchUserMessage.getMessageId()).getArchived());
        }
    }

    @Test
    public void testExecuteBatchIsArchivedDelete() {
        // given
        List<EArchiveBatchUserMessage> messageList = eArchiveBatchUserMessageDao.getBatchMessageList(batch1.getBatchId(), null, null);
        Assertions.assertEquals(3, messageList.size());
        Assertions.assertNotEquals(EArchiveBatchStatus.ARCHIVED, batch1.getEArchiveBatchStatus());

        // when
        eArchivingService.executeBatchIsArchived(batch1, messageList);

        //then
        EArchiveBatchEntity batchUpdated = eArchiveBatchDao.findEArchiveBatchByBatchId(batch1.getBatchId());
        // messages and
        Assertions.assertEquals(EArchiveBatchStatus.ARCHIVED, batchUpdated.getEArchiveBatchStatus());

        //delete messages
        List<Long> entityIds = new ArrayList<>();
        messageList.stream().forEach(ml -> entityIds.add(ml.getUserMessageEntityId()));

        List<String> messageIds =  new ArrayList<>();
        messageList.stream().forEach(ml -> messageIds.add(ml.getMessageId()));

        userMessageDefaultService.deleteMessagesWithIDs(entityIds);

    }

    @Test
    public void testSetBatchClientStatusFail() {
        // given
        Assertions.assertNotEquals(EArchiveBatchStatus.ARCHIVE_FAILED, batch1.getEArchiveBatchStatus());
        String message = UUID.randomUUID().toString();
        // when
        eArchivingService.setBatchClientStatus(batch1.getBatchId(), EArchiveBatchStatus.ARCHIVE_FAILED, message);
        //then
        EArchiveBatchEntity batchUpdated = eArchiveBatchDao.findEArchiveBatchByBatchId(batch1.getBatchId());
        Assertions.assertEquals(EArchiveBatchStatus.ARCHIVE_FAILED, batchUpdated.getEArchiveBatchStatus());
        Assertions.assertEquals(message, batchUpdated.getMessage());
    }
}
