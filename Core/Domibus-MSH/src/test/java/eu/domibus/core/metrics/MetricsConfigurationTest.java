package eu.domibus.core.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author Catalin Enache
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class MetricsConfigurationTest {

    @Tested
    MetricsConfiguration metricsConfiguration;


    @Test
    public void metricRegistry(final @Mocked DomibusPropertyProvider domibusPropertyProvider,
                               final @Mocked JMSManager jmsManager, final @Mocked AuthUtils authUtils,
                               final @Mocked DomainTaskExecutor domainTaskExecutor,
                               final @Mocked MetricRegistry metricRegistry) {

        new Expectations(metricsConfiguration) {{
            metricsConfiguration.createMetricRegistry(domibusPropertyProvider, jmsManager, authUtils, domainTaskExecutor);
            result = metricRegistry;
        }};

        //tested method
        metricsConfiguration.metricRegistry(domibusPropertyProvider, jmsManager, authUtils, domainTaskExecutor);

        new FullVerifications(metricsConfiguration) {{
            metricsConfiguration.addMetricsToLogs(domibusPropertyProvider, metricRegistry);
        }};
    }

    @Test
    public void createMetricRegistry(final @Mocked MetricRegistry metricRegistry,
                                     final @Mocked DomibusPropertyProvider domibusPropertyProvider,
                                     final @Mocked JMSManager jmsManager,
                                     final @Mocked AuthUtils authUtils,
                                     final @Mocked DomainTaskExecutor domainTaskExecutor) {

        new Expectations() {{
            new MetricRegistry();

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_METRICS_MONITOR_MEMORY);
            result = true;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_METRICS_MONITOR_GC);
            result = true;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_METRICS_MONITOR_CACHED_THREADS);
            result = true;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_METRICS_JMX_REPORTER_ENABLE);
            result = true;

        }};

        //tested method
        metricsConfiguration.createMetricRegistry(domibusPropertyProvider, jmsManager, authUtils, domainTaskExecutor);

        new VerificationsInOrder() {{
            String name;
            metricRegistry.register(name = withCapture(), withAny(new MemoryUsageGaugeSet()));
            Assertions.assertEquals("memory", name);

            metricRegistry.register(name = withCapture(), withAny(new GarbageCollectorMetricSet()));
            Assertions.assertEquals("gc", name);

            metricRegistry.register(name = withCapture(), withAny(new CachedThreadStatesGaugeSet(10, TimeUnit.SECONDS)));
            Assertions.assertEquals("threads", name);
        }};
    }
}
