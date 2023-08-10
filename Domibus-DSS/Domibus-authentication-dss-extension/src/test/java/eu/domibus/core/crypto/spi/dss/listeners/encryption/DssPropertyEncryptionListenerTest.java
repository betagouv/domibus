package eu.domibus.core.crypto.spi.dss.listeners.encryption;

import eu.domibus.core.crypto.spi.dss.DssConfiguration;
import eu.domibus.core.crypto.spi.dss.DssExtensionPropertyManager;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.*;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

/**
 * @author Soumya Chandran
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
class DssPropertyEncryptionListenerTest {

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
    void encryptGlobalProperties() {
        new Expectations() {{
            domainContextProvider.clearCurrentDomain();
        }};

        dssPropertyEncryptionListener.encryptGlobalProperties();

        new Verifications() {{
            pluginPasswordEncryptionService.encryptPasswordsInFile((PluginPasswordEncryptionContext) any);
        }};
    }

    @Test
    void encryptDomainProperties(@Injectable DomainDTO domain) {
        new Expectations() {{
            domibusConfigurationExtService.isMultiTenantAware();
            result = true;

            domainsExtService.getDomains();
            result = Collections.singletonList(domain);

            domainContextProvider.setCurrentDomain(domain);

        }};

        dssPropertyEncryptionListener.encryptPasswords();

        new Verifications() {{
            pluginPasswordEncryptionService.encryptPasswordsInFile((PluginPasswordEncryptionContext) any);
        }};
    }
}
