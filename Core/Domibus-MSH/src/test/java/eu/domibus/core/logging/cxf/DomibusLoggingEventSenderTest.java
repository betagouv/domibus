package eu.domibus.core.logging.cxf;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Catalin Enache
 * @since 4.1.4
 */
@ExtendWith(JMockitExtension.class)
public class DomibusLoggingEventSenderTest {

    @Tested
    DomibusLoggingEventSender domibusLoggingEventSender;

    @Injectable
    DomibusLoggingEventHelper domibusLoggingEventHelper;

    @Test
    public void test_getLogMessage(final @Mocked LogEvent logEvent) {
        new Expectations(domibusLoggingEventSender) {{
            domibusLoggingEventSender.checkIfStripPayloadPossible();
            result = true;
            domibusLoggingEventSender.isCxfLoggingInfoEnabled();
            result = true;
        }};

        //tested method
        domibusLoggingEventSender.getLogMessage(logEvent);

        new FullVerifications(domibusLoggingEventSender) {{
            domibusLoggingEventHelper.stripPayload((LogEvent) any);
        }};
    }

    @Test
    public void test_checkIfStripPayloadPossible(final @Mocked Logger logger) {
        new Expectations() {{
            ReflectionTestUtils.setField(domibusLoggingEventSender, "printPayload", true);

        }};

        //tested method
        Assertions.assertFalse(domibusLoggingEventSender.checkIfStripPayloadPossible());
    }
}
