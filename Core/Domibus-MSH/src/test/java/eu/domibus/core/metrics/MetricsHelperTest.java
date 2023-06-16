package eu.domibus.core.metrics;

import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthUtils;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Catalin Enache
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class MetricsHelperTest {

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    private JmsQueueCountSetScheduler jmsQueueCountSetScheduler;

    @Tested
    MetricsHelper metricsHelper;


    @Test
    public void test_showJMSCounts_ST() {
        new Expectations() {{
            domibusConfigurationService.isSingleTenantAware();
            result = true;
        }};

        Assertions.assertTrue(metricsHelper.showJMSCounts());
        new FullVerifications() {{

        }};
    }

    @Test
    public void test_showJMSCounts_MT() {
        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;
            result = true;

            authUtils.isSuperAdmin();
            result = true;
            result = false;
        }};

        Assertions.assertTrue(metricsHelper.showJMSCounts());
        Assertions.assertFalse(metricsHelper.showJMSCounts());
        new FullVerifications() {{
            domibusConfigurationService.isSingleTenantAware();
        }};
    }
}
