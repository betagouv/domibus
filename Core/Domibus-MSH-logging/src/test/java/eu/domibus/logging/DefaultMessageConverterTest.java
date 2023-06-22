package eu.domibus.logging;

import eu.domibus.logging.api.MessageCode;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class DefaultMessageConverterTest {

    @Tested
    DefaultMessageConverter defaultMessageConverter;

    @Test
    public void testGetMessageWithMarker() throws Exception {
        final MessageCode testMessageCode = new MessageCode() {
            @Override
            public String getCode() {
                return "myTestCode";
            }

            @Override
            public String getMessage() {
                return "test message {}";
            }
        };
        final String message = defaultMessageConverter.getMessage(DomibusLogger.BUSINESS_MARKER, testMessageCode, "param1");
        assertEquals("[BUSINESS - myTestCode] test message param1", message);
    }

    @Test
    public void testGetMessage() {
        final MessageCode testMessageCode = new MessageCode() {
            @Override
            public String getCode() {
                return "myTestCode";
            }

            @Override
            public String getMessage() {
                return "test message {}";
            }
        };
        final String message = defaultMessageConverter.getMessage(null, testMessageCode, "param1");
        assertEquals("[myTestCode] test message param1", message);
    }


}
