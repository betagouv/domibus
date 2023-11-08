package eu.domibus.core.error;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MSHRoleEntity;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.ErrorResult;
import eu.domibus.common.ErrorResultImpl;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ERRORLOG_CLEANER_BATCH_SIZE;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ERRORLOG_CLEANER_OLDER_DAYS;

/**
 * @author Thomas Dussart
 * @since 3.3
 * <p>
 * Service in charge or persisting errors.
 */
@Service
public class ErrorLogServiceImpl implements ErrorLogService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ErrorLogServiceImpl.class);
    protected ErrorLogDao errorLogDao;
    protected DomibusPropertyProvider domibusPropertyProvider;
    protected MshRoleDao mshRoleDao;
    protected ErrorLogEntryTruncateUtil errorLogEntryTruncateUtil;

    public ErrorLogServiceImpl(ErrorLogDao errorLogDao,
                               DomibusPropertyProvider domibusPropertyProvider,
                               MshRoleDao mshRoleDao,
                               ErrorLogEntryTruncateUtil errorLogEntryTruncateUtil) {
        this.errorLogEntryTruncateUtil = errorLogEntryTruncateUtil;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.errorLogDao = errorLogDao;
        this.mshRoleDao = mshRoleDao;
    }

    public void create(ErrorLogEntry errorLogEntry) {
        errorLogEntryTruncateUtil.truncate(errorLogEntry);
        if (errorLogEntry.getUserMessage() == null) {
            UserMessage um = new UserMessage();
            um.setEntityId(UserMessage.DEFAULT_USER_MESSAGE_ID_PK);
            errorLogEntry.setUserMessage(um);
        }
        errorLogDao.create(errorLogEntry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createErrorLog(EbMS3Exception exception, MSHRole mshRole, UserMessage userMessage) {
        ErrorLogEntry errorLogEntry = new ErrorLogEntry(exception);
        MSHRoleEntity role = mshRoleDao.findOrCreate(mshRole);
        errorLogEntry.setMshRole(role);
        errorLogEntry.setUserMessage(userMessage);
        create(errorLogEntry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createErrorLog(Ebms3Messaging ebms3Messaging, MSHRole mshRole, UserMessage userMessage) {
        ErrorLogEntry errorLogEntry = ErrorLogEntry.parse(ebms3Messaging);
        MSHRoleEntity role = mshRoleDao.findOrCreate(mshRole);
        errorLogEntry.setMshRole(role);
        create(errorLogEntry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createErrorLog(String messageInErrorId, ErrorCode errorCode, String errorDetail, MSHRole mshRole, UserMessage userMessage) {
        MSHRoleEntity role = mshRoleDao.findOrCreate(mshRole);
        final ErrorLogEntry errorLogEntry = new ErrorLogEntry(role, messageInErrorId, errorCode.getErrorCodeName(), errorDetail);
        errorLogEntry.setUserMessage(userMessage);
        create(errorLogEntry);
    }

    @Override
    public void deleteErrorLogWithoutMessageIds() {
        int days = domibusPropertyProvider.getIntegerProperty(DOMIBUS_ERRORLOG_CLEANER_OLDER_DAYS);
        int batchSize = domibusPropertyProvider.getIntegerProperty(DOMIBUS_ERRORLOG_CLEANER_BATCH_SIZE);

        errorLogDao.deleteErrorLogsWithoutMessageIdOlderThan(days, batchSize);
    }

    @Timer(clazz = ErrorLogServiceImpl.class, value = "deleteMessages.deleteErrorLogsByMessageIdInError")
    @Counter(clazz = ErrorLogServiceImpl.class, value = "deleteMessages.deleteErrorLogsByMessageIdInError")
    @Override
    public int deleteErrorLogsByMessageIdInError(List<Long> messageEntityIds) {
        return errorLogDao.deleteErrorLogsByMessageIdInError(messageEntityIds);
    }

    @Override
    @Transactional(readOnly = true) 
    public List<ErrorLogEntry> findPaged(final int from, final int max, final String sortColumn, final boolean asc, final Map<String, Object> filters) {
        return errorLogDao.findPaged(from, max, sortColumn, asc, filters);
    }

    @Override
    @Transactional
    public List<? extends ErrorResult> getErrors(String messageId, MSHRole mshRole) {
        List<ErrorLogEntry> errorsForMessage = errorLogDao.getErrorsForMessage(messageId, mshRole);
        return errorsForMessage.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ErrorLogEntry> getErrorsForMessage(String messageId, MSHRole role) {
        return errorLogDao.getErrorsForMessage(messageId, role);
    }

    @Override
    @Transactional
    public List<ErrorLogEntry> getErrorsForMessage(String messageId) {
        return errorLogDao.getErrorsForMessage(messageId);
    }

    @Override
    public ErrorResultImpl convert(ErrorLogEntry errorLogEntry) {
        ErrorResultImpl result = new ErrorResultImpl();
        final String errorCodeString = errorLogEntry.getErrorCode();
        result.setErrorCodeAsString(errorCodeString);
        try {
            //we try to convert from the error code as string to a standard error code; this conversation fails(normal scenario) if this is custom error code
            final ErrorCode errorCode = ErrorCode.findBy(errorCodeString);
            result.setErrorCode(errorCode);
        } catch (IllegalArgumentException e) {
            LOG.debug("Could not convert error code string from [{}]", errorCodeString, e);
        }
        result.setErrorDetail(errorLogEntry.getErrorDetail());
        result.setMessageInErrorId(errorLogEntry.getMessageInErrorId());
        result.setMshRole(eu.domibus.common.MSHRole.valueOf(errorLogEntry.getMshRole().name()));
        result.setNotified(errorLogEntry.getNotified());
        result.setTimestamp(errorLogEntry.getTimestamp());

        return result;
    }

    @Override
    @Transactional(readOnly = true) 
    public long countEntries(Map<String, Object> filters) {
        return errorLogDao.countEntries(filters);
    }

}
