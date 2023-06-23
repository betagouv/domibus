package eu.domibus.core.property.encryption;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.encryption.PasswordEncryptionContext;
import eu.domibus.api.property.encryption.PasswordEncryptionService;
import eu.domibus.core.property.DomibusRawPropertyProvider;
import eu.domibus.core.property.GlobalPropertyMetadataManager;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class PasswordEncryptionContextFactoryTest {

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private PasswordEncryptionService passwordEncryptionService;

    @Injectable
    private DomibusRawPropertyProvider domibusRawPropertyProvider;

    @Injectable
    private GlobalPropertyMetadataManager globalPropertyMetadataManager;

    @Tested
    PasswordEncryptionContextFactory passwordEncryptionContextFactory;

    @Test
    public void getPasswordEncryptionContextNoDomain() {
        final PasswordEncryptionContext passwordEncryptionContext = passwordEncryptionContextFactory.getPasswordEncryptionContext(null);
        Assertions.assertTrue(passwordEncryptionContext instanceof PasswordEncryptionContextDefault);

        new Verifications() {{
            new PasswordEncryptionContextDefault(passwordEncryptionService, domibusRawPropertyProvider, domibusConfigurationService, globalPropertyMetadataManager);
        }};
    }

    @Test
    public void getPasswordEncryptionContextWithDomain(@Injectable Domain domain) {
        final PasswordEncryptionContext passwordEncryptionContext = passwordEncryptionContextFactory.getPasswordEncryptionContext(domain);
        Assertions.assertTrue(passwordEncryptionContext instanceof PasswordEncryptionContextDomain);

        new Verifications() {{
            new PasswordEncryptionContextDomain(passwordEncryptionService, domibusRawPropertyProvider, domibusConfigurationService, globalPropertyMetadataManager, domain);
        }};
    }
}
