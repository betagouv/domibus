package eu.domibus.core.earchive.listener;

import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.archive.client.api.ArchiveWebhookApi;
import eu.domibus.archive.client.model.BatchNotification;
import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.api.earchive.EArchiveBatchUserMessage;
import eu.domibus.api.earchive.EArchiveBatchUtil;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_EARCHIVE_NOTIFICATION_URL;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_EARCHIVING_NOTIFICATION_DETAILS_ENABLED;

/**
 * @author François Gautier
 * @since 5.0
 */
@Component
public class EArchiveNotificationListener implements MessageListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveNotificationListener.class);

    private final DatabaseUtil databaseUtil;

    private final EArchivingDefaultService eArchiveService;

    private final JmsUtil jmsUtil;

    private final DomibusPropertyProvider domibusPropertyProvider;

    private final EArchiveBatchUtil eArchiveBatchUtil;

    private final ObjectProvider<ArchiveWebhookApi> archiveWebhookApiProvider;

    public EArchiveNotificationListener(
            DatabaseUtil databaseUtil,
            EArchivingDefaultService eArchiveService,
            JmsUtil jmsUtil,
            DomibusPropertyProvider domibusPropertyProvider,
            EArchiveBatchUtil eArchiveBatchUtil,
            ObjectProvider<ArchiveWebhookApi> archiveWebhookApiProvider) {
        this.databaseUtil = databaseUtil;
        this.eArchiveService = eArchiveService;
        this.jmsUtil = jmsUtil;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.eArchiveBatchUtil = eArchiveBatchUtil;
        this.archiveWebhookApiProvider = archiveWebhookApiProvider;
    }

    @Override
    public void onMessage(Message message) {
        LOG.putMDC(DomibusLogger.MDC_USER, databaseUtil.getDatabaseUserName());

        String batchId = jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
        Long entityId = jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");
        if (StringUtils.isBlank(batchId) || entityId == null) {
            LOG.error("Could not get the batchId [{}] and/or entityId [{}]", batchId, entityId);
            return;
        }

        jmsUtil.setCurrentDomainFromMessage(message);

        EArchiveBatchStatus notificationType = EArchiveBatchStatus.valueOf(jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE));

        LOG.info("Notification of type [{}] for batchId [{}] and entityId [{}]", notificationType, batchId, entityId);

        EArchiveBatchEntity eArchiveBatch = eArchiveService.getEArchiveBatch(entityId, true);
        if (notificationType != EArchiveBatchStatus.FAILED && notificationType != EArchiveBatchStatus.EXPORTED) {
            return;
        }

        LOG.info("Notification to the eArchive client for batch [{}] [{}] ", notificationType, eArchiveBatch);
        BatchNotification notification = buildBatchNotification(eArchiveBatch);

        try {
            if (notificationType == EArchiveBatchStatus.FAILED) {
                getEArchivingClientApi().putStaleNotification(notification, eArchiveBatch.getBatchId());
            }
            if (notificationType == EArchiveBatchStatus.EXPORTED) {
                getEArchivingClientApi().putExportNotification(notification, eArchiveBatch.getBatchId());
            }
        } catch (HttpServerErrorException | HttpClientErrorException ex) {
            LOG.warn("Notifying the eArchive client at [{}] failed: the remote server returned an error [{}]", domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_URL), ex.getStatusCode());
            throw ex;
        } catch (Exception ex) {
            LOG.warn("Notifying the eArchive client at [{}] failed", domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_URL));
            throw ex;
        }

        LOG.businessInfo(DomibusMessageCode.BUS_ARCHIVE_BATCH_NOTIFICATION_SENT, eArchiveBatch.getBatchId());
    }

    protected ArchiveWebhookApi getEArchivingClientApi() {
        return archiveWebhookApiProvider.getObject();
    }

    protected BatchNotification buildBatchNotification(EArchiveBatchEntity eArchiveBatch) {
        BatchNotification batchNotification = new BatchNotification();
        batchNotification.setBatchId(eArchiveBatch.getBatchId());
        batchNotification.setCode(eArchiveBatch.getDomibusCode());
        batchNotification.setMessage(eArchiveBatch.getMessage());
        batchNotification.setErrorCode(eArchiveBatch.getDomibusCode());
        batchNotification.setErrorDescription(eArchiveBatch.getMessage());
        batchNotification.setStatus(BatchNotification.StatusEnum.valueOf(eArchiveBatch.getEArchiveBatchStatus().name()));
        if (eArchiveBatch.getRequestType() == EArchiveRequestType.CONTINUOUS || eArchiveBatch.getRequestType() == EArchiveRequestType.SANITIZER) {
            batchNotification.setRequestType(BatchNotification.RequestTypeEnum.CONTINUOUS);
        } else if (eArchiveBatch.getRequestType() == EArchiveRequestType.MANUAL) {
            batchNotification.setRequestType(BatchNotification.RequestTypeEnum.MANUAL);
        }
        batchNotification.setTimestamp(OffsetDateTime.ofInstant(eArchiveBatch.getDateRequested().toInstant(), ZoneOffset.UTC));
        setStartDateAndEndDateInNotification(eArchiveBatch, batchNotification);
        batchNotification.setMessages(eArchiveBatch.geteArchiveBatchUserMessages().stream().map(EArchiveBatchUserMessage::getMessageId).collect(Collectors.toList()));

        return batchNotification;
    }

    protected void setStartDateAndEndDateInNotification(EArchiveBatchEntity eArchiveBatch, BatchNotification batchNotification) {

        final Boolean isNotificationWithStartAndEndDate = domibusPropertyProvider.getBooleanProperty(DOMIBUS_EARCHIVING_NOTIFICATION_DETAILS_ENABLED);
        if (BooleanUtils.isNotTrue(isNotificationWithStartAndEndDate)) {
            LOG.debug("eArchive client with batch Id [{}] needs to receive notifications without message start date and end date [{}]", eArchiveBatch.getBatchId(), isNotificationWithStartAndEndDate);
            return;
        }
        List<EArchiveBatchUserMessage> batchUserMessages = eArchiveBatch.geteArchiveBatchUserMessages();
        Long firstUserMessageEntityId = eArchiveBatchUtil.getMessageStartDate(batchUserMessages, 0);
        Long lastUserMessageEntityId = eArchiveBatchUtil.getMessageStartDate(batchUserMessages, eArchiveBatchUtil.getLastIndex(batchUserMessages));

        Date messageStartDate = eArchiveBatchUtil.getBatchMessageDate(firstUserMessageEntityId);
        Date messageEndDate = eArchiveBatchUtil.getBatchMessageDate(lastUserMessageEntityId);
        if (messageStartDate != null && messageEndDate != null) {
            batchNotification.setMessageStartDate(OffsetDateTime.ofInstant(messageStartDate.toInstant(), ZoneOffset.UTC));
            batchNotification.setMessageEndDate(OffsetDateTime.ofInstant(messageEndDate.toInstant(), ZoneOffset.UTC));
        }
        LOG.debug("eArchive batch messageStartDate [{}] and messageEndDate [{}] for batchId [{}]", messageStartDate, messageEndDate, eArchiveBatch.getBatchId());

    }

}
