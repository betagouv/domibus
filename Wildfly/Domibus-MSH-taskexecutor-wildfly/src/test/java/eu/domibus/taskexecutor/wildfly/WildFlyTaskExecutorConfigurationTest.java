package eu.domibus.taskexecutor.wildfly;

import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.concurrent.ManagedExecutorService;

@ExtendWith(JMockitExtension.class)
public class WildFlyTaskExecutorConfigurationTest {

    @Tested
    WildFlyTaskExecutorConfiguration wildFlyTaskExecutorConfiguration;

    @Test
    public void domibusExecutorService() {
        DomibusExecutorServiceFactory domibusExecutorServiceFactory = wildFlyTaskExecutorConfiguration.domibusExecutorService();
        Assertions.assertEquals(WildFlyTaskExecutorConfiguration.JAVA_JBOSS_EE_CONCURRENCY_EXECUTOR_DOMIBUS_EXECUTOR_SERVICE, domibusExecutorServiceFactory.getExecutorServiceJndiName());
    }

    @Test
    public void mshExecutorService() {
        DomibusExecutorServiceFactory mshExecutorServiceFactory = wildFlyTaskExecutorConfiguration.mshExecutorService();
        Assertions.assertEquals(WildFlyTaskExecutorConfiguration.JAVA_JBOSS_EE_CONCURRENCY_EXECUTOR_MSH_EXECUTOR_SERVICE, mshExecutorServiceFactory.getExecutorServiceJndiName());
    }

    @Test
    public void quartzExecutorService() {
        DomibusExecutorServiceFactory domibusExecutorServiceFactory = wildFlyTaskExecutorConfiguration.quartzExecutorService();
        Assertions.assertEquals(WildFlyTaskExecutorConfiguration.JAVA_JBOSS_EE_CONCURRENCY_EXECUTOR_QUARTZ_EXECUTOR_SERVICE, domibusExecutorServiceFactory.getExecutorServiceJndiName());
    }

    @Test
    public void taskExecutor(@Injectable ManagedExecutorService managedExecutorService,
                             @Mocked DomibusWildFlyTaskExecutor domibusWildFlyTaskExecutor) {
        wildFlyTaskExecutorConfiguration.taskExecutor(managedExecutorService);

        new Verifications() {{
            domibusWildFlyTaskExecutor.setExecutorService(managedExecutorService);
        }};
    }

    @Test
    public void mshTaskExecutor(@Injectable ManagedExecutorService managedExecutorService,
                             @Mocked DomibusWildFlyTaskExecutor domibusWildFlyTaskExecutor) {
        wildFlyTaskExecutorConfiguration.mshTaskExecutor(managedExecutorService);

        new Verifications() {{
            domibusWildFlyTaskExecutor.setExecutorService(managedExecutorService);
        }};
    }

    @Test
    public void quartzTaskExecutor(@Injectable ManagedExecutorService managedExecutorService,
                                   @Mocked DomibusWildFlyTaskExecutor domibusWildFlyTaskExecutor) {
        wildFlyTaskExecutorConfiguration.quartzTaskExecutor(managedExecutorService);

        new Verifications() {{
            domibusWildFlyTaskExecutor.setExecutorService(managedExecutorService);
        }};
    }
}
