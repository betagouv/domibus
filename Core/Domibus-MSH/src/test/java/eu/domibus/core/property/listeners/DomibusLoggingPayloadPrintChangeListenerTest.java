package eu.domibus.core.property.listeners;

import eu.domibus.core.logging.cxf.DomibusLoggingEventSender;
import mockit.FullVerifications;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_LOGGING_PAYLOAD_PRINT;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class DomibusLoggingPayloadPrintChangeListenerTest {

    @Mocked
    private DomibusLoggingEventSender loggingSender;

    protected DomibusLoggingPayloadPrintChangeListener listener;

    @BeforeEach
    public void setUp() {
        listener = new DomibusLoggingPayloadPrintChangeListener(loggingSender);
    }

    @Test
    public void handlesProperty_true() {
        Assertions.assertTrue(listener.handlesProperty(DOMIBUS_LOGGING_PAYLOAD_PRINT));
    }

    @Test
    public void handlesProperty_false() {
        Assertions.assertFalse(listener.handlesProperty("OTHER"));
    }

    @Test
    public void propertyValueChanged() {
        listener.propertyValueChanged("default", DOMIBUS_LOGGING_PAYLOAD_PRINT, "true");

        new FullVerifications() {{
            loggingSender.setPrintPayload(true);
            times = 1;
        }};
    }

    /**
     * Should not happen because of property validation. But default behaviour: set false.
     */
    @Test
    public void propertyValueChanged_invalid() {
        listener.propertyValueChanged("default", DOMIBUS_LOGGING_PAYLOAD_PRINT, "nope");

        new FullVerifications() {{
            loggingSender.setPrintPayload(false);
            times = 1;
        }};
    }
}
