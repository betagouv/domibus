package eu.domibus.core.message;

import eu.domibus.api.messaging.DuplicateMessageFoundException;
import eu.domibus.api.model.*;
import eu.domibus.api.util.DateUtil;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.message.dictionary.NotificationStatusDao;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.core.scheduler.ReprogrammableService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.procedure.ProcedureOutputs;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static eu.domibus.messaging.MessageConstants.FINAL_RECIPIENT;
import static eu.domibus.messaging.MessageConstants.ORIGINAL_SENDER;

/**
 * @author Christian Koch, Stefan Mueller, Federico Martini
 * @since 3.0
 */
@Repository
public class UserMessageLogDao extends MessageLogDao<UserMessageLog> {

    private static final String STR_MESSAGE_ID = "MESSAGE_ID";
    private static final String STR_MESSAGE_ENTITY_ID = "MESSAGE_ENTITY_ID";

    public static final int IN_CLAUSE_MAX_SIZE = 1000;
    public static final String MSH_ROLE = "MSH_ROLE";

    private final DateUtil dateUtil;

    private final UserMessageLogInfoFilter userMessageLogInfoFilter;

    private final MessageStatusDao messageStatusDao;

    private final NotificationStatusDao notificationStatusDao;

    private final ReprogrammableService reprogrammableService;

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageLogDao.class);

    public UserMessageLogDao(DateUtil dateUtil,
                             UserMessageLogInfoFilter userMessageLogInfoFilter,
                             MessageStatusDao messageStatusDao,
                             NotificationStatusDao notificationStatusDao,
                             ReprogrammableService reprogrammableService) {
        super(UserMessageLog.class);
        this.dateUtil = dateUtil;
        this.userMessageLogInfoFilter = userMessageLogInfoFilter;
        this.messageStatusDao = messageStatusDao;
        this.notificationStatusDao = notificationStatusDao;
        this.reprogrammableService = reprogrammableService;
    }

    public List<Long> findRetryMessages(final long minEntityId, final long maxEntityId) {
        TypedQuery<Long> query = this.em.createNamedQuery("UserMessageLog.findRetryMessages", Long.class);
        query.setParameter("MIN_ENTITY_ID", minEntityId);
        query.setParameter("MAX_ENTITY_ID", maxEntityId);
        query.setParameter("CURRENT_TIMESTAMP", dateUtil.getUtcDate());

        return query.getResultList();
    }

    public List<EArchiveBatchUserMessage> findMessagesForArchivingAsc(long lastUserMessageLogId, long maxEntityIdToArchived, int batchMaxSize) {
        LOG.debug("UserMessageLog.findMessagesForArchivingAsc -> lastUserMessageLogId : [{}] maxEntityIdToArchived : [{}] size : [{}] ",
                lastUserMessageLogId,
                maxEntityIdToArchived,
                batchMaxSize);
        TypedQuery<EArchiveBatchUserMessage> query = this.em.createNamedQuery("UserMessageLog.findMessagesForArchivingAsc", EArchiveBatchUserMessage.class);

        query.setParameter("LAST_ENTITY_ID", lastUserMessageLogId);
        query.setParameter("MAX_ENTITY_ID", maxEntityIdToArchived);
        query.setParameter("STATUSES", MessageStatus.getSuccessfulStates());
        query.setMaxResults(batchMaxSize);

        return query.getResultList();
    }

    public List<EArchiveBatchUserMessage> findMessagesNotFinalAsc(long lastUserMessageLogId, long maxEntityIdToArchived) {
        LOG.debug("UserMessageLog.findMessagesNotFinalDesc -> lastUserMessageLogId : [{}] maxEntityIdToArchived : [{}]",
                lastUserMessageLogId,
                maxEntityIdToArchived);
        TypedQuery<EArchiveBatchUserMessage> query = this.em.createNamedQuery("UserMessageLog.findMessagesForArchivingAsc", EArchiveBatchUserMessage.class);

        query.setParameter("LAST_ENTITY_ID", lastUserMessageLogId);
        query.setParameter("MAX_ENTITY_ID", maxEntityIdToArchived);
        query.setParameter("STATUSES", MessageStatus.getNotFinalStates());

        return query.getResultList();
    }

    public List<String> findFailedMessages(String finalRecipient, String originalUser) {
        return findFailedMessages(finalRecipient, originalUser, null, null);
    }

    public List<String> findFailedMessages(String finalRecipient, String originalUser, Long failedStartDate, Long failedEndDate) {

        Query query = this.em.createNamedQuery("UserMessageLog.findFailedMessagesDuringPeriod");
        query.setParameter("MESSAGE_STATUS", MessageStatus.SEND_FAILURE);
        query.setParameter("FINAL_RECIPIENT", finalRecipient);
        query.setParameter("ORIGINAL_USER", originalUser);
        query.setParameter("START_DATE", failedStartDate);
        query.setParameter("END_DATE", failedEndDate);
        query.unwrap(org.hibernate.query.Query.class).setResultTransformer(new UserMessageLogDtoResultTransformer());
        return ((List<UserMessageLogDto>) query.getResultList()).stream()
                .filter(userMessageLogDto -> isAMatch(userMessageLogDto, finalRecipient, originalUser))
                .map(UserMessageLogDto::getMessageId)
                .collect(Collectors.toList());
    }


    private boolean isAMatch(UserMessageLogDto userMessageLogDto, String finalRecipient, String originalUser) {
        if (StringUtils.isBlank(finalRecipient) && StringUtils.isBlank(originalUser)) {
            return true;
        }
        if (finalRecipient != null && !StringUtils.equalsIgnoreCase(userMessageLogDto.getProperties().get(FINAL_RECIPIENT), finalRecipient)) {
            LOG.trace("It's NOT a match for [{}] with finalRecipient [{}]", userMessageLogDto, finalRecipient);
            return false;
        }
        if (originalUser != null && !StringUtils.equalsIgnoreCase(userMessageLogDto.getProperties().get(ORIGINAL_SENDER), originalUser)) {
            LOG.trace("It's NOT a match for [{}] with originalUser [{}]", userMessageLogDto, originalUser);
            return false;
        }
        LOG.trace("It's a match for [{}] with finalRecipient [{}] and originalUser [{}]", userMessageLogDto, finalRecipient, originalUser);
        return true;
    }

    public List<UserMessageLogDto> findMessagesToDeleteNotInFinalStatus(String originalUser, Long startDate, Long endDate) {
        return findMessagesWithUserDuringPeriod("UserMessageLog.findMessagesWithSenderAndRecipientAndWithoutStatusDuringPeriod", originalUser, startDate, endDate);
    }

    public List<UserMessageLogDto> findMessagesToDeleteInFinalStatus(String originalUser, Long startDate, Long endDate) {
        return findMessagesWithUserDuringPeriod("UserMessageLog.findMessagesWithSenderAndRecipientAndStatusDuringPeriod", originalUser, startDate, endDate);
    }


    private List<UserMessageLogDto> findMessagesWithUserDuringPeriod(String queryName, String originalUser, Long startDate, Long endDate) {
        TypedQuery<UserMessageLogDto> query = this.em.createNamedQuery(queryName, UserMessageLogDto.class);
        query.setParameter("MESSAGE_STATUSES", MessageStatus.getSuccessfulStates());
        query.setParameter("ORIGINAL_USER", originalUser);
        query.setParameter("START_DATE", startDate);
        query.setParameter("END_DATE", endDate);
        return query.getResultList();
    }

    /**
     * Finds a UserMessageLog by message id. If the message id is not found it catches the exception raised Hibernate and returns null.
     *
     * @param messageId The message id
     * @return The UserMessageLog
     */
    @Transactional
    public UserMessageLog findByMessageIdSafely(String messageId, MSHRole mshRole) {
        final UserMessageLog userMessageLog = findByMessageId(messageId, mshRole);
        if (userMessageLog == null) {
            LOG.debug("Could not find any result for message with id [{}]", messageId);
            return null;
        }
        initializeChildren(userMessageLog);
        return userMessageLog;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void initializeChildren(UserMessageLog userMessageLog) {
        //initialize values from the second level cache
        userMessageLog.getMessageStatus();
        userMessageLog.getMshRole().getRole();
        userMessageLog.getNotificationStatus();
    }


    public MessageStatus getMessageStatusById(String messageId) {
        try {
            TypedQuery<MessageStatusEntity> query = em.createNamedQuery("UserMessageLog.getMessageStatusById", MessageStatusEntity.class);
            query.setParameter(STR_MESSAGE_ID, messageId);
            return query.getSingleResult().getMessageStatus();
        } catch (NoResultException nrEx) {
            LOG.debug("No result for message with id [{}]", messageId);
            return MessageStatus.NOT_FOUND;
        } catch (NonUniqueResultException exception) {
            throw new DuplicateMessageFoundException(messageId, exception);
        }
    }

    public MessageStatus getMessageStatus(String messageId, MSHRole mshRole) {
        try {
            TypedQuery<MessageStatusEntity> query = em.createNamedQuery("UserMessageLog.getMessageStatusByIdAndRole", MessageStatusEntity.class);
            query.setParameter(STR_MESSAGE_ID, messageId);
            query.setParameter(MSH_ROLE, mshRole);
            return query.getSingleResult().getMessageStatus();
        } catch (NoResultException nrEx) {
            LOG.debug("No result for message with id [{}]", messageId);
            return MessageStatus.NOT_FOUND;
        }
    }

    public MessageStatus getMessageStatus(final Long messageEntityId) {
        try {
            TypedQuery<MessageStatusEntity> query = em.createNamedQuery("UserMessageLog.getMessageStatusByEntityId", MessageStatusEntity.class);
            query.setParameter(STR_MESSAGE_ENTITY_ID, messageEntityId);
            return query.getSingleResult().getMessageStatus();
        } catch (NoResultException nrEx) {
            LOG.debug("No result for message with entity id [{}]", messageEntityId);
            return MessageStatus.NOT_FOUND;
        }
    }

    @Transactional(readOnly = true)
    public UserMessageLog findByEntityId(final Long entityId) {
        try {
            final UserMessageLog userMessageLog = super.read(entityId);

            if (userMessageLog != null) {
                initializeChildren(userMessageLog);
            }
            return userMessageLog;
        } catch (NoResultException nrEx) {
            LOG.debug("Could not find any result for message with entityId [{}]", entityId);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public UserMessageLog findByEntityIdSafely(final Long entityId) {
        try {
            final UserMessageLog userMessageLog = findByEntityId(entityId);
            if (userMessageLog != null) {
                initializeChildren(userMessageLog);
            }
            return userMessageLog;
        } catch (NoResultException nrEx) {
            LOG.debug("Could not find any result for message with entityId [{}]", entityId);
            return null;
        }
    }

    // keep this until we remove the deprecated ext methods
    public UserMessageLog findByMessageId(String messageId) {
        UserMessageLog userMessageLog;
        TypedQuery<UserMessageLog> query = em.createNamedQuery("UserMessageLog.findByMessageId", UserMessageLog.class);
        query.setParameter(STR_MESSAGE_ID, messageId);
        try {
            userMessageLog = DataAccessUtils.singleResult(query.getResultList());

        } catch (IncorrectResultSizeDataAccessException ex) {
            throw new DuplicateMessageFoundException(messageId, ex);
        }
        if (userMessageLog == null) {
            LOG.info("Did not find any UserMessageLog for message with [{}]=[{}]", STR_MESSAGE_ID, messageId);
            return null;
        }
        return userMessageLog;
    }

    public UserMessageLog findByMessageId(String messageId, MSHRole mshRole) {
        if (mshRole == null) {
            LOG.debug("Provided MSHRole is null; calling findByMessageId(messageId) for id [{}]", messageId);
            return findByMessageId(messageId);
        }

        TypedQuery<UserMessageLog> query = this.em.createNamedQuery("UserMessageLog.findByMessageIdAndRole", UserMessageLog.class);
        query.setParameter(STR_MESSAGE_ID, messageId);
        query.setParameter(MSH_ROLE, mshRole);

        UserMessageLog userMessageLog = DataAccessUtils.singleResult(query.getResultList());
        if (userMessageLog == null) {
            LOG.debug("Query UserMessageLog.findByMessageIdAndRole did not find any result for message with id [{}] and MSH role [{}]", messageId, mshRole);
        }
        return userMessageLog;
    }

    public List<UserMessageLogDto> getDeletedUserMessagesOlderThan(Date date, String mpc, Integer expiredDeletedMessagesLimit, boolean eArchiveIsActive) {
        return getMessagesOlderThan(date, mpc, expiredDeletedMessagesLimit, "UserMessageLog.findDeletedUserMessagesOlderThan", eArchiveIsActive);
    }

    public List<UserMessageLogDto> getUndownloadedUserMessagesOlderThan(Date date, String mpc, Integer expiredNotDownloadedMessagesLimit, boolean eArchiveIsActive) {
        return getMessagesOlderThan(date, mpc, expiredNotDownloadedMessagesLimit, "UserMessageLog.findUndownloadedUserMessagesOlderThan", eArchiveIsActive);
    }

    public List<UserMessageLogDto> getDownloadedUserMessagesOlderThan(Date date, String mpc, Integer expiredDownloadedMessagesLimit, boolean eArchiveIsActive) {
        return getMessagesOlderThan(date, mpc, expiredDownloadedMessagesLimit, "UserMessageLog.findDownloadedUserMessagesOlderThan", eArchiveIsActive);
    }

    private List<UserMessageLogDto> getMessagesOlderThan(Date startDate, String mpc, Integer expiredMessagesLimit, String queryName, boolean eArchiveIsActive) {
        Query query = em.createNamedQuery(queryName);
        query.setParameter("DATE", startDate);
        query.setParameter("MPC", mpc);
        query.setParameter("EARCHIVE_IS_ACTIVE", eArchiveIsActive);
        query.setMaxResults(expiredMessagesLimit);
        return query.getResultList();
    }
    public List<UserMessageLogDto> getSentUserMessagesOlderThan(Date date, String mpc, Integer expiredSentMessagesLimit, boolean isDeleteMessageMetadata, boolean eArchiveIsActive) {
        if (isDeleteMessageMetadata) {
            return getMessagesOlderThan(date, mpc, expiredSentMessagesLimit, "UserMessageLog.findSentUserMessagesOlderThan", eArchiveIsActive);
        }
        // return only messages with payload not already cleared
        return getSentUserMessagesWithPayloadNotClearedOlderThan(date, mpc, expiredSentMessagesLimit, eArchiveIsActive);
    }

    public List<UserMessageLogDto> getAllMessages() {
        Query query = em.createNamedQuery("UserMessageLog.findAllMessages");
        query.unwrap(org.hibernate.query.Query.class).setResultTransformer(new UserMessageLogDtoResultTransformer());
        return query.getResultList();
    }

    public void deleteExpiredMessages(Date startDate, Date endDate, String mpc, Integer expiredMessagesLimit, String queryName) {
        StoredProcedureQuery query = em.createStoredProcedureQuery(queryName)
                .registerStoredProcedureParameter(
                        "MPC",
                        String.class,
                        ParameterMode.IN
                )
                .registerStoredProcedureParameter(
                        "STARTDATE",
                        Date.class,
                        ParameterMode.IN
                )
                .registerStoredProcedureParameter(
                        "ENDDATE",
                        Date.class,
                        ParameterMode.IN
                )
                .registerStoredProcedureParameter(
                        "MAXCOUNT",
                        Integer.class,
                        ParameterMode.IN
                )
                .setParameter("MPC", mpc)
                .setParameter("STARTDATE", startDate)
                .setParameter("ENDDATE", endDate)
                .setParameter("MAXCOUNT", expiredMessagesLimit);

        try {
            query.execute();
        } finally {
            try {
                query.unwrap(ProcedureOutputs.class).release();
                LOG.debug("Finished releasing delete procedure");
            } catch (Exception ex) {
                LOG.error("Finally exception when using the stored procedure to delete", ex);
            }
        }
    }

    protected List<UserMessageLogDto> getSentUserMessagesWithPayloadNotClearedOlderThan(Date date, String mpc, Integer expiredSentMessagesLimit, boolean eArchiveIsActive) {
        return getMessagesOlderThan(date, mpc, expiredSentMessagesLimit, "UserMessageLog.findSentUserMessagesWithPayloadNotClearedOlderThan", eArchiveIsActive);
    }

    @Transactional
    public int getMessagesNewerThan(Date startDate, String mpc, MessageStatus messageStatus, String partitionName) {
        String sqlString = "select count(*) from " +
                "             TB_USER_MESSAGE_LOG PARTITION ($PARTITION) " +
                "             inner join  TB_USER_MESSAGE   on TB_USER_MESSAGE_LOG.ID_PK=TB_USER_MESSAGE.ID_PK" +
                "             inner join  TB_D_MESSAGE_STATUS on TB_USER_MESSAGE_LOG.MESSAGE_STATUS_ID_FK=TB_D_MESSAGE_STATUS.ID_PK" +
                "             inner join  TB_D_MPC on TB_USER_MESSAGE.MPC_ID_FK=TB_D_MPC.ID_PK" +
                "           where TB_D_MESSAGE_STATUS.STATUS=:MESSAGESTATUS" +
                "             and TB_D_MPC.VALUE=:MPC" +
                "             and TB_USER_MESSAGE_LOG.$DATE_COLUMN is not null" +
                "             and TB_USER_MESSAGE_LOG.$DATE_COLUMN > :STARTDATE";

        sqlString = sqlString.replace("$PARTITION", partitionName);
        sqlString = sqlString.replace("$DATE_COLUMN", getDateColumn(messageStatus));

        LOG.trace("sqlString to find non expired messages: [{}]", sqlString);
        final Query countQuery = em.createNativeQuery(sqlString);
        countQuery.setParameter("MPC", mpc);
        countQuery.setParameter("STARTDATE", startDate);
        countQuery.setParameter("MESSAGESTATUS", messageStatus);
        int result = ((BigDecimal) countQuery.getSingleResult()).intValue();
        LOG.debug("count by message status result [{}] for mpc [{}] on partition [{}]", result, mpc, partitionName);
        return result;
    }

    protected String getDateColumn(MessageStatus messageStatus) {
        switch (messageStatus) {
            case ACKNOWLEDGED:
            case RECEIVED:
            case DOWNLOADED:
                return messageStatus.name();
            case SEND_FAILURE:
                return "FAILED";
            default:
                LOG.warn("Messages with status [{}] are not defined on the retention mechanism", messageStatus);
                return "INVALID_STATUS_FOR_RETENTION";
        }
    }

    public String findBackendForMessageId(String messageId, MSHRole mshRole) {
        TypedQuery<String> query = em.createNamedQuery("UserMessageLog.findBackendForMessage", String.class);
        query.setParameter(STR_MESSAGE_ID, messageId);
        query.setParameter(MSH_ROLE, mshRole);
        return query.getSingleResult();
    }

    public String findBackendForMessageEntityId(long messageEntityId) {
        TypedQuery<String> query = em.createNamedQuery("UserMessageLog.findBackendForMessageEntityId", String.class);
        query.setParameter(STR_MESSAGE_ENTITY_ID, messageEntityId);
        return query.getSingleResult();
    }

    public void setAsNotified(final UserMessageLog messageLog) {
        final NotificationStatusEntity status = notificationStatusDao.findOrCreate(NotificationStatus.NOTIFIED);
        messageLog.setNotificationStatus(status);
    }

    @Override
    public List<MessageLogInfo> findAllInfoPaged(int from, int max, String column, boolean asc, Map<String, Object> filters) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving messages for parameters from [{}] max [{}] column [{}] asc [{}]", from, max, column, asc);
            for (Map.Entry<String, Object> stringObjectEntry : filters.entrySet()) {
                if (stringObjectEntry.getValue() != null) {
                    LOG.debug("Setting parameters for query ");
                    LOG.debug(stringObjectEntry.getKey() + "  " + stringObjectEntry.getValue());
                }
            }
        }

        long startTime = 0;
        if (LOG.isDebugEnabled()) {
            startTime = System.currentTimeMillis();
        }

        final List<MessageLogInfo> resultList = super.findAllInfoPaged(from, max, column, asc, filters);

        if (LOG.isDebugEnabled()) {
            final long endTime = System.currentTimeMillis();
            LOG.debug("[{}] millisecond to execute query for [{}] results", endTime - startTime, resultList.size());
        }
        return resultList;
    }

    @Timer(clazz = UserMessageLogDao.class, value = "deleteMessages.deleteMessageLogs")
    @Counter(clazz = UserMessageLogDao.class, value = "deleteMessages.deleteMessageLogs")
    @Transactional
    public int deleteMessageLogs(List<Long> ids) {
        final Query deleteQuery = em.createNamedQuery("UserMessageLog.deleteMessageLogs");
        deleteQuery.setParameter("IDS", ids);
        int result = deleteQuery.executeUpdate();
        LOG.trace("deleteUserMessageLogs result [{}]", result);
        return result;
    }

    @Transactional
    public int countMessagesNotArchivedOnPartition(String partitionName) {
        String sqlString = "SELECT COUNT(*) FROM TB_USER_MESSAGE_LOG PARTITION ($PARTITION) " +
                            "INNER JOIN TB_D_MESSAGE_STATUS dms ON MESSAGE_STATUS_ID_FK=dms.ID_PK " +
                            "INNER JOIN TB_USER_MESSAGE um ON TB_USER_MESSAGE_LOG.ID_PK=um.ID_PK " +
                            "WHERE dms.STATUS NOT IN :MESSAGE_STATUSES AND um.TEST_MESSAGE=0 AND archived IS NULL";
        sqlString = sqlString.replace("$PARTITION", partitionName);
        final Query countQuery = em.createNativeQuery(sqlString);
        try {
            countQuery.setParameter("MESSAGE_STATUSES", MessageStatus.getNonArchivableStatesAsString());
            int result = ((BigDecimal) countQuery.getSingleResult()).intValue();
            LOG.debug("count unarchived messages result [{}]", result);
            return result;
        } catch (NoResultException nre) {
            LOG.warn("Could not count unarchived messages for partition [{}], result [{}]", partitionName, nre);
            return -1;
        }
    }


    @Transactional
    public int countByMessageStatusOnPartition(List<String> messageStatuses, String partitionName) {
        String sqlString = "SELECT COUNT(*) FROM TB_USER_MESSAGE_LOG PARTITION ($PARTITION) INNER JOIN TB_D_MESSAGE_STATUS dms ON MESSAGE_STATUS_ID_FK=dms.ID_PK WHERE dms.STATUS NOT IN :MESSAGE_STATUSES";
        sqlString = sqlString.replace("$PARTITION", partitionName);
        try {
            final Query countQuery = em.createNativeQuery(sqlString);
            countQuery.setParameter("MESSAGE_STATUSES", messageStatuses);
            int result = ((BigDecimal) countQuery.getSingleResult()).intValue();
            LOG.debug("count by message status result [{}]", result);
            return result;
        } catch (NoResultException nre) {
            LOG.warn("Could not count in progress messages for partition [{}], result [{}]", partitionName, nre);
            return -1;
        }
    }

    protected MessageLogInfoFilter getMessageLogInfoFilter() {
        return userMessageLogInfoFilter;
    }

    @MDCKey({DomibusLogger.MDC_MESSAGE_ID, DomibusLogger.MDC_MESSAGE_ROLE, DomibusLogger.MDC_MESSAGE_ENTITY_ID})
    public void setMessageStatus(UserMessageLog messageLog, MessageStatus messageStatus) {
        MessageStatusEntity messageStatusEntity = messageStatusDao.findOrCreate(messageStatus);
        messageLog.setMessageStatus(messageStatusEntity);

        switch (messageStatus) {
            case DELETED:
                messageLog.setDeleted(new Date());
                reprogrammableService.removeRescheduleInfo(messageLog);
                break;
            case ACKNOWLEDGED:
            case ACKNOWLEDGED_WITH_WARNING:
                messageLog.setAcknowledged(new Date());
                reprogrammableService.removeRescheduleInfo(messageLog);
                break;
            case DOWNLOADED:
                messageLog.setDownloaded(new Date());
                reprogrammableService.removeRescheduleInfo(messageLog);
                break;
            case SEND_FAILURE:
                messageLog.setFailed(new Date());
                reprogrammableService.removeRescheduleInfo(messageLog);
                break;
            default:
        }
        LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_STATUS_UPDATE, "USER_MESSAGE", messageStatus);
    }

    @Override
    protected List<Predicate> getPredicates(Map<String, Object> filters, CriteriaBuilder cb, Root<UserMessageLog> mle) {
        List<Predicate> predicates = new ArrayList<>();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            if (filter.getValue() != null) {
                if (filter.getValue() instanceof String) {
                    if (!filter.getValue().toString().isEmpty()) {
                        switch (filter.getKey()) {
                            case "":
                                break;
                            default:
                                predicates.add(cb.like(mle.get(filter.getKey()), (String) filter.getValue()));
                                break;
                        }
                    }
                } else if (filter.getValue() instanceof Date) {
                    if (!filter.getValue().toString().isEmpty()) {
                        switch (filter.getKey()) {
                            case "receivedFrom":
                                predicates.add(cb.greaterThanOrEqualTo(mle.<Date>get("received"), Timestamp.valueOf(filter.getValue().toString())));
                                break;
                            case "receivedTo":
                                predicates.add(cb.lessThanOrEqualTo(mle.<Date>get("received"), Timestamp.valueOf(filter.getValue().toString())));
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    predicates.add(cb.equal(mle.<String>get(filter.getKey()), filter.getValue()));
                }
            }
        }
        return predicates;
    }

    public UserMessageLog findById(Long entityId) {
        return this.em.find(UserMessageLog.class, entityId);
    }

    public void updateArchived(List<Long> entityIds) {
        update(entityIds, this::updateArchivedBatched);
    }

    public void updateExported(List<Long> entityIds) {
        update(entityIds, this::updateExportedBatched);
    }

    public void update(List<Long> entityIds, Consumer<List<Long>> updateArchivedBatched) {
        if (CollectionUtils.isEmpty(entityIds)) {
            return;
        }

        int totalSize = entityIds.size();

        int maxBatchesToCreate = (totalSize - 1) / IN_CLAUSE_MAX_SIZE;

        IntStream.range(0, maxBatchesToCreate + 1)
                .mapToObj(createList(entityIds, totalSize, maxBatchesToCreate))
                .forEach(updateArchivedBatched);

    }

    private IntFunction<List<Long>> createList(List<Long> entityIds, int totalSize, int maxBatchesToCreate) {
        return i -> entityIds.subList(
                getFromIndex(i),
                getToIndex(i, totalSize, maxBatchesToCreate));
    }

    private int getFromIndex(int i) {
        return i * IN_CLAUSE_MAX_SIZE;
    }

    private int getToIndex(int i, int totalSize, int maxBatchesToCreate) {
        if (i == maxBatchesToCreate) {
            return totalSize;
        }
        return (i + 1) * IN_CLAUSE_MAX_SIZE;
    }

    public void updateArchivedBatched(List<Long> entityIds) {
        Query namedQuery = this.em.createNamedQuery("UserMessageLog.updateArchived");

        namedQuery.setParameter("ENTITY_IDS", entityIds);
        namedQuery.setParameter("CURRENT_TIMESTAMP", dateUtil.getUtcDate());
        int i = namedQuery.executeUpdate();
        if (LOG.isTraceEnabled()) {
            LOG.trace("UserMessageLogs [{}] updated to archived(0:no, 1: yes) with current_time: [{}]", entityIds, i);
        }
    }

    public void updateExportedBatched(List<Long> entityIds) {
        Query namedQuery = this.em.createNamedQuery("UserMessageLog.updateExported");

        namedQuery.setParameter("ENTITY_IDS", entityIds);
        namedQuery.setParameter("CURRENT_TIMESTAMP", dateUtil.getUtcDate());
        int i = namedQuery.executeUpdate();
        if (LOG.isTraceEnabled()) {
            LOG.trace("UserMessageLogs [{}] updated to archived(0:no, 1: yes) with current_time: [{}]", entityIds, i);
        }
    }

    public void updateDeletedBatched(List<Long> entityIds) {
        Query namedQuery = this.em.createNamedQuery("UserMessageLog.updateDeleted");

        namedQuery.setParameter("ENTITY_IDS", entityIds);
        namedQuery.setParameter("CURRENT_TIMESTAMP", dateUtil.getUtcDate());
        MessageStatusEntity deletedStatus = messageStatusDao.findMessageStatus(MessageStatus.DELETED);
        namedQuery.setParameter("DELETED_STATUS", deletedStatus);
        int i = namedQuery.executeUpdate();
        if (LOG.isTraceEnabled()) {
            LOG.trace("UserMessageLogs [{}] updated to deleted(0:no, 1: yes) with current_time: [{}]", entityIds, i);
        }
    }
}
