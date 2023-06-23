package eu.domibus.plugin.ws.backend.reliability.retry;

import eu.domibus.ext.domain.JmsMessageDTO;
import eu.domibus.ext.services.JMSExtService;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogDao;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import eu.domibus.plugin.ws.backend.reliability.queue.WSSendMessageListener;
import eu.domibus.plugin.ws.backend.rules.WSPluginDispatchRule;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.*;

import static eu.domibus.common.MessageStatus.ACKNOWLEDGED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class WSPluginBackendScheduleRetryServiceTest {
    public static final String FINAL_RECIPIENT = "finalRecipient";
    public static final String ORIGINAL_SENDER = "originalSender";
    public static final String MESSAGE_ID = "messageId";
    public static final long MESSAGE_ENTITY_ID = 555;
    public static final int RETRY_MAX = 10;
    public static final String RULE_NAME = "ruleName";
    public static final long BACKEND_MESSAGE_ID = 1L;
    public static final long BACKEND_MESSAGE_ID2 = 2L;
    public static final String MESSAGE_ID_2 = "messageId2";

    @Tested
    private WSPluginBackendScheduleRetryService retryService;

    @Injectable
    private WSBackendMessageLogDao wsBackendMessageLogDao;

    @Injectable
    protected JMSExtService jmsExtService;

    @Injectable
    protected Queue wsPluginSendQueue;


    @Test
    public void getMessagesNotAlreadyScheduled() {
        List<WSBackendMessageLogEntity> wsBackendMessageLogEntities = Collections.singletonList(new WSBackendMessageLogEntity());
        new Expectations() {{
            wsBackendMessageLogDao.findRetryMessages();
            result = wsBackendMessageLogEntities;
            times = 1;
        }};
        List<WSBackendMessageLogEntity> messagesNotAlreadyScheduled = retryService.getMessagesNotAlreadyScheduled();
        assertEquals(1, messagesNotAlreadyScheduled.size());
        assertNotNull(messagesNotAlreadyScheduled.get(0));
    }

    @Test
    public void getMessagesNotAlreadyScheduled_empty() {
        new Expectations() {{
            wsBackendMessageLogDao.findRetryMessages();
            result = new ArrayList<>();
            times = 1;
        }};
        assertEquals(0, retryService.getMessagesNotAlreadyScheduled().size());
    }

    @Test
    public void sendNotification(@Injectable WSPluginDispatchRule rule) {

        //Since we are making a capture of WSBackendMessageLogEntity, we can not use a Mock object here
        new Expectations() {{


            rule.getRuleName();
            result = RULE_NAME;

            rule.getRetryCount();
            result = RETRY_MAX;
            times = 1;
        }};

        HashMap<String, String> props = new HashMap<>();
        props.put(FINAL_RECIPIENT, FINAL_RECIPIENT);
        props.put(ORIGINAL_SENDER, ORIGINAL_SENDER);
        props.put(MessageConstants.STATUS_TO, ACKNOWLEDGED.name());
        retryService.schedule(MESSAGE_ID, MESSAGE_ENTITY_ID, props, rule, WSBackendMessageType.SEND_SUCCESS);

        new Verifications() {{
            WSBackendMessageLogEntity wsBackendMessageLogEntity;
            wsBackendMessageLogDao.create(wsBackendMessageLogEntity = withCapture());
            times = 1;

            assertEquals(MESSAGE_ID, wsBackendMessageLogEntity.getMessageId());
            assertEquals(RULE_NAME, wsBackendMessageLogEntity.getRuleName());
            assertEquals(ACKNOWLEDGED, wsBackendMessageLogEntity.getMessageStatus());
            assertEquals(FINAL_RECIPIENT, wsBackendMessageLogEntity.getFinalRecipient());
            assertEquals(ORIGINAL_SENDER, wsBackendMessageLogEntity.getOriginalSender());
            assertEquals(WSBackendMessageType.SEND_SUCCESS, wsBackendMessageLogEntity.getType());
            assertEquals(0, wsBackendMessageLogEntity.getSendAttempts());
            assertEquals(RETRY_MAX, wsBackendMessageLogEntity.getSendAttemptsMax());
            assertTrue(wsBackendMessageLogEntity.getScheduled());

            JmsMessageDTO jmsMessageDTO;
            jmsExtService.sendMessageToQueue(jmsMessageDTO = withCapture(), wsPluginSendQueue);
            assertEquals(MESSAGE_ID, jmsMessageDTO.getProperties().get(MessageConstants.MESSAGE_ID));
            assertEquals(0L, jmsMessageDTO.getProperties().get(WSSendMessageListener.ID));
            assertEquals(WSBackendMessageType.SEND_SUCCESS.name(), jmsMessageDTO.getProperties().get(WSSendMessageListener.TYPE));
        }};
    }

    @Test
    public void sendNotifications(@Injectable WSBackendMessageLogEntity entity1, @Injectable WSBackendMessageLogEntity entity2) throws JMSException {
        List<WSBackendMessageLogEntity> entities = Arrays.asList(entity1, entity2);
        new Expectations(retryService) {{
            retryService.getMessagesNotAlreadyScheduled();
            result = entities;
            times = 1;

            entity1.getMessageId();
            result = MESSAGE_ID;

            entity1.getEntityId();
            result = BACKEND_MESSAGE_ID;

            entity1.getType();
            result = WSBackendMessageType.SEND_SUCCESS;

            entity2.getMessageId();
            result = MESSAGE_ID_2;

            entity2.getEntityId();
            result = BACKEND_MESSAGE_ID2;

            entity2.getType();
            result = WSBackendMessageType.SEND_FAILURE;

            wsPluginSendQueue.getQueueName();
            result = "queueName";
        }};

        retryService.scheduleWaitingForRetry();

        new FullVerifications() {{

            List<JmsMessageDTO> jmsMessageDTO = new ArrayList<>();
            jmsExtService.sendMessageToQueue(withCapture(jmsMessageDTO), wsPluginSendQueue);

            assertEquals(MESSAGE_ID, jmsMessageDTO.get(0).getProperties().get(MessageConstants.MESSAGE_ID));
            assertEquals(BACKEND_MESSAGE_ID, jmsMessageDTO.get(0).getProperties().get(WSSendMessageListener.ID));
            assertEquals(WSBackendMessageType.SEND_SUCCESS.name(), jmsMessageDTO.get(0).getProperties().get(WSSendMessageListener.TYPE));

            assertEquals(MESSAGE_ID_2, jmsMessageDTO.get(1).getProperties().get(MessageConstants.MESSAGE_ID));
            assertEquals(BACKEND_MESSAGE_ID2, jmsMessageDTO.get(1).getProperties().get(WSSendMessageListener.ID));
            assertEquals(WSBackendMessageType.SEND_FAILURE.name(), jmsMessageDTO.get(1).getProperties().get(WSSendMessageListener.TYPE));


            entity1.setScheduled(true);
            entity2.setScheduled(true);
        }};
    }

    @Test
    public void send(@Injectable WSPluginDispatchRule rule,
                     @Injectable WSBackendMessageLogEntity backendMessage) {

        new Expectations(retryService) {{
            retryService.createWsBackendMessageLogEntity((List<String>)any, WSBackendMessageType.DELETED_BATCH, FINAL_RECIPIENT, rule);
            result = backendMessage;

            backendMessage.getEntityId();
            result = 1L;

            retryService.scheduleBackendMessage(backendMessage);
            times = 1;

        }};

        retryService.schedule(Arrays.asList("1", "2"), FINAL_RECIPIENT, rule, WSBackendMessageType.DELETED_BATCH);

        new FullVerifications() {{
            wsBackendMessageLogDao.create(backendMessage);
            times = 1;

        }};
    }
}
