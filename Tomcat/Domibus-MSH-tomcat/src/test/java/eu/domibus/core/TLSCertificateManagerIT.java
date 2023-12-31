package eu.domibus.core;

import eu.domibus.AbstractIT;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.TLSCertificateManagerImpl;
import eu.domibus.core.crypto.TruststoreDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static eu.domibus.core.crypto.TLSCertificateManagerImpl.TLS_TRUSTSTORE_NAME;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Transactional
public class TLSCertificateManagerIT extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TLSCertificateManagerIT.class);

    @Autowired
    private TLSCertificateManagerImpl tlsCertificateManager;

    @Autowired
    TruststoreDao truststoreDao;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    CertificateHelper certificateHelper;

    @BeforeEach
    public void clean() {
        resetInitialTruststore();
    }

    @Autowired
    DomainTaskExecutor domainTaskExecutor;

    @Test
    public void persistTrustStoresIfApplicable() {
        List<TrustStoreEntry> storeEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(2, storeEntries.size());

        domainTaskExecutor.submit(() -> createStore(TLS_TRUSTSTORE_NAME, "keystores/gateway_truststore2.jks"),  DomainService.DEFAULT_DOMAIN);

        boolean exists = truststoreDao.existsWithName(TLS_TRUSTSTORE_NAME);
        Assertions.assertTrue(exists);

        tlsCertificateManager.saveStoresFromDBToDisk();

        boolean isPersisted = truststoreDao.existsWithName(TLS_TRUSTSTORE_NAME);
        Assertions.assertFalse(isPersisted);

        storeEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(9, storeEntries.size());
        Assertions.assertTrue(storeEntries.stream().anyMatch(entry -> entry.getName().equals("cefsupportgw")));
    }

    @Test
    public void getTrustStoreEntries() {
        List<TrustStoreEntry> trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(2, trustStoreEntries.size());
    }

    @Test
    public void addCertificate() throws IOException {

        List<TrustStoreEntry> trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(2, trustStoreEntries.size());

        Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, "green_gw.cer");
        byte[] content = Files.readAllBytes(path);
        String green_gw = "green_gw";
        tlsCertificateManager.addCertificate(content, green_gw);

        trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(3, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(green_gw)));
    }

    @Test
    public void removeCertificate() {
        List<TrustStoreEntry> trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(2, trustStoreEntries.size());

        String blue_gw = "blue_gw";
        tlsCertificateManager.removeCertificate(blue_gw);

        trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(1, trustStoreEntries.size());
        Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(blue_gw)));
    }

    @Test
    @Transactional
    public void replaceTrustStore() throws IOException {
        tlsCertificateManager.saveStoresFromDBToDisk();

        List<TrustStoreEntry> trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(2, trustStoreEntries.size());

        Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, "cefsupportgwtruststore.jks");
        byte[] content = Files.readAllBytes(path);
        String file_name = "cefsupportgwtruststore.jks";
        KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(TLS_TRUSTSTORE_NAME, file_name, content, "test123");

        tlsCertificateManager.replaceTrustStore(storeInfo);

        trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        Assertions.assertEquals(9, trustStoreEntries.size());
    }

    private void resetInitialTruststore() {
        try {
            String storePassword = "test123";
            Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, "gateway_truststore_original.jks");
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(TLS_TRUSTSTORE_NAME, "gateway_truststore.jks", content, storePassword);
            tlsCertificateManager.replaceTrustStore(storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }
}
