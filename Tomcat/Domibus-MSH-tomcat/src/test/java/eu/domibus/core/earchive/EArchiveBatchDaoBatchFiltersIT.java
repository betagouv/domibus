package eu.domibus.core.earchive;

import eu.domibus.AbstractIT;
import eu.domibus.api.earchive.*;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.MessageDaoTestUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Stream;

import static eu.domibus.api.earchive.EArchiveBatchStatus.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Test filter variations for querying the EArchiveBatchEntities
 * Because we are using //todo fga @ExtendWith(value = Parameterized.class) the following rule ensures startup of the spring test context
 * - SpringMethodRule provides the instance-level and method-level functionality for TestContextManager.
 *
 * @author Joze Rihtarsic
 * @since 5.0
 */
@SuppressWarnings("unused")
@Transactional
public class EArchiveBatchDaoBatchFiltersIT extends AbstractIT {
    // test data
    private static final String BATCH_ID_00 = "BATCH_ID_00@" + UUID.randomUUID();

    private static final String BATCH_ID_01 = "BATCH_ID_01@" + UUID.randomUUID();

    private static final String BATCH_ID_02 = "BATCH_ID_02@" + UUID.randomUUID();

    private static final String BATCH_ID_03 = "BATCH_ID_03@" + UUID.randomUUID();

    private static final String BATCH_ID_04 = "BATCH_ID_04@" + UUID.randomUUID();
    public static final EArchiveBatchFilter QUEUED = new EArchiveBatchFilter(singletonList(EArchiveBatchStatus.QUEUED), null, null, null, null, null, null, null, null);
    public static final EArchiveBatchFilter EXPORTED = new EArchiveBatchFilter(singletonList(EArchiveBatchStatus.EXPORTED), null, null, null, null, null, null, null, null);
    public static final EArchiveBatchFilter REEXPORTED = new EArchiveBatchFilter(singletonList(EArchiveBatchStatus.EXPORTED), null, null, null, null, null, Boolean.TRUE, null, null);
    public static final EArchiveBatchFilter BY_TYPE = new EArchiveBatchFilter(singletonList(EArchiveBatchStatus.EXPORTED), singletonList(EArchiveRequestType.MANUAL), null, null, null, null, null, null, null);
    public static final EArchiveBatchFilter BY_DATE = new EArchiveBatchFilter(null, null, DateUtils.addDays(Calendar.getInstance().getTime(), -28), DateUtils.addDays(Calendar.getInstance().getTime(), -12), null, null, null, null, null);
    public static final EArchiveBatchFilter ALL = new EArchiveBatchFilter(null, null, null, null, null, null, null, null, null);
    public static final EArchiveBatchFilter BY_SIZE = new EArchiveBatchFilter(null, null, null, null, null, null, null, null, 2);
    public static final EArchiveBatchFilter BY_START = new EArchiveBatchFilter(null, null, null, null, null, null, null, 1, 2);

    private UserMessageLog uml1;


    static Stream<Arguments> testGetBatchRequestList() {
        return Stream.of(
                of("With filter status queued                  ", singletonList(BATCH_ID_04), QUEUED),
                of("With filter status exported                ", singletonList(BATCH_ID_02), EXPORTED),
                of("With filter status exported and reexported ", asList(BATCH_ID_02, BATCH_ID_00), REEXPORTED),
                of("With filter by type                        ", singletonList(BATCH_ID_02), BY_TYPE),
                // Note batches are ordered from latest to oldest
                of("With filter: request date                  ", asList(BATCH_ID_04, BATCH_ID_03, BATCH_ID_02), BY_DATE),
                of("With filter: get All                       ", asList(BATCH_ID_04, BATCH_ID_03, BATCH_ID_02, BATCH_ID_01), ALL),
                of("With filter: test page size                ", asList(BATCH_ID_04, BATCH_ID_03), BY_SIZE),
                of("With filter: test page start               ", asList(BATCH_ID_02, BATCH_ID_01), BY_START)
        );
    }

    static Stream<Arguments> testGetBatchRequestListCount() {
        return Stream.of(
                of("With filter status queued                  ", 1L, QUEUED),
                of("With filter status exported                ", 1L, EXPORTED),
                of("With filter status exported and reexported ", 2L, REEXPORTED),
                of("With filter by type                        ", 1L, BY_TYPE),
                // Note batches are ordered from latest to oldest
                of("With filter: request date                  ", 3L, BY_DATE),
                of("With filter: get All                       ", 4L, ALL),
                of("With filter: test page size                ", 4L, BY_SIZE),
                of("With filter: test page start               ", 4L, BY_START)
        );
    }

