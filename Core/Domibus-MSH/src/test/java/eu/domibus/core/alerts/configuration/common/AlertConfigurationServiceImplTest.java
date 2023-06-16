package eu.domibus.core.alerts.configuration.common;

import eu.domibus.api.property.DomibusPropertyChangeNotifier;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.global.CommonConfigurationManager;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.exception.ConfigurationException;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

import java.util.List;

@ExtendWith(JMockitExtension.class)
public class AlertConfigurationServiceImplTest {

    @Tested
    private AlertConfigurationServiceImpl alertConfigurationService;


    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private CommonConfigurationManager commonConfigurationManager;

    @Injectable
    ApplicationContext applicationContext;

    @Injectable
    DomibusPropertyChangeNotifier domibusPropertyChangeNotifier;

    @Test
    public void resetAll(@Mocked AlertConfigurationManager alertConfigurationManager) {
        new Expectations(alertConfigurationService) {{
            alertConfigurationService.getConfigurationManager((AlertType) any);
            result = alertConfigurationManager;
        }};

        alertConfigurationService.resetAll();

        new Verifications() {{
            commonConfigurationManager.reset();
            alertConfigurationService.getConfigurationManager((AlertType) any).reset();
            times = AlertType.values().length;
        }};
    }

    @Test
    public void getMailSubject(@Mocked AlertType alertType, @Mocked AlertModuleConfiguration alertModuleConfiguration) {
        new Expectations(alertConfigurationService) {{
            alertConfigurationService.getConfiguration(alertType);
            result = alertModuleConfiguration;
            alertModuleConfiguration.getMailSubject();
            result = "email subject";
        }};

        String res = alertConfigurationService.getMailSubject(alertType);

        Assertions.assertTrue(res.equals("email subject"));
    }

    @Test
    public void getModuleConfiguration(@Mocked AlertType alertType,
                                       @Mocked AlertConfigurationManager alertConfigurationManager,
                                       @Mocked AlertModuleConfiguration alertModuleConfiguration) {

        new Expectations(alertConfigurationService) {{
            alertConfigurationService.getConfigurationManager(alertType);
            result = alertConfigurationManager;
            alertConfigurationManager.getConfiguration();
            result = alertModuleConfiguration;
        }};

        AlertModuleConfiguration res = alertConfigurationService.getConfiguration(alertType);

        Assertions.assertTrue(res == alertModuleConfiguration);
    }

}
