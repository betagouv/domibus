package eu.domibus.core.alerts.listener;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.alerts.dao.EventDao;
import eu.domibus.core.alerts.listener.generic.RepetitiveEventListener;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.service.AlertService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class RepetitiveEventsListenerTest {

    @Tested
    private RepetitiveEventListener passwordEventsListener;

    @Injectable
    private AlertService alertService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private EventDao eventDao;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Test
    public void testPasswordEvent() throws Exception {
        setExpectations();
        passwordEventsListener.onEvent(new Event(), "default");
        setVerifications();
    }

    void setExpectations() {
        new Expectations() {{
            eventDao.read(anyLong);
            result = new eu.domibus.core.alerts.model.persist.Event();
        }};
    }

    void setVerifications() {
        new VerificationsInOrder() {{
            alertService.createAndEnqueueAlertOnEvent((Event) any);
            times = 1;
        }};
    }
}
