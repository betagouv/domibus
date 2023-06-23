package eu.domibus.plugin.ws.logging;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Catalin Enache
 * @since 4.1.4
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginLoggingEventSenderTest {

    @Injectable
    WSPluginLoggingEventHelper wsPluginLoggingEventHelper;

    @Tested
    WSPluginLoggingEventSender wsPluginLoggingEventSender;

    @Test
    public void test_getLogMessage_StripHeadersPayload(final @Injectable LogEvent logEvent) {
        new Expectations(wsPluginLoggingEventSender) {{
            wsPluginLoggingEventSender.isCxfLoggingInfoEnabled();
            result = true;

            wsPluginLoggingEventSender.checkIfStripPayloadPossible();
            result = true;
        }};

        //tested method
        wsPluginLoggingEventSender.getLogMessage(logEvent);

        new FullVerifications(wsPluginLoggingEventHelper) {{
            wsPluginLoggingEventHelper.stripHeaders((LogEvent) any);
            wsPluginLoggingEventHelper.stripPayload((LogEvent) any);
        }};
    }

    @Test
    public void test_checkIfApacheCxfLoggingInfoEnabled(final @Injectable Logger logger) {
        new MockUp<LoggerFactory>() {
            @Mock
            public Logger getLogger(String value) {
                return logger;
            }
        };

        new Expectations() {{
            logger.isInfoEnabled();
            result = true;
        }};

        //tested method
        Assertions.assertTrue(wsPluginLoggingEventSender.isCxfLoggingInfoEnabled());
    }

    @Test
    public void test_checkIfStripPayloadPossible(final @Injectable Logger logger) {
        new Expectations() {{
            ReflectionTestUtils.setField(wsPluginLoggingEventSender, "printPayload", true);
        }};

        //tested method
        Assertions.assertFalse(wsPluginLoggingEventSender.checkIfStripPayloadPossible());
    }
}
