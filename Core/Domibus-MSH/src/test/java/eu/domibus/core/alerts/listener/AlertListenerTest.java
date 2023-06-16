package eu.domibus.core.alerts.listener;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.service.AlertDispatcherService;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class AlertListenerTest {

    @Injectable
    private AlertDispatcherService alertDispatcherService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Tested
    private AlertListener alertListener;

    @Test
    public void onAlertWithDomain(@Mocked final Alert alert) {
        final String domain = "domain";
        alertListener.onAlert(alert, domain);
        new Verifications(){{
            domainContextProvider.setCurrentDomain(domain); times=1;
            alertDispatcherService.dispatch(alert);
        }};
    }

    @Test
    public void onAlertWithSuper(@Mocked final Alert alert) {
        final String domain = null;
        alertListener.onAlert(alert, domain);
        new Verifications(){{
            domainContextProvider.setCurrentDomain(withAny(new Domain())); times=0;
            alertDispatcherService.dispatch(alert);
        }};
    }
}
