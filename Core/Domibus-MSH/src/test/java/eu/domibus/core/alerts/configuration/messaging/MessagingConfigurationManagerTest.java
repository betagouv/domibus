package eu.domibus.core.alerts.configuration.messaging;

import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.configuration.common.ConfigurationReader;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.service.ConfigurationLoader;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(JMockitExtension.class)
public class MessagingConfigurationManagerTest {

    @Tested
    MessagingConfigurationManager configurationManager;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private ConfigurationLoader<MessagingModuleConfiguration> loader;

    @Injectable
    AlertConfigurationService alertConfigurationService;

    @Test
    public void getAlertType() {
        AlertType res = configurationManager.getAlertType();
        assertEquals(res, AlertType.MSG_STATUS_CHANGED);
    }

    @Test
    public void getConfiguration(@Mocked MessagingModuleConfiguration configuration) {
        new Expectations() {{
            loader.getConfiguration((ConfigurationReader<MessagingModuleConfiguration>) any);
            result = configuration;
        }};
        MessagingModuleConfiguration res = configurationManager.getConfiguration();
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
    public void readConfigurationEachMessagetStatusItsOwnAlertLevel() {
        final String mailSubject = "Messsage status changed";
        final List<String> messageCommunicationStates = new ArrayList<>(Arrays.asList("SEND_FAILURE", "SEND_FAILURE", "SEND_ENQUEUED", "ACKNOWLEDGED"));
        ;
        final List<String> messageCommunicationLevels = new ArrayList<>(Arrays.asList("HIGH", "HIGH", "MEDIUM", "LOW"));

        new Expectations(configurationManager) {{
            configurationManager.isAlertModuleEnabled();
            result = true;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE);
            result = true;
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_STATES);
            result = messageCommunicationStates;
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_LEVEL);
            result = messageCommunicationLevels;
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT);
            this.result = mailSubject;
        }};

        final MessagingModuleConfiguration messagingConfiguration = configurationManager.readConfiguration();
        assertEquals(mailSubject, messagingConfiguration.getMailSubject());
        assertEquals(AlertLevel.HIGH, messagingConfiguration.getAlertLevel(MessageStatus.SEND_FAILURE));
        assertEquals(AlertLevel.LOW, messagingConfiguration.getAlertLevel(MessageStatus.ACKNOWLEDGED));
        assertEquals(3, messagingConfiguration.messageStatusLevels.size());
        assertTrue(messagingConfiguration.isActive());
        new Verifications() {{
            messagingConfiguration.addStatusLevelAssociation(MessageStatus.SEND_FAILURE, AlertLevel.HIGH);
            messagingConfiguration.addStatusLevelAssociation(MessageStatus.SEND_ENQUEUED, AlertLevel.MEDIUM);
            messagingConfiguration.addStatusLevelAssociation(MessageStatus.ACKNOWLEDGED, AlertLevel.LOW);
        }};
    }

    @Test
    public void readConfigurationEachMessagetStatusHasTheSameAlertLevel() {
        final String mailSubject = "Messsage status changed";
        new Expectations(configurationManager) {{
            configurationManager.isAlertModuleEnabled();
            result = true;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE);
            result = true;
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_STATES);
            result = new ArrayList<>(Arrays.asList("SEND_FAILURE", "ACKNOWLEDGED"));
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_LEVEL);
            result = new ArrayList<>(Arrays.asList("HIGH"));
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT);
            this.result = mailSubject;
        }};

        final MessagingModuleConfiguration messagingConfiguration = configurationManager.readConfiguration();

        assertEquals(mailSubject, messagingConfiguration.getMailSubject());
        assertEquals(AlertLevel.HIGH, messagingConfiguration.getAlertLevel(MessageStatus.SEND_FAILURE));
        assertEquals(AlertLevel.HIGH, messagingConfiguration.getAlertLevel(MessageStatus.ACKNOWLEDGED));
        assertTrue(messagingConfiguration.isActive());
    }

    @Test
    public void readConfigurationIncorrectProperty() {
        new Expectations(configurationManager) {{
            configurationManager.isAlertModuleEnabled();
            result = true;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE);
            result = true;
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_STATES);
            result = new ArrayList<>(Arrays.asList("SEND_FLOP"));
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_LEVEL);
            result = new ArrayList<>(Arrays.asList("HIGH"));
        }};
        final MessagingModuleConfiguration messagingConfiguration = configurationManager.readConfiguration();
        assertFalse(messagingConfiguration.isActive());
    }

    @Test
    public void readMessageConfigurationActiveFalse() {
        final String mailSubject = "Messsage status changed";
        new Expectations(configurationManager) {{
            configurationManager.isAlertModuleEnabled();
            this.result = true;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE);
            result = false;
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT);
            result = mailSubject;
        }};
        final MessagingModuleConfiguration messagingConfiguration = configurationManager.readConfiguration();
        assertEquals(messagingConfiguration.getMailSubject(), mailSubject);
    }

    @Test
    public void readMessageConfigurationEmptyStatus() {
        new Expectations(configurationManager) {{
            configurationManager.isAlertModuleEnabled();
            this.result = true;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_ACTIVE);
            result = true;
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_STATES);
            result = new ArrayList<>();
            domibusPropertyProvider.getCommaSeparatedPropertyValues(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_LEVEL);
            result = new ArrayList<>();
        }};
        final MessagingModuleConfiguration messagingConfiguration = configurationManager.readConfiguration();
        assertFalse(messagingConfiguration.isActive());
    }

}
