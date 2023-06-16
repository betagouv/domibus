package eu.domibus.plugin.ws.property.listeners;

import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.ws.Endpoint;
import java.util.HashMap;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class MtomEnabledChangeListenerTest {

    @Injectable
    private Endpoint backendInterfaceEndpoint;

    @Injectable
    private Endpoint backendInterfaceEndpointDeprecated;

    private MtomEnabledChangeListener listener;

    @BeforeEach
    public void setUp() {
        listener = new MtomEnabledChangeListener(backendInterfaceEndpoint, backendInterfaceEndpointDeprecated);
    }

    @Test
    public void handlesProperty_false() {
        boolean result = listener.handlesProperty("wsplugin.schema.validation.enabled");
        Assertions.assertFalse(result);
    }

    @Test
    public void handlesProperty_true() {
        boolean result = listener.handlesProperty("wsplugin.mtom.enabled");
        Assertions.assertTrue(result);
    }

    @Test
    public void propertyValueChanged() {
        HashMap<String, Object> prop = new HashMap<>();
        HashMap<String, Object> propDeprecated = new HashMap<>();
        new Expectations() {{
            backendInterfaceEndpoint.getProperties();
            result = prop;
            backendInterfaceEndpointDeprecated.getProperties();
            result = propDeprecated;
        }};

        listener.propertyValueChanged("default", "wsplugin.mtom.enabled", "true");

        Assertions.assertEquals("true", prop.get("mtom-enabled"));
        Assertions.assertEquals("true", propDeprecated.get("mtom-enabled"));

        new FullVerifications() {
        };
    }
}
