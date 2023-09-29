package eu.domibus.core.property.encryption.plugin;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.encryption.PasswordDecryptionService;
import eu.domibus.api.property.encryption.PasswordEncryptionContext;
import eu.domibus.api.property.encryption.PasswordEncryptionResult;
import eu.domibus.api.property.encryption.PasswordEncryptionService;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.property.encryption.PasswordEncryptionContextFactory;
import eu.domibus.ext.services.PluginPasswordEncryptionContext;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.ext.domain.DomainDTO.DEFAULT_DOMAIN;

/**
 * @author Cosmin Baciu
 * @since 4.1.2
 */
@ExtendWith(JMockitExtension.class)
public class PasswordEncryptionExtServiceImplTest {

    @Tested
    protected PasswordEncryptionExtServiceImpl passwordEncryptionExtService;

    @Injectable
    protected PasswordEncryptionService passwordEncryptionService;

    @Injectable
    protected PasswordDecryptionService passwordDecryptionService;

    @Injectable
    protected PasswordEncryptionContextFactory passwordEncryptionContextFactory;

    @Injectable
    protected DomibusCoreMapper coreMapper;

    @Test
    public void encryptPasswordsInFilePositive(@Injectable PluginPasswordEncryptionContext pluginPasswordEncryptionContext,
                                               @Injectable Domain domain,
                                               @Injectable PasswordEncryptionContext passwordEncryptionContext) {
        new Expectations() {{
            pluginPasswordEncryptionContext.isEncryptionActive();
            result = true;

            pluginPasswordEncryptionContext.getConfigurationFile();
            result = "conf.properties";

            coreMapper.domainDTOToDomain(pluginPasswordEncryptionContext.getDomain());
            result = domain;

            passwordEncryptionContextFactory.getPasswordEncryptionContext(domain);
            result = passwordEncryptionContext;

        }};

        passwordEncryptionExtService.encryptPasswordsInFile(pluginPasswordEncryptionContext);

        new Verifications() {{
            passwordEncryptionService.encryptPasswords((PasswordEncryptionContext) any);
        }};
    }

    @Test
    public void encryptPasswordsInFileNoFile(@Injectable PluginPasswordEncryptionContext pluginPasswordEncryptionContext,
                                             @Injectable Domain domain,
                                             @Injectable PasswordEncryptionContext passwordEncryptionContext,
                                             @Mocked PluginPasswordEncryptionContextDelegate pluginPasswordEncryptionContextDelegate) {
        new Expectations() {{
            pluginPasswordEncryptionContext.isEncryptionActive();
            result = true;

            pluginPasswordEncryptionContext.getConfigurationFile();
            result = null;
        }};

        passwordEncryptionExtService.encryptPasswordsInFile(pluginPasswordEncryptionContext);

        new Verifications() {{
            passwordEncryptionService.encryptPasswords(pluginPasswordEncryptionContextDelegate);
            times = 0;
        }};
    }

    @Test
    public void encryptPasswordsInFileNotActive(@Injectable PluginPasswordEncryptionContext pluginPasswordEncryptionContext,
                                                @Injectable Domain domain,
                                                @Injectable PasswordEncryptionContext passwordEncryptionContext,
                                                @Mocked PluginPasswordEncryptionContextDelegate pluginPasswordEncryptionContextDelegate) {
        new Expectations() {{
            pluginPasswordEncryptionContext.isEncryptionActive();
            result = false;
        }};

        passwordEncryptionExtService.encryptPasswordsInFile(pluginPasswordEncryptionContext);

        new Verifications() {{
            passwordEncryptionService.encryptPasswords(pluginPasswordEncryptionContextDelegate);
            times = 0;
        }};
    }

    @Test
    public void isValueEncrypted() {
        String propertyValue = "";

        passwordEncryptionExtService.isValueEncrypted(propertyValue);

        new FullVerifications() {{
            passwordEncryptionService.isValueEncrypted(propertyValue);
        }};
    }

    @Test
    public void encryptProperty(@Injectable String propertyName,
                                @Injectable String encryptedFormatValue,
                                @Injectable Domain domain,
                                @Injectable PasswordEncryptionResult passwordEncryptionResult) {
        new Expectations() {{
            coreMapper.domainDTOToDomain(DEFAULT_DOMAIN);
            result = domain;

            passwordEncryptionService.encryptProperty(domain, propertyName, encryptedFormatValue);
            result = passwordEncryptionResult;
        }};

        passwordEncryptionExtService.encryptProperty(DEFAULT_DOMAIN, propertyName, encryptedFormatValue);

        new FullVerifications() {{
            coreMapper.passwordEncryptionResultToPasswordEncryptionResultDTO(passwordEncryptionResult);
        }};
    }
}
