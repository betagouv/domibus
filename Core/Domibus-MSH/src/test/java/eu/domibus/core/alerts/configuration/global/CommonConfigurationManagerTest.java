package eu.domibus.core.alerts.configuration.global;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.property.validators.DomibusPropertyValidator;
import eu.domibus.core.alerts.model.service.ConfigurationLoader;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.configuration.common.ConfigurationReader;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
public class CommonConfigurationManagerTest {

    @Tested
    CommonConfigurationManager configurationManager;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private ConfigurationLoader<CommonConfiguration> loader;

    @Injectable
    private AlertConfigurationService configurationService;

    @Test
    public void getConfiguration(@Mocked CommonConfiguration configuration) {
        new Expectations() {{
            loader.getConfiguration((ConfigurationReader<CommonConfiguration>) any);
            result = configuration;
        }};
        CommonConfiguration res = configurationManager.getConfiguration();
        assertEquals(res, configuration);
    }

    @Test
    public void reset() {
        configurationManager.reset();
        new Verifications() {{
            loader.resetConfiguration();
        }};
    }

    @Test
    public void readConfiguration() {
        final String sender = "thomas.dussart@ec.eur.europa.com";
        final String receiver = "f.f@f.com";
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MAIL_SENDING_ACTIVE);
            result = true;
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_EMAIL);
            result = sender;
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_RECEIVER_EMAIL);
            result = receiver;
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_CLEANER_ALERT_LIFETIME);
            result = 20;
        }};

        final CommonConfiguration commonConfiguration = configurationManager.readConfiguration();

        assertEquals(sender, commonConfiguration.getSendFrom());
        assertEquals(receiver, commonConfiguration.getSendTo());
        assertEquals(20, commonConfiguration.getAlertLifeTimeInDays(), 0);
    }

    @Test
    public void readDomainEmptyEmailConfiguration() {

        final String sender = "";
        final String receiver = "abc@gmail.com";
        new Expectations() {{

            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_EMAIL);
            result = sender;
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_RECEIVER_EMAIL);
            result = receiver;
        }};
        try {
            configurationManager.readDomainEmailConfiguration(1);
            Assertions.fail();
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals(ex.getMessage(), "Empty sender/receiver email address configured for the alert module.");
        }
    }

    @Test
    public void readDomainInvalidEmailConfiguration() {

        final String sender = "abc.def@mail#g.c";
        final String receiver = "abcd@gmail.com";
        List<String> emailsToValidate = new ArrayList<>();
        emailsToValidate.add(sender);
        emailsToValidate.add(receiver);
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.EMAIL.getValidator();
        new Expectations(configurationManager) {{

            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_EMAIL);
            result = sender;
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_RECEIVER_EMAIL);
            result = receiver;
            validator.isValid(sender);
            result = false;
        }};
        try {
            configurationManager.readDomainEmailConfiguration(1);
            Assertions.fail();
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals(ex.getMessage(), "Invalid sender/receiver email address configured for the alert module: abc.def@mail#g.c");
        }
    }
}
