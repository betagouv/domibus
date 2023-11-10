package eu.domibus.plugin.ws.backend.reliability.retry;

import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.ext.domain.JMSMessageDTOBuilder;
import eu.domibus.ext.domain.JmsMessageDTO;
import eu.domibus.ext.services.JMSExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogDao;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import eu.domibus.plugin.ws.backend.reliability.queue.WSSendMessageListener;
import eu.domibus.plugin.ws.backend.rules.WSPluginDispatchRule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static eu.domibus.plugin.ws.backend.reliability.queue.WSMessageListenerContainerConfiguration.WS_PLUGIN_SEND_QUEUE;
import static java.lang.String.join;

/**
 * @author Francois Gautier
 * @since 5.0
 */
@Service
public class WSPluginBackendScheduleRetryService {
    public static final String MESSAGE_ID_SEPARATOR = ";";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WSPluginBackendScheduleRetryService.class);

    private final WSBackendMessageLogDao wsBackendMessageLogDao;

    protected JMSExtService jmsExtService;

    protected Queue wsPluginSendQueue;

    public WSPluginBackendScheduleRetryService(WSBackendMessageLogDao wsBackendMessageLogDao,
                                               JMSExtService jmsExtService,
                                               @Qualifier(WS_PLUGIN_SEND_QUEUE) Queue wsPluginSendQueue) {
        this.wsBackendMessageLogDao = wsBackendMessageLogDao;
        this.jmsExtService = jmsExtService;
        this.wsPluginSendQueue = wsPluginSendQueue;
    }

    @Transactional
    public List<WSBackendMessageLogEntity> getMessagesNotAlreadyScheduled() {
        List<WSBackendMessageLogEntity> result = new ArrayList<>();

        final List<WSBackendMessageLogEntity> messageIdsToSend = wsBackendMessageLogDao.findRetryMessages();
        if (CollectionUtils.isEmpty(messageIdsToSend)) {
            LOG.trace("No backend message found to be resend");
            return result;
        }
        LOG.trace("Found [{}] backend messages to be send.", messageIdsToSend.size());

        return messageIdsToSend;
    }

    @Transactional
    public void scheduleWaitingForRetry() {
        try {
            final List<WSBackendMessageLogEntity> messagesNotAlreadyQueued = getMessagesNotAlreadyScheduled();

            for (final WSBackendMessageLogEntity backendMessage : messagesNotAlreadyQueued) {
                scheduleBackendMessage(backendMessage);
            }
        } catch (Exception e) {
            LOG.error("Error while sending notifications.", e);
        }
    }

    public void scheduleBackendMessage(WSBackendMessageLogEntity backendMessage) {
        LOG.debug("Send backendMessage [{}] to queue [{}]", backendMessage.getEntityId(), getQueueName());

        final JmsMessageDTO jmsMessage = JMSMessageDTOBuilder.
                create()
                .property(MessageConstants.MESSAGE_ID, backendMessage.getMessageId())
                .property(MessageConstants.MSH_ROLE, MSHRole.SENDING.name())
                .property(WSSendMessageListener.ID, backendMessage.getEntityId())
                .property(WSSendMessageListener.TYPE, backendMessage.getType().name())
                .build();
        jmsExtService.sendMessageToQueue(jmsMessage, wsPluginSendQueue);
        backendMessage.setScheduled(true);
    }

    private String getQueueName() {
        try {
            return wsPluginSendQueue.getQueueName();
        } catch (JMSException e) {
            LOG.warn("wsPluginSendQueue name not found", e);
            return null;
        }
    }

    @Transactional
    public void schedule(String messageId, long messageEntityId, Map<String, String> props, WSPluginDispatchRule rule, WSBackendMessageType messageType) {
        WSBackendMessageLogEntity backendMessage = createWsBackendMessageLogEntity(messageId, messageEntityId, messageType, props, rule);
        wsBackendMessageLogDao.create(backendMessage);
        scheduleBackendMessage(backendMessage);
    }

    @Transactional
    public void schedule(List<String> messageIds, String finalRecipient, WSPluginDispatchRule rule, WSBackendMessageType messageType) {
        if (CollectionUtils.isEmpty(messageIds)) {
            LOG.info("No message ids provided for recipient [{}] and message type [{}], exiting;", finalRecipient, messageType);
            return;
        }
        WSBackendMessageLogEntity backendMessage = createWsBackendMessageLogEntity(messageIds, messageType, finalRecipient, rule);
        wsBackendMessageLogDao.create(backendMessage);
        LOG.info("Scheduling messageType: [{}] backend message for entity id [{}] for [{}] domibus messages with Ids [{}] to be sent", messageType,
                backendMessage.getEntityId(), messageIds.size(), messageIds);
        scheduleBackendMessage(backendMessage);
    }

    protected WSBackendMessageLogEntity createWsBackendMessageLogEntity(
            List<String> messageIds,
            WSBackendMessageType messageType,
            String finalRecipient,
            WSPluginDispatchRule rule) {
        WSBackendMessageLogEntity entity = createWsBackendMessageLogEntity(messageIds.get(0), messageType, finalRecipient, rule);
        entity.setMessageIds(join(MESSAGE_ID_SEPARATOR, messageIds));
        return entity;
    }

    protected WSBackendMessageLogEntity createWsBackendMessageLogEntity(
            String messageId,
            WSBackendMessageType messageType,
            String finalRecipient,
            WSPluginDispatchRule rule) {
        WSBackendMessageLogEntity wsBackendMessageLogEntity = new WSBackendMessageLogEntity();
        wsBackendMessageLogEntity.setMessageId(messageId);
        wsBackendMessageLogEntity.setRuleName(rule.getRuleName());
        wsBackendMessageLogEntity.setFinalRecipient(finalRecipient);
        wsBackendMessageLogEntity.setType(messageType);
        wsBackendMessageLogEntity.setSendAttempts(0);
        wsBackendMessageLogEntity.setSendAttemptsMax(rule.getRetryCount());
        return wsBackendMessageLogEntity;
    }

    protected WSBackendMessageLogEntity createWsBackendMessageLogEntity(
            String messageId,
            long messageEntityId, WSBackendMessageType messageType,
            Map<String, String> props,
            WSPluginDispatchRule rule) {
        WSBackendMessageLogEntity wsBackendMessageLogEntity = createWsBackendMessageLogEntity(messageId, messageType, props.get(MessageConstants.FINAL_RECIPIENT), rule);
        wsBackendMessageLogEntity.setMessageEntityId(messageEntityId);
        wsBackendMessageLogEntity.setOriginalSender(props.get(MessageConstants.ORIGINAL_SENDER));
        String messageStatus = props.get(MessageConstants.STATUS_TO);
        if (StringUtils.isNotBlank(messageStatus)) {
            wsBackendMessageLogEntity.setMessageStatus(MessageStatus.valueOf(messageStatus));
        }
        return wsBackendMessageLogEntity;
    }
}
