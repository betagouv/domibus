package eu.domibus.core.alerts.listener;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.alerts.listener.generic.DefaultEventListener;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.service.AlertService;
import eu.domibus.core.alerts.service.EventService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Thomas Dussart
 * @since 4.0
 */

@ExtendWith(JMockitExtension.class)
public class DefaultEventListenerTest {

    @Injectable
    private EventService eventService;

    @Injectable
    private AlertService alertService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Tested
    private DefaultEventListener userAccountListener;

    @Test
    public void onLoginFailure(@Injectable final Event event) {
        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            times = 1;
            result = "databaseUserName";
        }};

        userAccountListener.onEvent(event, null);

        new Verifications() {{
            domainContextProvider.clearCurrentDomain();
            times = 1;
            eventService.persistEvent(event);
            times = 1;
            alertService.createAndEnqueueAlertOnEvent(event);
            times = 1;
        }};
    }

    @Test
    public void onLoginFailure_domain(@Injectable final Event event) {
        String domain = "domain";

        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            times = 1;
            result = "databaseUserName";
        }};

        userAccountListener.onEvent(event, domain);

        new Verifications() {{
            domainContextProvider.setCurrentDomain(withAny(domain));
            times = 1;
            eventService.persistEvent(event);
            times = 1;
            alertService.createAndEnqueueAlertOnEvent(event);
            times = 1;
        }};
    }
}
