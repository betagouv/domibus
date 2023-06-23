package eu.domibus.core.property;

import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;

import static eu.domibus.api.property.DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DomibusPropertyConfigurationTest {

    @Tested
    DomibusPropertyConfiguration domibusPropertyConfiguration;

    @Test
    public void domibusDefaultProperties(@Mocked PropertiesFactoryBean propertiesFactoryBean) throws IOException {

        domibusPropertyConfiguration.domibusDefaultProperties();

        new Verifications() {{
            Resource[] locations = null;
            propertiesFactoryBean.setLocations(locations = withCapture());
            Assertions.assertEquals("domibus-default.properties", locations[0].getFilename());
            Assertions.assertEquals(DOMIBUS_PROPERTY_FILE, locations[1].getFilename());
        }};

    }

    @Test
    public void domibusProperties(@Mocked PropertiesFactoryBean propertiesFactoryBean) throws IOException {
        String domibusConfigLocation = "configLocation";

        domibusPropertyConfiguration.domibusProperties(domibusConfigLocation);

        new Verifications() {{
            Resource[] locations = null;
            propertiesFactoryBean.setLocations(locations = withCapture());

            Assertions.assertEquals("domibus-default.properties", locations[0].getFilename());
            Assertions.assertEquals(DOMIBUS_PROPERTY_FILE, locations[1].getFilename());
            Assertions.assertEquals(File.separator + domibusConfigLocation + File.separator + DOMIBUS_PROPERTY_FILE, locations[2].getFile().getPath());
        }};
    }

    @Test
    public void propertySourcesPlaceholderConfigurer(@Mocked PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer) {
        domibusPropertyConfiguration.propertySourcesPlaceholderConfigurer();

        new Verifications() {{
            Boolean ignoreResourceNotFound = null;
            Boolean ignoreUnresolvablePlaceholders = null;
            propertySourcesPlaceholderConfigurer.setIgnoreResourceNotFound(ignoreResourceNotFound = withCapture());
            propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(ignoreUnresolvablePlaceholders = withCapture());

            Assertions.assertEquals(true, ignoreResourceNotFound);
            Assertions.assertEquals(true, ignoreUnresolvablePlaceholders);
        }};
    }
}
