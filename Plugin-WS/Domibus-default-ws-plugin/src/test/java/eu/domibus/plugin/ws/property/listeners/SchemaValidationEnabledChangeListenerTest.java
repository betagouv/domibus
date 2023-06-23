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
public class SchemaValidationEnabledChangeListenerTest {

    @Injectable
    private Endpoint backendInterfaceEndpoint;

    @Injectable
    private Endpoint backendInterfaceEndpointDeprecated;

    private SchemaValidationEnabledChangeListener listener;

    @BeforeEach
    public void setUp()  {
        listener = new SchemaValidationEnabledChangeListener(backendInterfaceEndpoint, backendInterfaceEndpointDeprecated);
    }

    @Test
    public void handlesProperty_true() {
        boolean result = listener.handlesProperty("wsplugin.schema.validation.enabled");
        Assertions.assertTrue(result);
    }

    @Test
    public void handlesProperty_false() {
        boolean result = listener.handlesProperty("wsplugin.mtom.enabled");
        Assertions.assertFalse(result);
    }

    @Test
    public void propertyValueChanged() {
        HashMap<String, Object> propBag = new HashMap<>();
        HashMap<String, Object> propBagDeprecated = new HashMap<>();
        new Expectations() {{
            backendInterfaceEndpoint.getProperties();
            result = propBag;
            backendInterfaceEndpointDeprecated.getProperties();
            result = propBagDeprecated;
        }};

        listener.propertyValueChanged("default", "wsplugin.schema.validation.enabled", "true");

        Assertions.assertEquals("true", propBag.get("schema-validation-enabled"));
        Assertions.assertEquals("true", propBagDeprecated.get("schema-validation-enabled"));

        new FullVerifications(){};
    }
}
