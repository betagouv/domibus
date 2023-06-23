package eu.domibus.plugin.fs.property.encryption;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.PasswordEncryptionExtService;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class FSPluginPropertyEncryptionListenerTest {

    @Injectable
    protected PasswordEncryptionExtService pluginPasswordEncryptionService;

    @Injectable
    protected FSPluginProperties fsPluginProperties;

    @Injectable
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    protected DomainExtService domainExtService;

    @Injectable
    protected DomainContextExtService domainContextProvider;

    @Tested
    FSPluginPropertyEncryptionListener fsPluginPropertyEncryptionListener;


    @Test
    public void encryptPasswords(@Injectable DomainDTO domainDTO,
                                 @Mocked FSPluginDomainPasswordEncryptionContext globalPasswordEncryptionContext,
                                 @Mocked FSPluginDomainPasswordEncryptionContext domainPasswordEncryptionContext) {
        new Expectations(fsPluginPropertyEncryptionListener) {{
            domibusConfigurationExtService.isMultiTenantAware();
            result = true;

            domainExtService.getDomains();
            result = Arrays.asList(domainDTO);

            fsPluginPropertyEncryptionListener.getGlobalPasswordEncryptionContext();
            result = globalPasswordEncryptionContext;

            fsPluginPropertyEncryptionListener.getDomainPasswordEncryptionContextDomain(domainDTO);
            result = domainPasswordEncryptionContext;
        }};

        fsPluginPropertyEncryptionListener.encryptPasswords();

        new Verifications() {{
            pluginPasswordEncryptionService.encryptPasswordsInFile(globalPasswordEncryptionContext);
            pluginPasswordEncryptionService.encryptPasswordsInFile(domainPasswordEncryptionContext);
        }};
    }
}
