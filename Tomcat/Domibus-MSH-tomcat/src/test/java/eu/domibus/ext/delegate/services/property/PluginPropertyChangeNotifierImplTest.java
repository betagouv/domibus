package eu.domibus.ext.delegate.services.property;


import eu.domibus.api.cluster.SignalService;
import eu.domibus.plugin.property.PluginPropertyChangeListener;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

@ExtendWith(JMockitExtension.class)
public class PluginPropertyChangeNotifierImplTest {

    @Injectable
    private SignalService signalService;

    @Tested
    PluginPropertyChangeNotifierImpl propertyChangeNotifier;

    @Mocked
    PluginPropertyChangeListener changeListener;

    @Test
    public void signalPropertyValueChanged() {
        String domainCode = "domain1";
        String propertyName = "prop1";
        String propertyValue = "val";
        boolean broadcast = true;

        propertyChangeNotifier.pluginPropertyChangeListeners = Arrays.asList(
                changeListener
        );

        new Expectations() {{
            changeListener.handlesProperty(propertyName);
            result = true;
        }};

        propertyChangeNotifier.signalPropertyValueChanged(domainCode, propertyName, propertyValue, broadcast);

        new Verifications() {{
            changeListener.propertyValueChanged(domainCode, propertyName, propertyValue);
            signalService.signalDomibusPropertyChange(domainCode, propertyName, propertyValue);
        }};
    }
}
