package eu.domibus.core.earchive.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.model.ListUserMessageDto;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.earchive.DomibusEArchiveException;
import eu.domibus.core.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchiveBatchStatus;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import gen.eu.domibus.archive.client.api.ArchiveWebhookApi;
import gen.eu.domibus.archive.client.model.BatchNotification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

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

    public EArchiveNotificationListener(
            DatabaseUtil databaseUtil,
            EArchivingDefaultService eArchiveService,
            JmsUtil jmsUtil, DomibusPropertyProvider domibusPropertyProvider) {
        this.databaseUtil = databaseUtil;
        this.eArchiveService = eArchiveService;
        this.jmsUtil = jmsUtil;
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    @Override
    public void onMessage(Message message) {
        LOG.putMDC(DomibusLogger.MDC_USER, databaseUtil.getDatabaseUserName());

        String batchId = jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);

        Long entityId = jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
        if (StringUtils.isBlank(batchId) || entityId == null) {
            LOG.error("Could not get the batchId [{}] and/or entityId [{}]", batchId, entityId);
            return;
        }
        jmsUtil.setDomain(message);

        EArchiveBatchStatus notificationType = EArchiveBatchStatus.valueOf(jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE));

        EArchiveBatchEntity eArchiveBatch = eArchiveService.getEArchiveBatch(entityId);

        if (notificationType == EArchiveBatchStatus.FAILED) {
            LOG.info("Notification to the earchive client for batch FAILED [{}] ", eArchiveBatch);
            ArchiveWebhookApi earchivingClientApi = initializeEarchivingClientApi();
            if (earchivingClientApi != null) {
                earchivingClientApi.putStaleNotification(buildBatchNotification(eArchiveBatch), batchId);
            }
        }

        if (notificationType == EArchiveBatchStatus.EXPORTED) {
            LOG.info("Notification to the earchive client for batch EXPORTED [{}] ", eArchiveBatch);
            ArchiveWebhookApi earchivingClientApi = initializeEarchivingClientApi();
            if (earchivingClientApi != null) {
                earchivingClientApi.putExportNotification(buildBatchNotification(eArchiveBatch), batchId);
            }
        }
    }

    private ArchiveWebhookApi initializeEarchivingClientApi() {
        String restUrl = domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_URL);
        if (StringUtils.isBlank(restUrl)) {
            LOG.debug("EArchiving client endpoint not configured -> skip notification");
            return null;
        }

        LOG.debug("Initializing earchiving client api with endpoint [{}]...", restUrl);

        ArchiveWebhookApi earchivingClientApi = new ArchiveWebhookApi();
        earchivingClientApi.getApiClient().setBasePath(restUrl);

        String username = domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_USERNAME);
        String password = domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_PASSWORD);
        if (StringUtils.isNotBlank(username)) {
            earchivingClientApi.getApiClient().setUsername(username);
            earchivingClientApi.getApiClient().setPassword(password);
        }

        return earchivingClientApi;
    }

    protected BatchNotification buildBatchNotification(EArchiveBatchEntity eArchiveBatch) {
        BatchNotification batchNotification = new BatchNotification();
        batchNotification.setBatchId(eArchiveBatch.getBatchId());
        batchNotification.setErrorDescription(eArchiveBatch.getError());
        batchNotification.setStatus(BatchNotification.StatusEnum.valueOf(eArchiveBatch.getEArchiveBatchStatus().name()));
        batchNotification.setRequestType(BatchNotification.RequestTypeEnum.valueOf(eArchiveBatch.getRequestType().name()));
        batchNotification.setTimestamp(OffsetDateTime.ofInstant(eArchiveBatch.getDateRequested().toInstant(), ZoneOffset.UTC));

        ListUserMessageDto messageListDto = getUserMessageDtoFromJson(eArchiveBatch);
        List<String> messageIds = messageListDto.getUserMessageDtos().stream()
                .map(um -> um.getMessageId()).collect(Collectors.toList());
        batchNotification.setMessages(messageIds);

        Long firstPkUserMessage = messageListDto.getUserMessageDtos().stream()
                .map(um -> um.getEntityId()).reduce(Long::min).orElse(null);


        Date messageStartDate = dateFromLongDate(extractDateFromPKUserMessageId(firstPkUserMessage));
        Date messageEndDate = dateFromLongDate(extractDateFromPKUserMessageId(eArchiveBatch.getLastPkUserMessage()));
        batchNotification.setMessageStartDate(OffsetDateTime.ofInstant(messageStartDate.toInstant(), ZoneOffset.UTC));
        batchNotification.setMessageEndDate(OffsetDateTime.ofInstant(messageEndDate.toInstant(), ZoneOffset.UTC));

        return batchNotification;
    }

    private Date dateFromLongDate(Long dateAsLong) {
        return new Date(dateAsLong);
    }

    // TODO use method from EArchiveBatchUtils when PR #2753 is merged
    private Long extractDateFromPKUserMessageId(Long pkUserMessage) {
        if (pkUserMessage == null) {
            return null;
        }
        long MAX_INCREMENT_NUMBER = 9999999999L;
        return pkUserMessage / (MAX_INCREMENT_NUMBER + 1);
    }

    // TODO use method from EArchiveBatchUtils when PR #2753 is merged
    private ListUserMessageDto getUserMessageDtoFromJson(EArchiveBatchEntity eArchiveBatch) {
        try {
            return new ObjectMapper().readValue(eArchiveBatch.getMessageIdsJson(), ListUserMessageDto.class);
        } catch (IOException e) {
            throw new DomibusEArchiveException("Could not read batch list from batch:" + eArchiveBatch.getBatchId(), e);
        }
    }
}
