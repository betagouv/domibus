package eu.domibus.plugin.ws.backend.reliability.retry;

import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.dispatch.WSPluginMessageSender;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginBackendSendRetryWorkerTest {

    @Injectable
    protected WSPluginBackendScheduleRetryService retryService;

    @Injectable
    protected WSPluginMessageSender wsPluginMessageSender;

    @Injectable
    protected DomainExtService domainExtService;

    @Injectable
    protected DomainContextExtService domainContextExtService;

    @Tested
    protected WSPluginBackendSendRetryWorker retryWorker;

    @Test
    public void executeJob(@Injectable WSBackendMessageLogEntity entity1,
                           @Injectable WSBackendMessageLogEntity entity2) {

        retryWorker.executeJob(null, null);

        new FullVerifications() {{
            retryService.scheduleWaitingForRetry();
            times = 1;
        }};
    }
}
