package eu.domibus.plugin.jms;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.ErrorResult;
import eu.domibus.common.MessageReceiveFailureEvent;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.JmsMessageDTO;
import eu.domibus.ext.services.*;
import eu.domibus.plugin.handler.MessagePuller;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.plugin.jms.property.JmsPluginPropertyManager;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.support.destination.JndiDestinationResolver;

import javax.jms.MapMessage;

import static eu.domibus.plugin.jms.JMSMessageConstants.JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR;
import static eu.domibus.plugin.jms.JMSMessageConstants.JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR_ROUTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class JMSPluginImplTest {

    @Injectable
    protected MessageRetriever messageRetriever;

    @Injectable
    protected MessageSubmitter messageSubmitter;

    @Injectable
    protected MessagePuller messagePuller;

    @Injectable
    private JmsOperations replyJmsTemplate;

    @Injectable
    private JmsOperations mshToBackendTemplate;

    @Injectable
    private JmsOperations errorNotifyConsumerTemplate;

    @Injectable
    private JmsOperations errorNotifyProducerTemplate;

    @Injectable
    protected JMSExtService jmsService;

    @Injectable
    protected DomibusPropertyExtService domibusPropertyService;

    @Injectable
    protected DomainContextExtService domainContextExtService;

    @Injectable
    private MessageExtService messageExtService;

    @Injectable
    protected JMSPluginQueueService JMSPluginQueueService;

    @Injectable
    protected JMSMessageTransformer jmsMessageTransformer;

    @Injectable
    protected JndiDestinationResolver jndiDestinationResolver;

    @Injectable
    protected MetricRegistry metricRegistry;

    @Injectable
    String name = "myjmsplugin";

    @Injectable
    JmsPluginPropertyManager jmsPluginPropertyManager;

    @Injectable
    BackendConnectorProviderExtService backendConnectorProviderExtService;

    @Injectable
    DomainExtService domainExtService;

    @Tested
    JMSPluginImpl backendJMS;

    @Test
    public void testReceiveMessage(@Injectable final MapMessage map,
                                   @Injectable QueueContext queueContext) throws Exception {
        final String messageId = "1";
        final String jmsCorrelationId = "2";
        final String messageTypeSubmit = JMSMessageConstants.MESSAGE_TYPE_SUBMIT;

        new Expectations(backendJMS) {{
            backendJMS.checkEnabled();

            map.getStringProperty(JMSMessageConstants.MESSAGE_ID);
            result = messageId;

            messageExtService.cleanMessageIdentifier(messageId);
            result = messageId;

            map.getJMSCorrelationID();
            result = jmsCorrelationId;

            map.getStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY);
            result = messageTypeSubmit;

            jmsMessageTransformer.getQueueContext(messageId, map);
            result = queueContext;

            backendJMS.submit(withAny(new ActiveMQMapMessage()));
            result = messageId;

            backendJMS.sendReplyMessage(queueContext, anyString, jmsCorrelationId);
        }};

        backendJMS.receiveMessage(map);

        new Verifications() {{
            backendJMS.submit(map);

            String capturedJmsCorrelationId;
            backendJMS.sendReplyMessage(queueContext, null, capturedJmsCorrelationId = withCapture());

            assertEquals(capturedJmsCorrelationId, jmsCorrelationId);
        }};
    }

    @Test
    public void testReceiveMessage_MessageId_WithEmptySpaces(@Injectable final MapMessage map,
                                                             @Injectable QueueContext queueContext) throws Exception {
        final String messageId = " test123 ";
        final String messageIdTrimmed = "test123";
        final String jmsCorrelationId = "2";
        final String messageTypeSubmit = JMSMessageConstants.MESSAGE_TYPE_SUBMIT;

        new Expectations(backendJMS) {{
            backendJMS.checkEnabled();

            map.getStringProperty(JMSMessageConstants.MESSAGE_ID);
            result = messageId;

            messageExtService.cleanMessageIdentifier(messageId);
            result = messageIdTrimmed;

            map.getJMSCorrelationID();
            result = jmsCorrelationId;

            map.getStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY);
            result = messageTypeSubmit;

            backendJMS.submit(withAny(new ActiveMQMapMessage()));
            result = messageIdTrimmed;

            backendJMS.sendReplyMessage((QueueContext) any, anyString, jmsCorrelationId);
        }};

        backendJMS.receiveMessage(map);

        new Verifications() {{
            backendJMS.submit(map);

            String capturedJmsCorrelationId;
            String capturedErrorMessage;
            backendJMS.sendReplyMessage(queueContext, capturedErrorMessage = withCapture(), capturedJmsCorrelationId = withCapture());

            assertEquals(capturedJmsCorrelationId, jmsCorrelationId);
            assertNull(capturedErrorMessage);
        }};
    }

    @Test
    public void testReceiveMessageWithUnacceptedMessage(@Injectable final MapMessage map, @Injectable DomainDTO domain,
                                                        @Injectable QueueContext queueContext) throws Exception {
        final String messageId = "1";
        final String jmsCorrelationId = "2";
        final String unacceptedMessageType = "unacceptedMessageType";

        new Expectations(backendJMS) {{
            backendJMS.checkEnabled();

            map.getStringProperty(JMSMessageConstants.MESSAGE_ID);
            result = messageId;

            messageExtService.cleanMessageIdentifier(messageId);
            result = messageId;

            map.getJMSCorrelationID();
            result = jmsCorrelationId;

            map.getStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY);
            result = unacceptedMessageType;

            jmsMessageTransformer.getQueueContext(messageId, map);
            result = queueContext;

            backendJMS.sendReplyMessage(queueContext, anyString, jmsCorrelationId);
        }};

        backendJMS.receiveMessage(map);

        new Verifications() {{
            String capturedJmsCorrelationId;
            backendJMS.sendReplyMessage(queueContext, anyString, capturedJmsCorrelationId = withCapture());

            assertEquals(jmsCorrelationId, capturedJmsCorrelationId);
        }};
    }

    @Test
    public void testMessageReceiveFailed(@Injectable MessageReceiveFailureEvent messageReceiveFailureEvent,
                                         @Injectable ErrorResult errorResult) {
        final String myEndpoint = "myEndpoint";
        final String messageId = "1";
        final ErrorCode errorCode = ErrorCode.EBMS_0010;
        final String errorDetail = "myError";

        new Expectations(backendJMS) {{
            backendJMS.checkEnabled();

            messageReceiveFailureEvent.getErrorResult();
            result = errorResult;

            errorResult.getErrorCode();
            result = errorCode;

            errorResult.getErrorDetail();
            result = errorDetail;

            messageReceiveFailureEvent.getEndpoint();
            result = myEndpoint;

            messageReceiveFailureEvent.getMessageId();
            result = messageId;
        }};

        backendJMS.messageReceiveFailed(messageReceiveFailureEvent);

        new Verifications() {{
            JmsMessageDTO jmsMessageDTO = null;
            QueueContext queueContext = null;
            backendJMS.sendJmsMessage(jmsMessageDTO = withCapture(), queueContext = withCapture(), JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR, JMSPLUGIN_QUEUE_CONSUMER_NOTIFICATION_ERROR_ROUTING);

            Assertions.assertEquals(errorCode.getErrorCodeName(), jmsMessageDTO.getStringProperty(JMSMessageConstants.ERROR_CODE));
            Assertions.assertEquals(errorDetail, jmsMessageDTO.getStringProperty(JMSMessageConstants.ERROR_DETAIL));
            Assertions.assertEquals(messageId, queueContext.getMessageId());
        }};
    }
}
