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
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@ExtendWith(JMockitExtension.class)
public class FSPurgeSentWorkerTest {

    @Injectable
    private FSPurgeSentService purgeSentService;

    @Tested
    private FSPurgeSentWorker purgeSentWorker;

    @Injectable
    private DomainExtService domainExtService;

    @Injectable
    private DomainContextExtService domainContextExtService;

    @Test
    public void testExecuteJob(@Injectable final JobExecutionContext context) throws Exception {
        purgeSentWorker.executeJob(context, null);

        new VerificationsInOrder(){{
            purgeSentService.purgeMessages();
        }};
    }

}
