package eu.domibus.core.message.acknowledge;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.UserMessageServiceHelper;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class MessageAcknowledgeDefaultServiceTest {

    @Tested
    MessageAcknowledgeDefaultService messageAcknowledgeDefaultService;

    @Injectable
    MessageAcknowledgementDao messageAcknowledgementDao;

    @Injectable
    AuthUtils authUtils;

    @Injectable
    MessageAcknowledgeConverter messageAcknowledgeConverter;

    @Injectable
    UserMessageServiceHelper userMessageServiceHelper;

    @Injectable
    MessageAcknowledgementPropertyDao messageAcknowledgementPropertyDao;

    @Injectable
    UserMessageDao userMessageDao;

    @Test
    public void testAcknowledgeMessageDelivered(@Injectable final MessageAcknowledgementEntity entity) {
        final String messageId = "1";
        final Timestamp acknowledgeTimestamp = new Timestamp(System.currentTimeMillis());
        final String finalRecipient = "C4";
        final Map<String, String> properties = new HashMap<>();
        properties.put("prop1", "value1");

        final UserMessage userMessage = new UserMessage();
        final String localAccessPointId = "C3";

        new Expectations(messageAcknowledgeDefaultService) {{
            messageAcknowledgeDefaultService.getUserMessage(messageId, MSHRole.RECEIVING);
            result = userMessage;

            messageAcknowledgeDefaultService.getLocalAccessPointId(userMessage);
            result = localAccessPointId;

            userMessageServiceHelper.getFinalRecipientValue(userMessage);
            result = finalRecipient;

            messageAcknowledgeDefaultService.acknowledgeMessage(userMessage, acknowledgeTimestamp, localAccessPointId, finalRecipient, properties, true);

        }};

        messageAcknowledgeDefaultService.acknowledgeMessageDelivered(messageId, acknowledgeTimestamp, properties, true);
    }


    @Test
    public void testAcknowledgeMessageDeliveredWithNoProperties(@Injectable final MessageAcknowledgementEntity entity) {
        final String messageId = "1";
        final Timestamp acknowledgeTimestamp = new Timestamp(System.currentTimeMillis());

        new Expectations(messageAcknowledgeDefaultService) {{
            messageAcknowledgeDefaultService.acknowledgeMessageDelivered(messageId, acknowledgeTimestamp, null, true);

        }};

        messageAcknowledgeDefaultService.acknowledgeMessageDelivered(messageId, acknowledgeTimestamp);

        new Verifications() {{
            messageAcknowledgeDefaultService.acknowledgeMessageDelivered(messageId, acknowledgeTimestamp, null, true);
        }};
    }

    @Test
    public void testAcknowledgeMessageProcessed(@Injectable final MessageAcknowledgementEntity entity) {
        final UserMessage userMessage = new UserMessage();
        final String localAccessPointId = "C3";

        final String messageId = "1";
        final Timestamp acknowledgeTimestamp = new Timestamp(System.currentTimeMillis());
        final String finalRecipient = "C4";
        final Map<String, String> properties = new HashMap<>();
        properties.put("prop1", "value1");

        new Expectations(messageAcknowledgeDefaultService) {{
            messageAcknowledgeDefaultService.getUserMessage(messageId, MSHRole.RECEIVING);
            result = userMessage;

            messageAcknowledgeDefaultService.getLocalAccessPointId(userMessage);
            result = localAccessPointId;

            userMessageServiceHelper.getFinalRecipientValue(userMessage);
            result = finalRecipient;

            messageAcknowledgeDefaultService.acknowledgeMessage(userMessage, acknowledgeTimestamp, finalRecipient, localAccessPointId, properties, true);

        }};

        messageAcknowledgeDefaultService.acknowledgeMessageProcessed(messageId, acknowledgeTimestamp, properties);
    }

    @Test
    public void testGetAcknowledgedMessages() {
        final String messageId = "1";
        final List<MessageAcknowledgementEntity> messageAcknowledgements = new ArrayList<>();
        messageAcknowledgements.add(new MessageAcknowledgementEntity());
        messageAcknowledgements.add(new MessageAcknowledgementEntity());


        new Expectations(messageAcknowledgeDefaultService) {{
            messageAcknowledgementDao.findByMessageId(messageId, MSHRole.RECEIVING);
            result = messageAcknowledgements;

        }};

        messageAcknowledgeDefaultService.getAcknowledgedMessages(messageId);

        new Verifications() {{
            messageAcknowledgeConverter.convert(messageAcknowledgements);
        }};
    }

    @Test
    public void testAcknowledgeMessage(@Injectable final UserMessage userMessage,
                                       @Injectable final MessageAcknowledgementEntity entity) {
        final String messageId = "1";
        final Timestamp acknowledgeTimestamp = new Timestamp(System.currentTimeMillis());
        final String from = "C3";
        final String to = "C4";
        final Map<String, String> properties = new HashMap<>();
        properties.put("prop1", "value1");
        final String user = "baciuco";


        new Expectations(messageAcknowledgeDefaultService) {{
            authUtils.getAuthenticatedUser();
            result = user;

            messageAcknowledgeConverter.create(user, userMessage, acknowledgeTimestamp, from, to);
            result = entity;

        }};

        messageAcknowledgeDefaultService.acknowledgeMessage(userMessage, acknowledgeTimestamp, from, to, properties, true);

        new Verifications() {{
            messageAcknowledgementDao.create(entity);
            messageAcknowledgeConverter.convert(entity, properties);
        }};
    }
}
