package eu.domibus.core.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.model.MSHRole;
import eu.domibus.common.DeliverMessageEvent;
import eu.domibus.common.NotificationType;
import eu.domibus.core.plugin.notification.NotifyMessageCreator;
import eu.domibus.messaging.MessageConstants;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@ExtendWith(JMockitExtension.class)
public class NotifyMessageCreatorTest {

    @Test
    public void testCreateMessage() throws Exception {
        NotifyMessageCreator creator = new NotifyMessageCreator(MSHRole.RECEIVING, NotificationType.MESSAGE_RECEIVED, null, new ObjectMapper());
        JmsMessage message = creator.createMessage(new DeliverMessageEvent());
//        assertEquals(message.getProperty(MessageConstants.MESSAGE_ID), "myMessageId");
        assertEquals(message.getProperty(MessageConstants.NOTIFICATION_TYPE), NotificationType.MESSAGE_RECEIVED.name());
    }
}
