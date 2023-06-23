package eu.domibus.core.property;

import eu.domibus.api.cluster.SignalService;
import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.core.property.listeners.BlacklistChangeListener;
import eu.domibus.core.property.listeners.ConcurrencyChangeListener;
import eu.domibus.core.property.listeners.CronExpressionChangeListener;
import eu.domibus.plugin.property.PluginPropertyChangeListener;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class PropertyChangeNotifierImplTest {

    @Tested
    PropertyChangeNotifierImpl domibusPropertyChangeNotifier;

    @Injectable
    List<DomibusPropertyChangeListener> propertyChangeListeners;

    @Injectable
    List<PluginPropertyChangeListener> pluginPropertyChangeListeners;

    @Injectable
    SignalService signalService;

    @Mocked
    BlacklistChangeListener blacklistChangeListener;

    @Mocked
    ConcurrencyChangeListener concurrencyChangeListener;

    @Mocked
    CronExpressionChangeListener cronExpressionChangeListener;

    @Test
    public void signalPropertyValueChanged() {
        String domainCode = "domain1";
        String propertyName = "prop1";
        String propertyValue = "val";
        boolean broadcast = true;

        domibusPropertyChangeNotifier.allPropertyChangeListeners = Arrays.asList(
                blacklistChangeListener,
                concurrencyChangeListener,
                cronExpressionChangeListener
        );

        new Expectations() {{
            blacklistChangeListener.handlesProperty(propertyName);
            result = false;
            concurrencyChangeListener.handlesProperty(propertyName);
            result = true;
            cronExpressionChangeListener.handlesProperty(propertyName);
            result = false;
        }};

        domibusPropertyChangeNotifier.signalPropertyValueChanged(domainCode, propertyName, propertyValue, broadcast);

        new Verifications() {{
            concurrencyChangeListener.propertyValueChanged(domainCode, propertyName, propertyValue);
            signalService.signalDomibusPropertyChange(domainCode, propertyName, propertyValue);
        }};
    }

    @Test
    void signalPropertyValueChanged_error() {
        String domainCode = "domain1";
        String propertyName = "prop1";
        String propertyValue = "val";
        boolean broadcast = true;

        domibusPropertyChangeNotifier.allPropertyChangeListeners = Arrays.asList(blacklistChangeListener);

        new Expectations() {{
            blacklistChangeListener.handlesProperty(propertyName);
            result = true;
            blacklistChangeListener.propertyValueChanged(domainCode, propertyName, propertyValue);
            result = new Exception("");
        }};

        Assertions.assertThrows(DomibusPropertyException. class,
        () -> domibusPropertyChangeNotifier.signalPropertyValueChanged(domainCode, propertyName, propertyValue, broadcast))
        ;
        Assertions.fail();

        new Verifications() {{
            signalService.signalDomibusPropertyChange(domainCode, propertyName, propertyValue);
            times = 0;
        }};
    }

    @Test
    void signalPropertyValueChanged_error2() {
        String domainCode = "domain1";
        String propertyName = "prop1";
        String propertyValue = "val";
        boolean broadcast = true;

        domibusPropertyChangeNotifier.allPropertyChangeListeners = Arrays.asList(blacklistChangeListener);

        new Expectations() {{
            blacklistChangeListener.handlesProperty(propertyName);
            result = true;
            signalService.signalDomibusPropertyChange(domainCode, propertyName, propertyValue);
            result = new Exception("");
        }};

        Assertions.assertThrows(DomibusPropertyException. class,
        () -> domibusPropertyChangeNotifier.signalPropertyValueChanged(domainCode, propertyName, propertyValue, broadcast))
        ;
        Assertions.fail();

        new Verifications() {{
            blacklistChangeListener.handlesProperty(propertyName);
        }};
    }
}
