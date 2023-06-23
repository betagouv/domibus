package eu.domibus.core.alerts.service;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.user.UserEntityBase;
import eu.domibus.core.alerts.configuration.account.AccountDisabledModuleConfiguration;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.user.ui.UserDao;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class ConsoleUserAlertsServiceImplTest {

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private UserDao userDao;

    @Injectable
    private EventService eventService;

    @Injectable
    AlertConfigurationService alertConfigurationService;

    @Tested
    private ConsoleUserAlertsServiceImpl userAlertsService;

    @Test
    public void testGetMaximumDefaultPasswordAgeProperty() {
        String prop = userAlertsService.getMaximumDefaultPasswordAgeProperty();

        Assertions.assertEquals(ConsoleUserAlertsServiceImpl.MAXIMUM_DEFAULT_PASSWORD_AGE, prop);
    }

    @Test
    public void testGetMaximumPasswordAgeProperty() {
        String prop = userAlertsService.getMaximumPasswordAgeProperty();

        Assertions.assertEquals(ConsoleUserAlertsServiceImpl.MAXIMUM_PASSWORD_AGE, prop);
    }

    @Test
    public void testGetEventTypeForPasswordExpired() {
        EventType val = userAlertsService.getEventTypeForPasswordExpired();

        Assertions.assertEquals(EventType.PASSWORD_EXPIRED, val);
    }

    @Test
    public void testGetUserType() {
        UserEntityBase.Type val = userAlertsService.getUserType();

        Assertions.assertEquals(UserEntityBase.Type.CONSOLE, val);
    }

    @Test
    public void testGetAccountDisabledConfiguration(@Injectable AccountDisabledModuleConfiguration accountDisabledModuleConfiguration) {
        new Expectations() {{
            alertConfigurationService.getConfiguration(AlertType.USER_ACCOUNT_DISABLED);
            result = accountDisabledModuleConfiguration;
        }};

        userAlertsService.getAccountDisabledConfiguration();
    }
}
