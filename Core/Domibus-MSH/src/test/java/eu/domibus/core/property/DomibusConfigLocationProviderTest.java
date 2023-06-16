package eu.domibus.core.property;

import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.ServletContext;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DomibusConfigLocationProviderTest {

    @Tested
    DomibusConfigLocationProvider domibusConfigLocationProvider;

    @Test
    public void getDomibusConfigLocationWithServletInitParameterConfigured(@Injectable ServletContext servletContext) {
        String domibusConfigLocationInitParameter = "servletConfigLocation";
        new Expectations() {{
            servletContext.getInitParameter(DomibusPropertyMetadataManagerSPI.DOMIBUS_CONFIG_LOCATION);
            result = domibusConfigLocationInitParameter;
        }};

        Assertions.assertEquals(domibusConfigLocationInitParameter, domibusConfigLocationProvider.getDomibusConfigLocation(servletContext));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getDomibusConfigLocation(@Injectable ServletContext servletContext,
                                         @Mocked System system) {
        String systemConfigLocation = "systemConfigLocation";
        new Expectations() {{
            servletContext.getInitParameter(DomibusPropertyMetadataManagerSPI.DOMIBUS_CONFIG_LOCATION);
            result = null;

            system.getProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_CONFIG_LOCATION);
            result = systemConfigLocation;
        }};

        Assertions.assertEquals(systemConfigLocation, domibusConfigLocationProvider.getDomibusConfigLocation(servletContext));
    }
}
