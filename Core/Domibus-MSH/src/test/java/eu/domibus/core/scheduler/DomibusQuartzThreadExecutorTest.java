package eu.domibus.core.scheduler;

import eu.domibus.api.spring.SpringContextProvider;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;

import static eu.domibus.common.TaskExecutorConstants.DOMIBUS_LONG_RUNNING_TASK_EXECUTOR_BEAN_NAME;

/**
 * @author baciu
 */
@ExtendWith(JMockitExtension.class)
public class DomibusQuartzThreadExecutorTest {

    @Tested
    DomibusQuartzThreadExecutor domibusQuartzThreadExecutor;

    @Injectable
    ApplicationContext applicationContext;

    @Test
    public void testExecute(final @Injectable TaskExecutor taskExecutor, @Mocked SpringContextProvider springContextProvider) throws Exception {
        Thread thread = new Thread();

        new Expectations() {{
            SpringContextProvider.getApplicationContext().getBean(DOMIBUS_LONG_RUNNING_TASK_EXECUTOR_BEAN_NAME, TaskExecutor.class);
            result = taskExecutor;
        }};

        domibusQuartzThreadExecutor.execute(thread);

        new Verifications() {{
            taskExecutor.execute(thread);
        }};
    }
}
