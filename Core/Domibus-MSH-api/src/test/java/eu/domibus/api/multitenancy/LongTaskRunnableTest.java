package eu.domibus.api.multitenancy;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class LongTaskRunnableTest {

    @Injectable
    protected Runnable runnable;

    @Injectable
    protected Runnable errorHandler;

    @Test
    public void run() {
        new Expectations() {{
            runnable.run();
            result = new DomainTaskException("long running task exception");
        }};

        SetMDCContextTaskRunnable longTaskRunnable = new SetMDCContextTaskRunnable(runnable, errorHandler);
        longTaskRunnable.run();

        new Verifications() {{
            errorHandler.run();
        }};
    }
}
