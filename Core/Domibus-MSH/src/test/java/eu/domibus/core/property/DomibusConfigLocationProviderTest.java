package eu.domibus.core.property;

import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
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
    public void getDomibusConfigLocation(@Injectable ServletContext servletContext) {
        String systemConfigLocation = "systemConfigLocation";

        new MockUp<System>() {
            @Mock
            String getProperty(String key) {
                return systemConfigLocation;
            }
        };

        new Expectations() {{
            servletContext.getInitParameter(DomibusPropertyMetadataManagerSPI.DOMIBUS_CONFIG_LOCATION);
            result = null;
        }};

        Assertions.assertEquals(systemConfigLocation, domibusConfigLocationProvider.getDomibusConfigLocation(servletContext));
    }
}
