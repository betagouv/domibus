package eu.domibus.core.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainsAware;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.util.WarningUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.api.property.DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;

/**
 * Created by idragusa on 4/14/16.
 */
@Component
public class GatewayConfigurationValidator implements DomainsAware {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(GatewayConfigurationValidator.class);

    private static final String BLUE_GW_ALIAS = "blue_gw";

    private static final String DOMIBUS_PROPERTIES_SHA256 = "domibus.properties.sha256";

    @Autowired
    private DomibusConfigurationService domibusConfigurationService;

    @Autowired
    protected DomainService domainService;

    @Autowired
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    public void validateConfiguration() {
        LOG.info("Checking gateway configuration ...");
        validateCertificates();

        try {
            final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(DOMIBUS_PROPERTIES_SHA256);
            if (resourceAsStream == null) {
                WarningUtil.warnOutput("Could not verify the configuration file hash [" + DOMIBUS_PROPERTIES_SHA256 + "]");
                return;
            }
            try (BufferedReader br = new BufferedReader((new InputStreamReader(resourceAsStream)))) {
                validateFileHash(DOMIBUS_PROPERTY_FILE, br.readLine());
            }
        } catch (Exception e) {
            LOG.warn("Could not verify the configuration file hash", e);
        }
    }

    @Override
    public void onDomainAdded(final Domain domain) {
        validateCerts(domain);
    }

    @Override
    public void onDomainRemoved(Domain domain) {
    }

    public   void validateCertificates() {
        final List<Domain> domains = domainService.getDomains();
        validateCertificates(domains);
    }

    private void validateCertificates(List<Domain> domains) {
        for (Domain domain : domains) {
            validateCerts(domain);
        }
    }

    private void validateCerts(Domain domain) {
        KeyStore trustStore = null;
        try {
            trustStore = multiDomainCertificateProvider.getTrustStore(domain);
        } catch (Exception e) {
            LOG.warn("Failed to load certificates for domain [{}]! : [{}]", domain.getCode(), e.getMessage(), e);
            warnOutput(domain, "CERTIFICATES ARE NOT CONFIGURED PROPERLY - NOT FOR PRODUCTION USAGE");
        }
        if (trustStore == null) {
            LOG.warn("Failed to load certificates for domain [{}]", domain.getCode());
            return;
        }

        try {
            if (trustStore.containsAlias(BLUE_GW_ALIAS)) {
                warnOutput(domain, "SAMPLE CERTIFICATES ARE BEING USED - NOT FOR PRODUCTION USAGE");
            }
        } catch (KeyStoreException e) {
            LOG.warn("Failed to load certificates! " + e.getMessage(), e);
            warnOutput(domain, "CERTIFICATES ARE NOT CONFIGURED PROPERLY - NOT FOR PRODUCTION USAGE");
        }

        //check if the aliases defined in domibus.properties exist in the Keystore
        final String privateKeyRsaSignAlias = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_RSA_SIGN_ALIAS);
        final String privateKeyRsaDecryptAlias = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_RSA_DECRYPT_ALIAS);
        final String privateKeyEccSignAlias = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_ECC_SIGN_ALIAS);
        final String privateKeyEccDecryptAlias = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_ECC_DECRYPT_ALIAS);

        KeyStore keyStore = null;
        try {
            keyStore = multiDomainCertificateProvider.getKeyStore(domain);
        } catch (Exception e) {
            LOG.warn("Failed to load certificates for domain [{}]! : [{}]", domain.getCode(), e.getMessage(), e);
            warnOutput(domain, "CERTIFICATES ARE NOT CONFIGURED PROPERLY");
        }
        if (keyStore == null) {
            LOG.warn("Failed to load certificates for domain [{}]", domain.getCode());
            return;
        }

        if (privateKeyRsaSignAlias != null) {
            validateAlias(privateKeyRsaSignAlias, keyStore, domain);
        }
        if (privateKeyRsaDecryptAlias != null) {
            validateAlias(privateKeyRsaDecryptAlias, keyStore, domain);
        }
        if (privateKeyEccSignAlias != null) {
            validateAlias(privateKeyEccSignAlias, keyStore, domain);
        }
        if (privateKeyEccDecryptAlias != null) {
            validateAlias(privateKeyEccDecryptAlias, keyStore, domain);
        }
    }

    private void validateAlias(String certificateAlias, KeyStore keyStore, Domain domain) {
        try {
            if (keyStore.containsAlias(certificateAlias)) {
                debugOutput(domain, "CERTIFICATE WITH THE FOLLOWING ALIAS WAS FOUND: " + certificateAlias);
            }
        } catch (KeyStoreException e) {
            LOG.warn("Failed to load certificate with alias: [{}] for domain [{}]! : [{}]", certificateAlias, domain.getCode(), e.getMessage(), e);
        }
    }

    private void validateFileHash(String filename, String expectedHash) throws IOException {
        File file = new File(domibusConfigurationService.getConfigLocation(), filename);
        try {
            String hash = DigestUtils.sha256Hex(FileUtils.readFileToByteArray(file));
            LOG.debug("Hash for [{}]: [{}]", filename, hash);
            if (hash.compareTo(expectedHash) == 0) {
                warnOutput("SAMPLE CONFIGURATION FILE IS BEING USED - NOT FOR PRODUCTION USAGE " + filename);
            }

        } catch (IOException e) {
            LOG.error("Failed to read configuration file " + filename + " " + e.getMessage());
            throw e;
        }
    }

    private void warnOutput(String message) {
        LOG.warn(WarningUtil.warnOutput(message));
    }

    private void warnOutput(Domain domain, String message) {
        LOG.warn(WarningUtil.warnOutput("Domain [" + domain.getCode() + "]:" + message));
    }

    private void debugOutput(Domain domain, String message) {
        LOG.debug(WarningUtil.warnOutput("Domain [" + domain.getCode() + "]:" + message));
    }

}
