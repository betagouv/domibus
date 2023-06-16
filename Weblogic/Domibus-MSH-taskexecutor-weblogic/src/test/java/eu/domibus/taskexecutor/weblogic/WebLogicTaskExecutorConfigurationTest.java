package eu.domibus.taskexecutor.weblogic;

import commonj.work.WorkManager;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class WebLogicTaskExecutorConfigurationTest {

    @Tested
    WebLogicTaskExecutorConfiguration webLogicTaskExecutorConfiguration;

    @Test
    public void workManagerFactory() {
        WorkManagerFactory workManagerFactory = webLogicTaskExecutorConfiguration.workManagerFactory();
        Assertions.assertEquals(WebLogicTaskExecutorConfiguration.JAVA_COMP_ENV_DOMIBUS_WORK_MANAGER, workManagerFactory.getWorkManagerJndiName());
    }

    @Test
    public void quartzWorkManager() {
        WorkManagerFactory workManagerFactory = webLogicTaskExecutorConfiguration.quartzWorkManager();
        Assertions.assertEquals(WebLogicTaskExecutorConfiguration.JAVA_COMP_ENV_QUARTZ_WORK_MANAGER, workManagerFactory.getWorkManagerJndiName());
    }

    @Test
    public void mshWorkManager() {
        WorkManagerFactory workManagerFactory = webLogicTaskExecutorConfiguration.mshWorkManagerFactory();
        Assertions.assertEquals(WebLogicTaskExecutorConfiguration.JAVA_COMP_ENV_MSH_WORK_MANAGER, workManagerFactory.getWorkManagerJndiName());
    }


    @Test
    public void taskExecutor(@Injectable WorkManager workManager) {
        DomibusWorkManagerTaskExecutor domibusWorkManagerTaskExecutor = webLogicTaskExecutorConfiguration.taskExecutor(workManager);
        Assertions.assertEquals(domibusWorkManagerTaskExecutor.workManager, workManager);
    }

    @Test
    public void quartzTaskExecutor(@Injectable WorkManager workManager) {
        DomibusWorkManagerTaskExecutor domibusWorkManagerTaskExecutor = webLogicTaskExecutorConfiguration.quartzTaskExecutor(workManager);
        Assertions.assertEquals(domibusWorkManagerTaskExecutor.workManager, workManager);
    }

    @Test
    public void mshTaskExecutor(@Injectable WorkManager workManager) {
        DomibusWorkManagerTaskExecutor domibusWorkManagerTaskExecutor = webLogicTaskExecutorConfiguration.mshTaskExecutor(workManager);
        Assertions.assertEquals(domibusWorkManagerTaskExecutor.workManager, workManager);
    }
}
