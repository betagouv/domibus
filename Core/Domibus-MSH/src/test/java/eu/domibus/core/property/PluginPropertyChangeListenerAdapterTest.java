package eu.domibus.core.property;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.plugin.property.PluginPropertyChangeListener;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class PluginPropertyChangeListenerAdapterTest {

    @Tested
    PluginPropertyChangeListenerAdapter pluginPropertyChangeListenerAdapter;

    @Injectable
    PluginPropertyChangeListener pluginPropertyChangeListener;

    @Test
    public void handlesProperty(@Injectable String propertyName) {
        Boolean handles = true;
        new Expectations() {{
            pluginPropertyChangeListener.handlesProperty(propertyName);
            result = handles;
        }};

        boolean res = pluginPropertyChangeListenerAdapter.handlesProperty(propertyName);

        Assertions.assertEquals(handles, res);
    }

    @Test
    public void propertyValueChanged_error(@Injectable String domainCode, @Injectable String propertyName,
                                           @Injectable String propertyValue) {
        String errorMessage = "errorMessage";
        DomibusCoreException cause = new DomibusCoreException("test");
        DomibusPropertyException exception = new DomibusPropertyException(errorMessage, cause);

        new Expectations() {{
            pluginPropertyChangeListener.propertyValueChanged(domainCode, propertyName, propertyValue);
            result = exception;
        }};

        try {
            pluginPropertyChangeListenerAdapter.propertyValueChanged(domainCode, propertyName, propertyValue);
            Assertions.fail();
        } catch (DomibusPropertyException ex) {
            Assertions.assertEquals(ex.getCause(), cause);
            Assertions.assertTrue(ex.getMessage().contains(errorMessage));
        }
    }

    @Test
    public void propertyValueChanged_ok(@Injectable String domainCode, @Injectable String propertyName, @Injectable String propertyValue) {
        pluginPropertyChangeListenerAdapter.propertyValueChanged(domainCode, propertyName, propertyValue);

        new Verifications() {{
            pluginPropertyChangeListener.propertyValueChanged(domainCode, propertyName, propertyValue);
        }};

    }
}
