package eu.domibus.core.alerts.listener;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.alerts.model.service.Alert;
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
public class PluginListenerTest {

    @Tested
    private PluginEvenListener pluginListener;

    @Injectable
    private EventService eventService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private AlertService alertService;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Test
    public void onPluginEvent_noDomain(@Injectable final Event event, @Injectable final Alert alert) {
        new Expectations(){{
            databaseUtil.getDatabaseUserName();
            result = "userName";
        }};

        pluginListener.onPluginEvent(event, null);

        new Verifications(){{
            domainContextProvider.clearCurrentDomain();
            eventService.persistEvent(event);
            alertService.createAndEnqueueAlertOnPluginEvent(event);
        }};
    }

    @Test
    public void onPluginEvent(@Injectable final Event event, @Injectable final Alert alert) {
        final String domain = "domain";
        new Expectations(){{
            databaseUtil.getDatabaseUserName();
            result = "userName";
        }};

        pluginListener.onPluginEvent(event, domain);

        new Verifications(){{
            domainContextProvider.setCurrentDomain(domain);times=1;
            eventService.persistEvent(event);
            alertService.createAndEnqueueAlertOnPluginEvent(event);
        }};
    }
}
