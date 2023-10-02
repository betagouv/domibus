package eu.domibus.core.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * @since 4.2
 * @author Catalin Enache
 */
@ExtendWith(JMockitExtension.class)
public class DomibusAdminServletTest {

    @Tested
    DomibusAdminServlet domibusAdminServlet;

    @SuppressWarnings("unused")
    @Test
    public void test_init(final @Injectable ServletConfig config,
                          final @Injectable ServletContext servletContext,
                          final @Mocked DomibusMetricsServlet domibusMetricsServlet,
                          final @Mocked MetricsServlet metricsServlet,
                          final @Mocked HealthCheckRegistry healthCheckRegistry,
                          final @Mocked HealthCheckServlet healthCheckServlet) throws Exception {

        new Expectations() {{
            config.getInitParameter(DomibusAdminServlet.METRICS_URI_PARAM_KEY);
            result = "/metrics";
        }};

        //tested method
        domibusAdminServlet.init(config);

        new Verifications() {{
            domibusMetricsServlet.init(config);
        }};
    }


}
