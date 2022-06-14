package eu.domibus.ext.services;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.PasswordEncryptionResultDTO;

import java.util.List;
import java.util.function.Function;

/**
 * An interface containing utility operations for encrypting passwords.
 *
 * @author Cosmin Baciu
 * @since 4.1.2
 */
public interface PasswordEncryptionExtService {

    /**
     * Encrypts passwords configured in a properties file
     *
     * @param pluginPasswordEncryptionContext password context used for encrypting passwords. For more info see {@link PluginPasswordEncryptionContext}
     */
    void encryptPasswordsInFile(PluginPasswordEncryptionContext pluginPasswordEncryptionContext);

    /**
     * Checks if a specific property value is encrypted
     *
     * @param propertyValue The property value to check
     * @return true if the value is encrypted
     */
    boolean isValueEncrypted(final String propertyValue);

    /**
     * Encrypts a property value configured for a specific domain
     *
     * @param domain The domain in which the property has been configured
     * @param propertyName The property name
     * @param propertyValue The property value to be encrypted
     * @return The encrypted property result
     */
    PasswordEncryptionResultDTO encryptProperty(DomainDTO domain, String propertyName, String propertyValue);

    List<String> getPropertiesToEncrypt(String encryptedProperties, Function<String, String> getPropertyFn);
}