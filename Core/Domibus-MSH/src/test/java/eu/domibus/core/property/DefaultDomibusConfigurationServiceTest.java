package eu.domibus.core.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.ext.LoggerWrapper;

import java.io.File;

import static eu.domibus.api.property.DomibusConfigurationService.PASSWORD_ENCRYPTION_ACTIVE_PROPERTY;
import static eu.domibus.ext.services.DomibusPropertyManagerExt.DOMAINS_HOME;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class DefaultDomibusConfigurationServiceTest {

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    PropertyRetrieveManager propertyRetrieveManager;

    @Injectable
    PrimitivePropertyTypesManager primitivePropertyTypesManager;

    @Injectable
    DomibusLoggerFactory domibusLoggerFactory;

    @Tested
    DefaultDomibusConfigurationService defaultDomibusConfigurationService;

    @Test
    public void isPasswordEncryptionActive() {
        new Expectations(defaultDomibusConfigurationService) {{
            defaultDomibusConfigurationService.getBooleanProperty(PASSWORD_ENCRYPTION_ACTIVE_PROPERTY);
            result = true;
        }};

        Assertions.assertTrue(defaultDomibusConfigurationService.isPasswordEncryptionActive());

    }

    @Test
    public void isPasswordEncryptionActive1(@Injectable Domain domain) {
        new Expectations(defaultDomibusConfigurationService) {{
            defaultDomibusConfigurationService.getBooleanProperty(domain, PASSWORD_ENCRYPTION_ACTIVE_PROPERTY);
            result = true;
        }};

        Assertions.assertTrue(defaultDomibusConfigurationService.isPasswordEncryptionActive(domain));
    }

    @Test
    public void getConfigurationFileName() {
        Assertions.assertEquals(DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE, defaultDomibusConfigurationService.getConfigurationFileName());
    }

    @Test
    public void getConfigurationFileNameDefaultDomain() {
        String domainConfigFile = "default.key";

        new Expectations(defaultDomibusConfigurationService) {{
            defaultDomibusConfigurationService.isSingleTenantAware();
            result = false;

            defaultDomibusConfigurationService.getDomainConfigurationFileName(DomainService.DEFAULT_DOMAIN);
            result = domainConfigFile;
        }};

        final String configurationFileName = defaultDomibusConfigurationService.getConfigurationFileName(DomainService.DEFAULT_DOMAIN);
        Assertions.assertEquals(domainConfigFile, configurationFileName);
    }

    @Test
    public void getConfigurationFileNameCustomDomain(@Injectable Domain domain,
                                                     @Mocked DomibusLoggerFactory domibusLoggerFactory,
                                                     @Mocked LoggerWrapper loggerWrapper) {

        String domainConfigFile = "/homecustom.key";

        new Expectations(defaultDomibusConfigurationService) {{
            defaultDomibusConfigurationService.isSingleTenantAware();
            result = false;
            defaultDomibusConfigurationService.getDomainConfigurationFileName(domain);
            result = domainConfigFile;
        }};

        final String configurationFileName = defaultDomibusConfigurationService.getConfigurationFileName(domain);
        Assertions.assertEquals(domainConfigFile, configurationFileName);
    }

    @Test
    public void getDomainConfigurationFileName(@Injectable Domain domain) {
        String myDomain = "myDomain";
        new Expectations() {{
            domain.getCode();
            result = myDomain;
        }};

        final String domainConfigurationFileName = defaultDomibusConfigurationService.getDomainConfigurationFileName(domain);
        Assertions.assertEquals(DOMAINS_HOME + File.separator + "myDomain" + File.separator + "myDomain-" + DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE, domainConfigurationFileName);
    }
}
