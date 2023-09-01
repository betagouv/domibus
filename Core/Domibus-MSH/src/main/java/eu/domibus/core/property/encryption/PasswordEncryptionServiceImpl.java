package eu.domibus.core.property.encryption;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.encryption.PasswordEncryptionContext;
import eu.domibus.api.property.encryption.PasswordEncryptionResult;
import eu.domibus.api.property.encryption.PasswordEncryptionSecret;
import eu.domibus.api.property.encryption.PasswordEncryptionService;
import eu.domibus.api.util.EncryptionUtil;
import eu.domibus.core.property.DomibusRawPropertyProvider;
import eu.domibus.core.util.DomibusEncryptionException;
import eu.domibus.core.util.backup.BackupService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.domibus.core.property.PropertyChangeManager.LINE_COMMENT_PREFIX;
import static eu.domibus.core.property.PropertyChangeManager.PROPERTY_VALUE_DELIMITER;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@Service
public class PasswordEncryptionServiceImpl implements PasswordEncryptionService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PasswordEncryptionServiceImpl.class);

    public static final String ENC_START = "ENC(";
    public static final String ENC_END = ")";

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    protected PasswordEncryptionDao passwordEncryptionDao;

    @Autowired
    protected EncryptionUtil encryptionUtil;

    @Autowired
    protected BackupService backupService;

    // we need Lazy here to avoid circular dependency
    // passwordEncryptionExtService->passwordEncryptionContextFactory->domibusPropertyEncryptionNotifier->FSPluginPropertyEncryptionListener
    @Autowired
    @Lazy
    protected DomibusPropertyEncryptionNotifier domibusPropertyEncryptionListenerDelegate;

    @Autowired
    protected PasswordEncryptionContextFactory passwordEncryptionContextFactory;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected PasswordDecryptionHelper passwordDecryptionHelper;

    @Override
    public boolean isValueEncrypted(String propertyValue) {
        return passwordDecryptionHelper.isValueEncrypted(propertyValue);
    }

    @Override
    public void encryptPasswords() {
        LOG.debug("Encrypting passwords");

        //operate on global context, without a current domain
        domainContextProvider.clearCurrentDomain();
        final PasswordEncryptionContext passwordEncryptionContext = passwordEncryptionContextFactory.getPasswordEncryptionContext(null);
        encryptPasswords(passwordEncryptionContext);

        if (domibusConfigurationService.isMultiTenantAware()) {
            final List<Domain> domains = domainService.getDomains();
            encryptPasswords(domains);
        }

        domibusPropertyEncryptionListenerDelegate.signalEncryptPasswords();

        LOG.debug("Finished encrypting passwords");
    }

    @Override
    public void onDomainAdded(final Domain domain) {
        encryptPasswords(domain);
    }

    @Override
    public void onDomainRemoved(Domain domain) {
        // nothing to do
    }

    private void encryptPasswords(List<Domain> domains) {
        for (Domain domain : domains) {
            encryptPasswords(domain);
        }
    }

    private void encryptPasswords(Domain domain) {
        domainContextProvider.setCurrentDomain(domain);
        final PasswordEncryptionContext passwordEncryptionContextDomain = passwordEncryptionContextFactory.getPasswordEncryptionContext(domain);
        encryptPasswords(passwordEncryptionContextDomain);
        domainContextProvider.clearCurrentDomain();
    }

    @Override
    public void encryptPasswords(PasswordEncryptionContext passwordEncryptionContext) {
        LOG.debug("Encrypting password if configured");

        final Boolean encryptionActive = passwordEncryptionContext.isPasswordEncryptionActive();
        if (isNotTrue(encryptionActive)) {
            LOG.info("Password encryption is not active");
            return;
        }

        final List<String> propertiesToEncrypt = passwordEncryptionContext.getPropertiesToEncrypt();
        if (CollectionUtils.isEmpty(propertiesToEncrypt)) {
            LOG.info("No properties are needed to be encrypted");
            return;
        }

        final File encryptedKeyFile = passwordEncryptionContext.getEncryptedKeyFile();

        PasswordEncryptionSecret secret;
        if (encryptedKeyFile.exists()) {
            secret = passwordEncryptionDao.getSecret(encryptedKeyFile);
        } else {
            secret = passwordEncryptionDao.createSecret(encryptedKeyFile);
        }

        LOG.debug("Using encrypted key file [{}]", encryptedKeyFile);
        final SecretKey secretKey = encryptionUtil.getSecretKey(secret.getSecretKey());
        final GCMParameterSpec secretKeySpec = encryptionUtil.getSecretKeySpec(secret.getInitVector());
        final List<PasswordEncryptionResult> encryptedProperties = encryptProperties(passwordEncryptionContext, propertiesToEncrypt, secretKey, secretKeySpec);

        replacePropertiesInFile(passwordEncryptionContext, encryptedProperties);

        LOG.debug("Finished creating the encryption key");
    }

    protected List<PasswordEncryptionResult> encryptProperties(PasswordEncryptionContext passwordEncryptionContext, List<String> propertiesToEncrypt, SecretKey secretKey, GCMParameterSpec secretKeySpec) {
        List<PasswordEncryptionResult> result = new ArrayList<>();

        LOG.debug("Encrypting properties");

        for (String propertyName : propertiesToEncrypt) {
            final PasswordEncryptionResult passwordEncryptionResult = encryptProperty(passwordEncryptionContext, secretKey, secretKeySpec, propertyName);
            if (passwordEncryptionResult != null) {
                LOG.debug("Property [{}] encrypted [{}]", propertyName, passwordEncryptionResult.getFormattedBase64EncryptedValue());

                result.add(passwordEncryptionResult);
            }
        }

        return result;
    }

    @Override
    public PasswordEncryptionResult encryptProperty(Domain domain, String propertyName, String propertyValue) {
        LOG.debug("Encrypting property [{}] for domain [{}]", propertyName, domain);

        final PasswordEncryptionContext passwordEncryptionContext = passwordEncryptionContextFactory.getPasswordEncryptionContext(domain);

        final Boolean encryptionActive = passwordEncryptionContext.isPasswordEncryptionActive();
        if (isNotTrue(encryptionActive)) {
            throw new DomibusEncryptionException(String.format("Password encryption is not active for domain [%s]", domain));
        }

        final File encryptedKeyFile = passwordEncryptionContext.getEncryptedKeyFile();
        if (!encryptedKeyFile.exists()) {
            throw new DomibusEncryptionException(String.format("Could not find encrypted key file for domain [%s]", domain));
        }

        PasswordEncryptionSecret secret = passwordEncryptionDao.getSecret(encryptedKeyFile);
        LOG.debug("Using encrypted key file [{}]", encryptedKeyFile);

        final SecretKey secretKey = encryptionUtil.getSecretKey(secret.getSecretKey());
        final GCMParameterSpec secretKeySpec = encryptionUtil.getSecretKeySpec(secret.getInitVector());
        return encryptProperty(secretKey, secretKeySpec, propertyName, propertyValue);
    }

    protected PasswordEncryptionResult encryptProperty(PasswordEncryptionContext passwordEncryptionContext, SecretKey secretKey, GCMParameterSpec secretKeySpec, String propertyName) {
        final String propertyValue = passwordEncryptionContext.getProperty(propertyName);
        return encryptProperty(secretKey, secretKeySpec, propertyName, propertyValue);
    }

    protected PasswordEncryptionResult encryptProperty(SecretKey secretKey, GCMParameterSpec secretKeySpec, String propertyName, String propertyValue) {
        if (passwordDecryptionHelper.isValueEncrypted(propertyValue)) {
            LOG.debug("Property [{}] is already encrypted", propertyName);
            return null;
        }

        final byte[] encryptedValue = encryptionUtil.encrypt(propertyValue.getBytes(), secretKey, secretKeySpec);
        final String base64EncryptedValue = Base64.encodeBase64String(encryptedValue);

        final PasswordEncryptionResult passwordEncryptionResult = new PasswordEncryptionResult();
        passwordEncryptionResult.setPropertyName(propertyName);
        passwordEncryptionResult.setPropertyValue(propertyValue);
        passwordEncryptionResult.setBase64EncryptedValue(base64EncryptedValue);
        passwordEncryptionResult.setFormattedBase64EncryptedValue(formatEncryptedValue(base64EncryptedValue));
        return passwordEncryptionResult;
    }

    protected String formatEncryptedValue(String value) {
        return String.format(ENC_START + "%s" + ENC_END, value);
    }

    protected String extractValueFromEncryptedFormat(String encryptedFormat) {
        return StringUtils.substringBetween(encryptedFormat, ENC_START, ENC_END);
    }

    protected void replacePropertiesInFile(PasswordEncryptionContext passwordEncryptionContext, List<PasswordEncryptionResult> encryptedProperties) {
        final File configurationFile = passwordEncryptionContext.getConfigurationFile();

        LOG.debug("Replacing configured properties in file [{}] with encrypted values", configurationFile);
        final List<String> replacedLines = getReplacedLines(encryptedProperties, configurationFile);

        if (!arePropertiesNewlyEncrypted(configurationFile, replacedLines)) {
            LOG.debug("No new properties encrypted in file [{}]", configurationFile);
            return;
        }

        try {
            backupService.backupFile(configurationFile);
        } catch (IOException e) {
            throw new DomibusEncryptionException(String.format("Could not back up [%s]", configurationFile), e);
        }

        LOG.info("Writing encrypted values in file [{}]", configurationFile);

        try {
            Files.write(configurationFile.toPath(), replacedLines);
        } catch (IOException e) {
            throw new DomibusEncryptionException(String.format("Could not write encrypted values to file [%s] ", configurationFile), e);
        }

    }

    protected boolean arePropertiesNewlyEncrypted(File configurationFile, List<String> replacedLines) {
        boolean arePropertiesNewlyEncrypted;

        if (configurationFile == null) {
            LOG.debug("Configuration file should not be null!");
            return false;
        }

        try {
            List<String> originalLines = Files.readAllLines(configurationFile.toPath());
            arePropertiesNewlyEncrypted = !CollectionUtils.containsAll(originalLines, replacedLines);
            LOG.debug("Are properties newly encrypted?: [{}]", arePropertiesNewlyEncrypted);
        } catch (IOException e) {
            throw new DomibusEncryptionException("Could not read configuration file " + configurationFile, e);
        }
        return arePropertiesNewlyEncrypted;
    }

    protected List<String> getReplacedLines(List<PasswordEncryptionResult> encryptedProperties, File configurationFile) {
        try (final Stream<String> lines = Files.lines(configurationFile.toPath())) {
            return lines
                    .map(line -> replaceLine(encryptedProperties, line))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new DomibusEncryptionException(String.format("Could not replace properties: could not read configuration file [%s]", configurationFile), e);
        }
    }

    protected String replaceLine(List<PasswordEncryptionResult> encryptedProperties, String line) {
        if (startsWith(line, LINE_COMMENT_PREFIX) || containsNone(line, PROPERTY_VALUE_DELIMITER)) {
            return line;
        }
        String filePropertyName = trim(substringBefore(line, PROPERTY_VALUE_DELIMITER));

        if (isBlank(substringAfter(line, PROPERTY_VALUE_DELIMITER))) {
            LOG.trace("Property [{}] is empty", filePropertyName);
            return line;
        }
        final Optional<PasswordEncryptionResult> encryptedValueOptional = encryptedProperties.stream()
                .filter(encryptionResult -> arePropertiesMatching(filePropertyName, encryptionResult))
                .findFirst();
        if (!encryptedValueOptional.isPresent()) {
            LOG.trace("Property [{}] is not encrypted", filePropertyName);
            return line;
        }
        final PasswordEncryptionResult passwordEncryptionResult = encryptedValueOptional.get();
        LOG.debug("Replacing value for property [{}] with [{}]", filePropertyName, passwordEncryptionResult.getFormattedBase64EncryptedValue());

        String newLine = filePropertyName + PROPERTY_VALUE_DELIMITER + passwordEncryptionResult.getFormattedBase64EncryptedValue();
        LOG.debug("New encrypted value for property is [{}]", newLine);

        return newLine;
    }

    protected boolean arePropertiesMatching(String filePropertyName, PasswordEncryptionResult encryptionResult) {
        return StringUtils.contains(filePropertyName, encryptionResult.getPropertyName());
    }

    @Deprecated
    public List<String> getPropertiesToEncrypt(String encryptedProperties, Function<String, String> getPropertyFn) {
        return getPropertiesToEncrypt(encryptedProperties, (String) -> Boolean.TRUE, getPropertyFn);
    }

    public List<String> getPropertiesToEncrypt(String encryptedProperties, Function<String, Boolean> handlesPropertyFn, Function<String, String> getPropertyFn) {
        final String propertiesToEncryptString = getPropertyFn.apply(encryptedProperties);
        if (StringUtils.isEmpty(propertiesToEncryptString)) {
            LOG.debug("No properties to encrypt");
            return new ArrayList<>();
        }

        final String[] propertiesToEncrypt = StringUtils.split(propertiesToEncryptString, ",");
        LOG.debug("The following properties are configured for encryption [{}]", Arrays.asList(propertiesToEncrypt));

        List<String> properties = Arrays.stream(propertiesToEncrypt).filter(propertyName -> {
            propertyName = StringUtils.trim(propertyName);

            Boolean handlesProperty = handlesPropertyFn.apply(propertyName);
            if (BooleanUtils.isFalse(handlesProperty)) {
                LOG.debug("Encryption is not supported for property [{}]", propertyName);
                return false;
            }

            final String propertyValue = getPropertyFn.apply(propertyName);
            if (StringUtils.isBlank(propertyValue)) {
                LOG.debug("Cannot encrypt blank value for property [{}]", propertyName);
                return false;
            }
            return !isValueEncrypted(propertyValue);
        }).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(properties)) {
            LOG.debug("The following properties are not encrypted [{}]", properties);
        }

        return properties;
    }
}
