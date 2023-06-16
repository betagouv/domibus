package eu.domibus.core.alerts.model.service;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.alerts.configuration.common.ConfigurationReader;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class ConfigurationLoaderTest {

    @Tested
    private ConfigurationLoader configurationLoader;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Test
    public void getConfigurationForDomain(@Injectable final ConfigurationReader configurationReader, @Injectable final Domain domain) {
        new Expectations() {{
            domainContextProvider.getCurrentDomainSafely();
            result = domain;

            configurationReader.readConfiguration();
            result = new Object();
        }};
        configurationLoader.getConfiguration(configurationReader);
        configurationLoader.getConfiguration(configurationReader);
        new Verifications() {{
            configurationReader.readConfiguration();
            times = 1;
        }};
    }

    @Test
    public void getConfigurationForSuper(@Mocked final ConfigurationReader configurationReader) {
        new Expectations() {{
            domainContextProvider.getCurrentDomainSafely();
            result = null;

            configurationReader.readConfiguration();
            result = new Object();
        }};

        configurationLoader.getConfiguration(configurationReader);
        configurationLoader.getConfiguration(configurationReader);
        
        new Verifications() {{
            configurationReader.readConfiguration();
            times = 1;
        }};
    }
}
