package eu.domibus.core.crypto.spi.dss.listeners.encryption;

import eu.domibus.core.crypto.spi.dss.DssConfiguration;
import eu.domibus.core.crypto.spi.dss.DssExtensionPropertyManager;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.PasswordEncryptionExtService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * @author Soumya Chandran
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class DssPropertyEncryptionListenerTest {

    @Injectable
    protected PasswordEncryptionExtService pluginPasswordEncryptionService;

    @Injectable
    protected DssConfiguration dssConfiguration;

    @Injectable
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    protected DomainExtService domainsExtService;

    @Injectable
    DssExtensionPropertyManager propertyProvider;

    @Injectable
    DomainContextExtService domainContextProvider;

    @Tested
    DssPropertyEncryptionListener dssPropertyEncryptionListener;

    @Test
    @Disabled("EDELIVERY-6896")
    public void encryptGlobalProperties(@Mocked DssGlobalPasswordEncryptionContext dssPropertyPasswordEncryptionContext) {
        new Expectations() {{
            domainContextProvider.clearCurrentDomain();

            new DssGlobalPasswordEncryptionContext(propertyProvider, domibusConfigurationExtService, pluginPasswordEncryptionService);
            result = dssPropertyPasswordEncryptionContext;
        }};

        dssPropertyEncryptionListener.encryptGlobalProperties();

        new Verifications() {{
            pluginPasswordEncryptionService.encryptPasswordsInFile(dssPropertyPasswordEncryptionContext);
        }};
    }
    
    @Test
    @Disabled("EDELIVERY-6896")
    public void encryptDomainProperties(@Injectable DomainDTO domain,
                                         @Mocked DssDomainPasswordEncryptionContext dssPropertyPasswordEncryptionContext) {
        new Expectations() {{
            domibusConfigurationExtService.isMultiTenantAware();
            result = true;

            domainsExtService.getDomains();
            result = Arrays.asList(domain);

            domainContextProvider.setCurrentDomain(domain);

            new DssDomainPasswordEncryptionContext(propertyProvider, domibusConfigurationExtService, pluginPasswordEncryptionService, domain);
            result = dssPropertyPasswordEncryptionContext;
        }};

        dssPropertyEncryptionListener.encryptPasswords();

        new Verifications() {{
            pluginPasswordEncryptionService.encryptPasswordsInFile(dssPropertyPasswordEncryptionContext);
        }};
    }
}