    @Autowired
    EArchiveBatchDao eArchiveBatchDao;

    @Autowired
    EArchiveBatchUserMessageDao eArchiveBatchUserMessageDao;
    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;


    @BeforeEach
    @Transactional
    public void setup() {
        Date currentDate = Calendar.getInstance().getTime();
        uml1 = messageDaoTestUtil.createUserMessageLog("uml1-" + UUID.randomUUID(), currentDate);
        // prepare database -> create batches
        // reexported can be null or false
        create(BATCH_ID_00, DateUtils.addDays(currentDate, -30), 1L, 10L, EArchiveRequestType.CONTINUOUS, EArchiveBatchStatus.EXPORTED, Boolean.TRUE, null);
        create(BATCH_ID_01, DateUtils.addDays(currentDate, -30), 1L, 10L, EArchiveRequestType.CONTINUOUS, ARCHIVED, Boolean.FALSE, null);
        create(BATCH_ID_02, DateUtils.addDays(currentDate, -25), 11L, 20L, EArchiveRequestType.MANUAL, EArchiveBatchStatus.EXPORTED, Boolean.FALSE, BATCH_ID_00);
        create(BATCH_ID_03, DateUtils.addDays(currentDate, -22), 21L, 30L, EArchiveRequestType.MANUAL, EXPIRED, Boolean.FALSE, null);
        create(BATCH_ID_04, DateUtils.addDays(currentDate, -15), 31L, 40L, EArchiveRequestType.CONTINUOUS, EArchiveBatchStatus.QUEUED, Boolean.FALSE, null);
    }

    private void create(String batchId, Date dateRequested, Long firstPkUserMessage, Long lastPkUserMessage, EArchiveRequestType continuous, EArchiveBatchStatus status, Boolean reexported, String origin) {
        EArchiveBatchEntity batch = new EArchiveBatchEntity();

        batch.setBatchId(batchId);
        batch.setReExported(reexported);
        batch.setOriginalBatchId(origin);
        batch.setDateRequested(dateRequested);
        batch.setFirstPkUserMessage(firstPkUserMessage);
        batch.setLastPkUserMessage(lastPkUserMessage);
        batch.setRequestType(continuous);
        batch.setEArchiveBatchStatus(status);

        EArchiveBatchEntity merge = eArchiveBatchDao.merge(batch);
        EArchiveBatchEntity batchCreated = em.createQuery("select batch from EArchiveBatchEntity batch where batch.batchId = :batchId", EArchiveBatchEntity.class)
                .setParameter("batchId", batchId)
                .getResultList()
                .get(0);

        List<EArchiveBatchUserMessage> eArchiveBatchUserMessages = new ArrayList<>();
        for (int i = firstPkUserMessage.intValue(); i < lastPkUserMessage; i++) {
            EArchiveBatchUserMessage entity = new EArchiveBatchUserMessage(uml1.getEntityId(), uml1.getUserMessage().getMessageId());
            entity.seteArchiveBatch(merge);
            EArchiveBatchUserMessage merge1 = eArchiveBatchUserMessageDao.merge(entity);
            eArchiveBatchUserMessages.add(merge1);
        }

        merge.seteArchiveBatchUserMessages(eArchiveBatchUserMessages);
        eArchiveBatchDao.update(merge);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void testGetBatchRequestList(String testName, List<String> expectedBatchIds, EArchiveBatchFilter filter) {
        // given-when
        List<EArchiveBatchEntity> resultList = eArchiveBatchDao.getBatchRequestList(filter);
        // then
        Assertions.assertEquals(expectedBatchIds.size(), resultList.size());
        Assertions.assertArrayEquals(expectedBatchIds.toArray(), resultList.stream().map(EArchiveBatchEntity::getBatchId).toArray());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void testGetBatchRequestListCount(String testName, Long total, EArchiveBatchFilter filter) {
        // given-when
        Long count = eArchiveBatchDao.getBatchRequestListCount(filter);
        // then
        Assertions.assertEquals(total, count);
    }
}
