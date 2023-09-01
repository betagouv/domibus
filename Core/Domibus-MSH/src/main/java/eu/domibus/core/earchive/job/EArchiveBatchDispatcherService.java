package eu.domibus.core.earchive.job;

import eu.domibus.api.earchive.DomibusEArchiveException;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JMSMessageBuilder;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchiveBatchStart;
import eu.domibus.api.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.jms.spi.InternalJMSConstants;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jms.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author François Gautier
 * @since 5.0
 */
@Service
public class EArchiveBatchDispatcherService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveBatchDispatcherService.class);

    private final JMSManager jmsManager;

    private final Queue eArchiveQueue;

    private final DomibusPropertyProvider domibusPropertyProvider;

    private final EArchivingJobService eArchivingJobService;

    public EArchiveBatchDispatcherService(JMSManager jmsManager,
                                          @Qualifier(InternalJMSConstants.EARCHIVE_QUEUE) Queue eArchiveQueue,
                                          DomibusPropertyProvider domibusPropertyProvider,
                                          EArchivingJobService eArchivingJobService) {
        this.jmsManager = jmsManager;
        this.eArchiveQueue = eArchiveQueue;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.eArchivingJobService = eArchivingJobService;
    }

    @Timer(clazz = EArchiveBatchDispatcherService.class, value = "earchive_createBatch")
    @Counter(clazz = EArchiveBatchDispatcherService.class, value = "earchive_createBatch")
    public void startBatch(Domain domain, EArchiveRequestType eArchiveRequestType) {
        LOG.debug("start eArchive batch for domain [{}] and of type [{}]", domain, eArchiveRequestType);
        final String eArchiveActive = domibusPropertyProvider.getProperty(domain, DOMIBUS_EARCHIVE_ACTIVE);
        if (BooleanUtils.isNotTrue(BooleanUtils.toBooleanObject(eArchiveActive))) {
            LOG.debug("eArchiving is not enabled");
            return;
        }

        EArchiveBatchStart continuousStartDate = eArchivingJobService.getContinuousStartDate(eArchiveRequestType);
        Long lastEntityIdProcessed = continuousStartDate.getLastPkUserMessage();
        Long newLastEntityIdProcessed = lastEntityIdProcessed;
        long maxEntityIdToArchived = eArchivingJobService.getMaxEntityIdToArchived(eArchiveRequestType);
        int batchMaxSize = getProperty(DOMIBUS_EARCHIVE_BATCH_SIZE);
        int batchPayloadMaxSize = getProperty(DOMIBUS_EARCHIVE_BATCH_SIZE_PAYLOAD) * 1024 * 1024;
        int maxNumberOfBatchesCreated = getProperty(DOMIBUS_EARCHIVE_BATCH_MAX);
        LOG.trace("Start eArchive batch lastEntityIdProcessed [{}], " +
                        "maxEntityIdToArchived [{}], " +
                        "batchMaxSize [{}], " +
                        "batchPayloadMaxSize [{}], " +
                        "maxNumberOfBatchesCreated [{}]",
                lastEntityIdProcessed,
                maxEntityIdToArchived,
                batchMaxSize,
                batchPayloadMaxSize,
                maxNumberOfBatchesCreated);

        for (int i = 0; i < maxNumberOfBatchesCreated; i++) {
            EArchiveBatchEntity batchAndEnqueue = createBatchAndEnqueue(newLastEntityIdProcessed, batchMaxSize, batchPayloadMaxSize, maxEntityIdToArchived, domain, eArchiveRequestType);
            if (batchAndEnqueue == null) {
                break;
            }
            newLastEntityIdProcessed = batchAndEnqueue.getLastPkUserMessage();
            LOG.debug("eArchive created with last entity [{}]", lastEntityIdProcessed);
        }
        if (eArchiveRequestType == EArchiveRequestType.SANITIZER) {
            eArchivingJobService.createEventOnNonFinalMessages(lastEntityIdProcessed, maxEntityIdToArchived);
            eArchivingJobService.createEventOnStartDateContinuousJobStopped(eArchivingJobService.getContinuousStartDate(EArchiveRequestType.CONTINUOUS).getModificationTime());
        }
        if (batchCreated(lastEntityIdProcessed, newLastEntityIdProcessed)) {
            eArchivingJobService.updateLastEntityIdExported(newLastEntityIdProcessed, eArchiveRequestType);
            LOG.debug("Dispatch eArchiving batches finished with last entityId [{}]", lastEntityIdProcessed);
        } else {
            if (BooleanUtils.isTrue(domibusPropertyProvider.getBooleanProperty(domain, DOMIBUS_EARCHIVE_EXPORT_EMPTY)) && eArchiveRequestType == EArchiveRequestType.CONTINUOUS) {
                // create empty batch!
                EArchiveBatchEntity eArchiveBatchWithoutMessages = createBatchAndEnqueue(lastEntityIdProcessed, domain, EArchiveRequestType.CONTINUOUS, new ArrayList<>());
                LOG.debug("eArchive [{}] created with no messages", eArchiveBatchWithoutMessages.getBatchId());
            }
        }
    }

    private boolean batchCreated(Long lastEntityIdProcessed, Long newLastEntityIdProcessed) {
        return !Objects.equals(newLastEntityIdProcessed, lastEntityIdProcessed);
    }

    /**
     * Create a new batch and enqueue it
     */
    private EArchiveBatchEntity createBatchAndEnqueue(final Long lastEntityIdProcessed, int batchMaxSize, int batchPayloadMaxSize, long maxEntityIdToArchived, Domain domain, EArchiveRequestType requestType) {
        List<EArchiveBatchUserMessage> messagesForArchivingAsc = eArchivingJobService.findMessagesForArchivingAsc(lastEntityIdProcessed, maxEntityIdToArchived, batchMaxSize, batchPayloadMaxSize);

        if (CollectionUtils.isEmpty(messagesForArchivingAsc)) {
            LOG.debug("No message to archive");
            return null;
        }
        long lastEntityIdTreated = messagesForArchivingAsc.get(messagesForArchivingAsc.size() - 1).getUserMessageEntityId();

        return createBatchAndEnqueue(lastEntityIdTreated, domain, requestType, messagesForArchivingAsc);
    }

    public EArchiveBatchEntity createBatchAndEnqueue(long lastEntityIdTreated, Domain domain, EArchiveRequestType requestType, List<EArchiveBatchUserMessage> messagesForArchivingAsc) {
        EArchiveBatchEntity eArchiveBatch = eArchivingJobService.createEArchiveBatchWithMessages(lastEntityIdTreated, messagesForArchivingAsc, requestType);

        enqueueEArchive(eArchiveBatch, domain, EArchiveBatchStatus.EXPORTED.name());
        LOG.businessInfo(DomibusMessageCode.BUS_ARCHIVE_BATCH_CREATE, requestType, eArchiveBatch.getBatchId());
        return eArchiveBatch;
    }

    /**
     * updates the data for batchId  and send it to EArchive queue for reexport
     *
     * @param batchId the batch id
     * @return reexported batch entity
     */
    public EArchiveBatchEntity reExportBatchAndEnqueue(final String batchId, Domain domain) {
        LOG.debug("Re-Export [{}] the batch and submit it to queue!", batchId);
        EArchiveBatchEntity eArchiveBatch = eArchivingJobService.reExportEArchiveBatch(batchId);
        enqueueEArchive(eArchiveBatch, domain, EArchiveBatchStatus.EXPORTED.name());
        LOG.businessInfo(DomibusMessageCode.BUS_ARCHIVE_BATCH_REEXPORT, batchId);
        return eArchiveBatch;
    }

    private int getProperty(String property) {
        Integer integerProperty = domibusPropertyProvider.getIntegerProperty(property);
        if (integerProperty == null) {
            throw new DomibusEArchiveException("Property [" + property + "] not found");
        }
        return integerProperty;
    }

    public void enqueueEArchive(EArchiveBatchEntity eArchiveBatch, Domain domain, String jmsType) {

        jmsManager.sendMessageToQueue(JMSMessageBuilder
                .create()
                .property(MessageConstants.BATCH_ID, eArchiveBatch.getBatchId())
                .property(MessageConstants.BATCH_ENTITY_ID, String.valueOf(eArchiveBatch.getEntityId()))
                .property(MessageConstants.DOMAIN, getDomainCode(domain))
                .type(jmsType)
                .build(), eArchiveQueue);
    }

    private String getDomainCode(Domain domain) {
        if (domain == null) {
            return "default";
        }
        return domain.getCode();
    }
}
