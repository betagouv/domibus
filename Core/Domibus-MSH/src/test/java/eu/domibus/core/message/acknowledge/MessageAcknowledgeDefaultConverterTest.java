package eu.domibus.core.message.acknowledge;

import eu.domibus.api.message.acknowledge.MessageAcknowledgement;
import eu.domibus.api.model.UserMessage;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class MessageAcknowledgeDefaultConverterTest {

    @Tested
    MessageAcknowledgeDefaultConverter messageAcknowledgeDefaultConverter;

    @Test
    public void testCreate(@Injectable UserMessage userMessage) {
        String user = "baciuco";
        String messageId = "1";
        Timestamp acknowledgeTimestamp = new Timestamp(System.currentTimeMillis());
        String from = "C3";
        String to = "C4";

        final MessageAcknowledgementEntity messageAcknowledgementEntity = messageAcknowledgeDefaultConverter.create(user, userMessage, acknowledgeTimestamp, from, to);
        assertEquals(messageAcknowledgementEntity.getCreatedBy(), user);
        assertEquals(messageAcknowledgementEntity.getAcknowledgeDate(), acknowledgeTimestamp);
        assertEquals(messageAcknowledgementEntity.getFrom(), from);
        assertEquals(messageAcknowledgementEntity.getTo(), to);
    }

    @Test
    public void testConvert(@Injectable UserMessage userMessage)  {
        String user = "baciuco";
        Timestamp acknowledgeTimestamp = new Timestamp(System.currentTimeMillis());
        String from = "C3";
        String to = "C4";

        final MessageAcknowledgementEntity messageAcknowledgementEntity = messageAcknowledgeDefaultConverter.create(user, userMessage, acknowledgeTimestamp, from, to);

        final MessageAcknowledgement converted = messageAcknowledgeDefaultConverter.convert(messageAcknowledgementEntity);
        assertEquals(messageAcknowledgementEntity.getCreatedBy(), converted.getCreateUser());
        assertEquals(messageAcknowledgementEntity.getAcknowledgeDate(), converted.getAcknowledgeDate());
        assertEquals(messageAcknowledgementEntity.getFrom(), converted.getFrom());
        assertEquals(messageAcknowledgementEntity.getTo(), converted.getTo());
    }
}
