package eu.domibus.plugin.jms;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.common.*;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.JmsMessageDTO;
import eu.domibus.ext.domain.metrics.Counter;
import eu.domibus.ext.domain.metrics.Timer;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.ext.services.JMSExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.AbstractBackendConnector;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.jms.property.JmsPluginPropertyManager;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.destination.JndiDestinationResolver;

import javax.jms.*;
import java.text.MessageFormat;
import java.util.List;

import static eu.domibus.plugin.jms.JMSMessageConstants.*;

/**
 * @author Christian Koch, Stefan Mueller
 * @author Cosmin Baciu
 */
public class JMSPluginImpl extends AbstractBackendConnector<MapMessage, MapMessage> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(JMSPluginImpl.class);

    public static final String PLUGIN_NAME = "Jms";

    protected final JMSExtService jmsExtService;
    protected final DomainContextExtService domainContextExtService;
    protected final JMSPluginQueueService jmsPluginQueueService;
    protected final JmsOperations mshToBackendTemplate;
    protected final JMSMessageTransformer jmsMessageTransformer;
    protected final MetricRegistry metricRegistry;
    protected final JndiDestinationResolver jndiDestinationResolver;
    protected final JmsPluginPropertyManager jmsPluginPropertyManager;

    public JMSPluginImpl(MetricRegistry metricRegistry,
                         JMSExtService jmsExtService,
                         DomainContextExtService domainContextExtService,
                         JMSPluginQueueService jmsPluginQueueService,
                         JmsOperations mshToBackendTemplate,
                         JMSMessageTransformer jmsMessageTransformer,
                         JndiDestinationResolver jndiDestinationResolver, JmsPluginPropertyManager jmsPluginPropertyManager) {
        super(PLUGIN_NAME);
        this.jmsExtService = jmsExtService;
        this.domainContextExtService = domainContextExtService;
        this.jmsPluginQueueService = jmsPluginQueueService;
        this.mshToBackendTemplate = mshToBackendTemplate;
        this.jmsMessageTransformer = jmsMessageTransformer;
        this.metricRegistry = metricRegistry;
        this.jndiDestinationResolver = jndiDestinationResolver;
        this.jmsPluginPropertyManager = jmsPluginPropertyManager;
    }

    @Override
    public boolean shouldCoreManageResources() {
        return true;
    }

    @Override
    public MessageSubmissionTransformer<MapMessage> getMessageSubmissionTransformer() {
        return this.jmsMessageTransformer;
    }

    @Override
    public MessageRetrievalTransformer<MapMessage> getMessageRetrievalTransformer() {
        return this.jmsMessageTransformer;
    }

    /**
     * This method is called when a message was received at the incoming queue
     *
     * @param map The incoming JMS Message
     */
    @MDCKey(value = {DomibusLogger.MDC_MESSAGE_ID, DomibusLogger.MDC_MESSAGE_ROLE, DomibusLogger.MDC_MESSAGE_ENTITY_ID}, cleanOnStart = true)
    @Timer(clazz = JMSPluginImpl.class, value = "receiveMessage")
    @Counter(clazz = JMSPluginImpl.class, value = "receiveMessage")
    public void receiveMessage(final MapMessage map) {
        try {
            checkEnabled();

            String messageID = map.getStringProperty(MESSAGE_ID);
            if (StringUtils.isNotBlank(messageID)) {
                //trim the empty space
                messageID = messageExtService.cleanMessageIdentifier(messageID);
                LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageID);
            }
            final String jmsCorrelationID = map.getJMSCorrelationID();
            final String messageType = map.getStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY);

            LOG.info("Received message with messageId [{}], jmsCorrelationID [{}]", messageID, jmsCorrelationID);

            QueueContext queueContext = jmsMessageTransformer.getQueueContext(messageID, map);
            LOG.debug("Extracted queue context [{}]", queueContext);

            if (!MESSAGE_TYPE_SUBMIT.equals(messageType)) {
                String wrongMessageTypeMessage = getWrongMessageTypeErrorMessage(messageID, jmsCorrelationID, messageType);
                LOG.error(wrongMessageTypeMessage);
                sendReplyMessage(queueContext, wrongMessageTypeMessage, jmsCorrelationID);
                return;
            }

            String errorMessage = null;
            try {
                //in case the messageID is not sent by the user it will be generated
                messageID = submit(map);
            } catch (final MessagingProcessingException e) {
                LOG.error("Exception occurred receiving message [{}}], jmsCorrelationID [{}}]", messageID, jmsCorrelationID, e);
                errorMessage = e.getMessage() + ": Error Code: " + (e.getEbms3ErrorCode() != null ? e.getEbms3ErrorCode().getErrorCodeName() : " not set");
            }

            sendReplyMessage(queueContext, errorMessage, jmsCorrelationID);

            LOG.info("Submitted message with messageId [{}], jmsCorrelationID [{}}]", messageID, jmsCorrelationID);
        } catch (Exception e) {
            throw new DefaultJmsPluginException("Exception occurred while receiving message [" + map + "]", e);
        }
    }

    protected String getWrongMessageTypeErrorMessage(String messageID, String jmsCorrelationID, String messageType) {
        return MessageFormat.format("Illegal messageType [{0}] on message with JMSCorrelationId [{1}] and messageId [{2}]. Only [{3}] messages are accepted on this queue",
                messageType, jmsCorrelationID, messageID, MESSAGE_TYPE_SUBMIT);
    }

    protected void sendReplyMessage(QueueContext queueContext, final String errorMessage, final String correlationId) {
        String messageId = queueContext.getMessageId();
        LOG.info("Sending reply message with message id [{}], error message [{}] and correlation id [{}]", messageId, errorMessage, correlationId);

        final JmsMessageDTO jmsMessageDTO = new ReplyMessageCreator(messageId, errorMessage, correlationId).createMessage();
        sendJmsMessage(jmsMessageDTO, queueContext, JMSPLUGIN_QUEUE_REPLY, JMSPLUGIN_QUEUE_REPLY_ROUTING);
    }

    @Override
    @Timer(clazz = JMSPluginImpl.class, value = "deliverMessage")
    @Counter(clazz = JMSPluginImpl.class, value = "deliverMessage")
    public void deliverMessage(final DeliverMessageEvent event) {
        checkEnabled();

        String messageId = event.getMessageId();
        LOG.debug("Delivering message [{}] for final recipient [{}]", messageId, event.getProps().get(MessageConstants.FINAL_RECIPIENT));

        QueueContext queueContext = createQueueContext(event);
        final String queueValue = jmsPluginQueueService.getJMSQueue(queueContext, JMSPLUGIN_QUEUE_OUT, JMSPLUGIN_QUEUE_OUT_ROUTING);
        LOG.info("Sending message to queue [{}]", queueValue);
        mshToBackendTemplate.send(queueValue, new DownloadMessageCreator(event.getMessageEntityId(), queueValue));
    }

    @Override
    public void messageReceiveFailed(MessageReceiveFailureEvent messageReceiveFailureEvent) {
        checkEnabled();

        LOG.debug("Handling messageReceiveFailed");
        final JmsMessageDTO jmsMessageDTO = new ErrorMessageCreator(messageReceiveFailureEvent.getErrorResult(),
                messageReceiveFailureEvent.getEndpoint(),
                NotificationType.MESSAGE_RECEIVED_FAILURE).createMessage();

        QueueContext queueContext = createQueueContext(messageReceiveFailureEvent);
        sendJmsMessage(jmsMessageDTO, queueContext, JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR, JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR_ROUTING);
    }

    @Override
    public void messageSendFailed(final MessageSendFailedEvent event) {
        checkEnabled();

        final ErrorResult errorResult = getErrorResult(event.getMessageId(), MSHRole.SENDING);
        final JmsMessageDTO jmsMessageDTO = new ErrorMessageCreator(errorResult, null, NotificationType.MESSAGE_SEND_FAILURE).createMessage();

        QueueContext queueContext = createQueueContext(event);
        sendJmsMessage(jmsMessageDTO, queueContext, JMSPLUGIN_QUEUE_PRODUCER_NOTIFICATION_ERROR, JMSPLUGIN_QUEUE_PRODUCER_NOTIFICATION_ERROR_ROUTING);
    }

    protected ErrorResult getErrorResult(String messageId, MSHRole mshRole) {
        try {
            List<ErrorResult> errors = super.getErrorsForMessage(messageId, mshRole);
            if (CollectionUtils.isEmpty(errors)) {
                return null;
            }
            return errors.get(errors.size() - 1);
        } catch (final MessageNotFoundException e) {
            LOG.error("Exception occurred while getting errors for message [{}], mshRole [{}]", messageId, mshRole, e);
            throw new DefaultJmsPluginException("Exception occurred while getting errors for message [" + messageId + "]", e);
        }
    }

    private QueueContext createQueueContext(MessageEvent event) {
        final String service = event.getProps().get(MessageConstants.SERVICE);
        final String action = event.getProps().get(MessageConstants.ACTION);
        final String messageId = event.getMessageId();
        QueueContext queueContext = new QueueContext(messageId, service, action);
        return queueContext;
    }

    @Timer(clazz = JMSPluginImpl.class, value = "messageSendSuccess")
    @Counter(clazz = JMSPluginImpl.class, value = "messageSendSuccess")
    @Override
    public void messageSendSuccess(MessageSendSuccessEvent event) {
        checkEnabled();

        LOG.debug("Handling messageSendSuccess");
        final JmsMessageDTO jmsMessageDTO = new SignalMessageCreator(event.getMessageEntityId(), event.getMessageId(), NotificationType.MESSAGE_SEND_SUCCESS).createMessage();

        QueueContext queueContext = createQueueContext(event);
        sendJmsMessage(jmsMessageDTO, queueContext, JMSPLUGIN_QUEUE_REPLY, JMSPLUGIN_QUEUE_REPLY_ROUTING);
    }

    @Override
    public void messageDeletedBatchEvent(final MessageDeletedBatchEvent event) {
        LOG.info("Message delete batch event");
    }

    @Override
    public void messageDeletedEvent(final MessageDeletedEvent event) {
        LOG.info("Message delete event [{}]", event.getMessageId());
    }

    protected void sendJmsMessage(JmsMessageDTO message, QueueContext queueContext, String defaultQueueProperty, String routingQueuePrefixProperty) {
        final String queueValue = jmsPluginQueueService.getJMSQueue(queueContext, defaultQueueProperty, routingQueuePrefixProperty);

        LOG.info("Sending message with message id [{}] to queue [{}]", queueContext.getMessageId(), queueValue);
        jmsExtService.sendMapMessageToQueue(message, queueValue, mshToBackendTemplate);
    }

    @Override
    public MapMessage downloadMessage(final Long messageEntityId, MapMessage target) throws MessageNotFoundException {
        checkEnabled();

        LOG.debug("Downloading message with entity id [{}]", messageEntityId);
        try {
            Submission submission = messageRetriever.downloadMessage(messageEntityId);
            MapMessage result = getMessageRetrievalTransformer().transformFromSubmission(submission, target);

            LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_RETRIEVED);
            return result;
        } catch (Exception ex) {
            LOG.businessError(DomibusMessageCode.BUS_MESSAGE_RETRIEVE_FAILED, ex);
            throw ex;
        }
    }

    private class DownloadMessageCreator implements MessageCreator {
        private String destination;
        private long messageEntityId;

        public DownloadMessageCreator(final long messageEntityId, String destination) {
            this.messageEntityId = messageEntityId;
            this.destination = destination;
        }

        @Override
        @MDCKey(value = {DomibusLogger.MDC_MESSAGE_ID, DomibusLogger.MDC_MESSAGE_ROLE, DomibusLogger.MDC_MESSAGE_ENTITY_ID}, cleanOnStart = true)
        public MapMessage createMessage(final Session session) throws JMSException {
            final MapMessage mapMessage = session.createMapMessage();
            try {
                downloadMessage(messageEntityId, mapMessage);
            } catch (final MessageNotFoundException e) {
                throw new DefaultJmsPluginException("Unable to create push message", e);
            }
            mapMessage.setStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY, JMSMessageConstants.MESSAGE_TYPE_INCOMING);
            final DomainDTO currentDomain = domainContextExtService.getCurrentDomain();
            mapMessage.setStringProperty(MessageConstants.DOMAIN, currentDomain.getCode());

            String queueName = destination;
            if (jndiDestinationResolver != null) {
                Destination jmsDestination = jndiDestinationResolver.resolveDestinationName(session, destination, false);
                LOG.debug("Jms destination [{}] resolved to: [{}]", destination, jmsDestination);
                if (jmsDestination instanceof Queue) {
                    queueName = ((Queue) jmsDestination).getQueueName();
                }
            }
            mapMessage.setStringProperty(JMSMessageConstants.PROPERTY_ORIGINAL_QUEUE, queueName);

            return mapMessage;
        }
    }

    @Override
    public boolean isEnabled(final String domainCode) {
        return doIsEnabled(domainCode);
    }

    @Override
    public void setEnabled(final String domainCode, final boolean enabled) {
        doSetEnabled(domainCode, enabled);
    }

    @Override
    public String getDomainEnabledPropertyName() {
        return JMSPLUGIN_DOMAIN_ENABLED;
    }

    @Override
    public DomibusPropertyManagerExt getPropertyManager() {
        return jmsPluginPropertyManager;
    }
}
