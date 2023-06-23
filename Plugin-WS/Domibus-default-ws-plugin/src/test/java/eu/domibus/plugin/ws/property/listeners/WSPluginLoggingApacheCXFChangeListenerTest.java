package eu.domibus.plugin.ws.property.listeners;

import eu.domibus.plugin.ws.logging.WSPluginLoggingEventSender;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author François Gautier
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginLoggingApacheCXFChangeListenerTest {

    @Injectable
    private LoggingFeature loggingFeature;

    @Injectable
    private WSPluginLoggingEventSender loggingSender;

    protected WSPluginLoggingApacheCXFChangeListener listener;

    @BeforeEach
    public void setUp() {
        listener = new WSPluginLoggingApacheCXFChangeListener(loggingFeature, loggingSender);
    }

    @Test
    public void handlesProperty_true() {
        Assertions.assertTrue(listener.handlesProperty(DOMIBUS_LOGGING_METADATA_PRINT));
    }

    @Test
    public void handlesProperty_false() {
        Assertions.assertFalse(listener.handlesProperty("I hate pickles"));
    }

    @Test
    public void propertyValueChanged() {
        listener.propertyValueChanged("default", DOMIBUS_LOGGING_METADATA_PRINT, "true");
        listener.propertyValueChanged("default", DOMIBUS_LOGGING_CXF_LIMIT, "20000");
        listener.propertyValueChanged("default", DOMIBUS_LOGGING_PAYLOAD_PRINT, "false");

        new FullVerifications() {{
            loggingSender.setPrintMetadata(true);
            times = 1;
            loggingFeature.setLimit(anyInt);
            loggingSender.setPrintPayload(false);
        }};
    }

    /**
     * Should not happen because of property validation. But default behaviour: set false.
     */
    @Test
    public void propertyValueChanged_invalid() {
        listener.propertyValueChanged("default", DOMIBUS_LOGGING_METADATA_PRINT, "nope");

        new FullVerifications() {{
            loggingSender.setPrintMetadata(false);
            times = 1;
        }};
    }
}
