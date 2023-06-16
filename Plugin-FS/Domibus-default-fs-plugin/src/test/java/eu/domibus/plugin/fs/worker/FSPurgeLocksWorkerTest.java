package eu.domibus.plugin.fs.worker;

import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomainExtService;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobExecutionContext;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class FSPurgeLocksWorkerTest {

    @Tested
    private FSPurgeLocksWorker fsPurgeLocksWorker;

    @Injectable
    private FSPurgeLocksService fsPurgeLocksService;

    @Injectable
    private DomainExtService domainExtService;

    @Injectable
    private DomainContextExtService domainContextExtService;

    @Test
    public void testExecuteJob(@Injectable final JobExecutionContext context) throws Exception {
        fsPurgeLocksWorker.executeJob(context, null);

        new VerificationsInOrder() {{
            fsPurgeLocksService.purge();
        }};
    }

}
