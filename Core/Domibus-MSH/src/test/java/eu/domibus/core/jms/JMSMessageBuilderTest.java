package eu.domibus.core.jms;

import eu.domibus.api.jms.JMSMessageBuilder;
import eu.domibus.api.jms.JmsMessage;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by Cosmin Baciu on 02-Sep-16.
 */
@ExtendWith(JMockitExtension.class)
public class JMSMessageBuilderTest {

    @Test
    public void testCreateMessage() throws Exception {
        JmsMessage message = JMSMessageBuilder
                .create()
                .property("stringProp", "myString")
                .property("integerProp", "100")
                .property("longProp", "200")
                .build();
        assertEquals(message.getProperty("stringProp"), "myString");
        assertEquals(Integer.parseInt(message.getProperty("integerProp")), 100);
        assertEquals(Long.parseLong(message.getProperty("longProp")), 200L);
    }
}
