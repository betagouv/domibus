package eu.domibus.core;

import eu.domibus.AbstractIT;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.lock.DBClusterSynchronizedRunnable;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.pki.KeystorePersistenceInfo;
import eu.domibus.api.pki.KeystorePersistenceService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.api.util.FileServiceUtil;
import eu.domibus.core.certificate.Certificate;
import eu.domibus.core.certificate.CertificateDaoImpl;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.certificate.CertificateServiceImpl;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.core.crypto.TruststoreDao;
import eu.domibus.core.crypto.spi.CryptoSpiException;
import eu.domibus.core.util.SecurityUtilImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.AssertFalse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static eu.domibus.api.property.DomibusConfigurationService.CLUSTER_DEPLOYMENT;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_TRUSTSTORE_LOCATION;
import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;
import static eu.domibus.core.spring.DomibusApplicationContextListener.SYNC_LOCK_KEY;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Ion Perpegel
 * @since 5.0
 */

public class MultiDomainCryptoServiceIT extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MultiDomainCryptoServiceIT.class);

    @Autowired
    private MultiDomainCryptoServiceImpl multiDomainCryptoService;

    @Autowired
    TruststoreDao truststoreDao;

    @Autowired
    CertificateServiceImpl certificateService;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    CertificateDaoImpl certificateDao;

    @Autowired
    CertificateHelper certificateHelper;

    @Autowired
    KeystorePersistenceService keystorePersistenceService;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    KeystorePersistenceService getKeystorePersistenceService;

    @Autowired
    SecurityUtilImpl securityUtil;

    Domain domain;
    String location, back;

    @BeforeEach
    public void doBefore() {
        domain = DomainService.DEFAULT_DOMAIN;

        final LocalDateTime localDateTime = LocalDateTime.of(0, 1, 1, 0, 0);
        final LocalDateTime offset = localDateTime.minusDays(15);
        final LocalDateTime notification = localDateTime.minusDays(7);
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        List<Certificate> certs2 = certificateDao.findExpiredToNotifyAsAlert(getDate(notification), getDate(offset));
        certificateDao.deleteAll(certs2);

        resetInitialTruststore();

        // back-up initial trust file
        location = domibusPropertyProvider.getProperty(DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        back = location.replace("gateway_truststore.jks", "gateway_truststore_back.jks");
        try {
            Files.copy(Paths.get(location), Paths.get(back), REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error("Error backing trsustore file", e);
        }
    }

    @BeforeEach
    public void doAfter() {
        //restore initial trust store
        if (back != null && location != null) {
            try {
                Files.copy(Paths.get(back), Paths.get(location), REPLACE_EXISTING);
                Files.delete(Paths.get(back));
            } catch (IOException e) {
                LOG.error("Error restoring trsustore file", e);
            }
        }
        resetClusterDeploymentProperty();
    }

    private void resetClusterDeploymentProperty() {
        domainContextProvider.clearCurrentDomain();
        domibusPropertyProvider.setProperty(CLUSTER_DEPLOYMENT, "false");
    }

    @Test
    @Disabled
    public void saveStoresFromDBToDisk() {

        createStore(DOMIBUS_TRUSTSTORE_NAME, "keystores/gateway_truststore2.jks");

        boolean exists = truststoreDao.existsWithName(DOMIBUS_TRUSTSTORE_NAME);
        Assertions.assertTrue(exists);

        multiDomainCryptoService.saveStoresFromDBToDisk();

        exists = truststoreDao.existsWithName(DOMIBUS_TRUSTSTORE_NAME);
        Assertions.assertFalse(exists);

        List<TrustStoreEntry> storeEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, storeEntries.size());
        Assertions.assertTrue(storeEntries.stream().noneMatch(entry -> entry.getName().equals("cefsupportgw")));

        multiDomainCryptoService.resetTrustStore(domain);

        storeEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(9, storeEntries.size());
        Assertions.assertTrue(storeEntries.stream().anyMatch(entry -> entry.getName().equals("cefsupportgw")));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void replaceTrustStore() throws IOException {
        String newStoreName = "gateway_truststore2.jks";
        String storePassword = "test123";


        Path path = getPath(newStoreName);
        byte[] content = Files.readAllBytes(path);
        KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, newStoreName, content, storePassword);

        KeyStore initialStore = multiDomainCryptoService.getTrustStore(domain);
        KeyStoreContentInfo initialStoreContent = multiDomainCryptoService.getTrustStoreContent(domain);
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);

        multiDomainCryptoService.replaceTrustStore(domain, storeInfo);

        KeyStore newStore = multiDomainCryptoService.getTrustStore(domain);
        List<TrustStoreEntry> newStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        KeyStoreContentInfo newStoreContent = multiDomainCryptoService.getTrustStoreContent(domain);

        Assertions.assertNotEquals(initialStore, newStore);
        Assertions.assertNotEquals(initialStoreContent.getContent(), newStoreContent.getContent());
        Assertions.assertNotEquals(initialStoreEntries.size(), newStoreEntries.size());
    }

    private Path getPath(String newStoreName) {
        return Paths.get(domibusConfigurationService.getConfigLocation(), "domains", domain.getCode(), KEYSTORES, newStoreName);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getTrustStoreEntries() {
        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(DomainService.DEFAULT_DOMAIN);

        KeystorePersistenceInfo trustPersistInfo = keystorePersistenceService.getTrustStorePersistenceInfo();
        List<TrustStoreEntry> trustStoreEntries2 = certificateService.getStoreEntries(trustPersistInfo);

        Assertions.assertEquals(trustStoreEntries2.size(), trustStoreEntries.size());
        Assertions.assertEquals(trustStoreEntries2.get(0), trustStoreEntries.get(0));

        Assertions.assertEquals(2, trustStoreEntries.size());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void addCertificate() throws IOException {
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());

        String certName = "green_gw.cer";
        Path path = getPath(certName);
        byte[] content = Files.readAllBytes(path);
        String green_gw = "green_gw";
        X509Certificate x509Certificate = certificateService.loadCertificate(Base64.getEncoder().encodeToString(content));
        multiDomainCryptoService.addCertificate(domainContextProvider.getCurrentDomain(), Arrays.asList(new CertificateEntry(green_gw, x509Certificate)), true);

        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(3, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(green_gw)));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void addSameCertificate() throws IOException {
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());

        String certName = "green_gw.cer";
        Path path = getPath(certName);
        byte[] content = Files.readAllBytes(path);
        String green_gw = "green_gw";
        X509Certificate x509Certificate = certificateService.loadCertificate(Base64.getEncoder().encodeToString(content));

        multiDomainCryptoService.addCertificate(domain, Arrays.asList(new CertificateEntry(green_gw, x509Certificate)), true);

        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(3, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(green_gw)));

        boolean added = multiDomainCryptoService.addCertificate(domain, x509Certificate, green_gw, true);
        Assertions.assertFalse(added);
        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(3, trustStoreEntries.size());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getCertificateFromTruststore() throws KeyStoreException {
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);

        Assertions.assertEquals(2, initialStoreEntries.size());

        String blue_gw = "blue_gw";
        X509Certificate certificateFromTruststore = multiDomainCryptoService.getCertificateFromTruststore(domain, blue_gw);

        Assertions.assertTrue(certificateFromTruststore.getIssuerDN().getName().contains(blue_gw));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getTrustStoreReplaceTrustStore() throws KeyStoreException, IOException {
        String file_name = "cefsupportgwtruststore.jks";

        KeyStore trustStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertTrue(trustStore.containsAlias("blue_gw"));

        Path path = getPath(file_name);
        byte[] content = Files.readAllBytes(path);

        String password = "test123";
        KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, file_name, content, password);

        multiDomainCryptoService.replaceTrustStore(DomainService.DEFAULT_DOMAIN, storeInfo);

        trustStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertTrue(trustStore.containsAlias("ceftestparty4gw"));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void isCertificateChainValid() {
        String blue_gw = "blue_gw";
        boolean certificateChainValid = multiDomainCryptoService.isCertificateChainValid(domainContextProvider.getCurrentDomain(), blue_gw);

        Assertions.assertTrue(certificateChainValid);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getDefaultX509Identifier() throws WSSecurityException {
        String blue_gw = "blue_gw";
        String defaultX509Identifier = multiDomainCryptoService.getDefaultX509Identifier(domainContextProvider.getCurrentDomain());

        Assertions.assertEquals(defaultX509Identifier, blue_gw);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void removeCertificate() {
        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());

        String red_gw = "red_gw";
        boolean removed = multiDomainCryptoService.removeCertificate(domainContextProvider.getCurrentDomain(), red_gw);

        Assertions.assertTrue(removed);
        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(1, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().noneMatch(entry -> entry.getName().equals(red_gw)));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void removeSameCertificate() {
        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());

        String red_gw = "red_gw";
        boolean removed = multiDomainCryptoService.removeCertificate(domainContextProvider.getCurrentDomain(), red_gw);

        Assertions.assertTrue(removed);
        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(1, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().noneMatch(entry -> entry.getName().equals(red_gw)));

        removed = multiDomainCryptoService.removeCertificate(domainContextProvider.getCurrentDomain(), red_gw);

        Assertions.assertFalse(removed);
        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(1, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().noneMatch(entry -> entry.getName().equals(red_gw)));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void isChangedOnDisk() throws KeyStoreException, IOException {
        KeyStore trustStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertTrue(trustStore.containsAlias("blue_gw"));

        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());

        String fileName = "cefsupportgwtruststore.jks";
        Path path = getPath(fileName);
        byte[] content = Files.readAllBytes(path);
        String fileName2 = "gateway_truststore.jks";
        Path currentPath = getPath(fileName2);
        Files.write(currentPath, content, StandardOpenOption.WRITE);

        boolean isChangedOnDisk = multiDomainCryptoService.isTrustStoreChangedOnDisk(domain);
        Assertions.assertTrue(isChangedOnDisk);

        trustStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertFalse(trustStore.containsAlias("ceftestparty4gw"));

        multiDomainCryptoService.resetTrustStore(domain);
        trustStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertTrue(trustStore.containsAlias("ceftestparty4gw"));
    }

    @Test
    public void changedFileAndAddedCertificate() throws IOException {
        setClusterDepluymentProperty();
        String added_cer = "new_cer_gw";

        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());
        Assertions.assertFalse(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals(added_cer)));
        Assertions.assertFalse(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore();

        // change trust file to simulate a cluster propagation
        String newLoc = location.replace("gateway_truststore.jks", "gateway_truststore_3_certs.jks");
        Files.copy(Paths.get(newLoc), Paths.get(location), REPLACE_EXISTING);

        // file changed but the trust in memory not
        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());
        Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore(false);

        // when adding or removing a cert, the store is read from the disk first to have the latest version
        String fileName = "red_gw.cer";
        Path path = getPath(fileName);
        byte[] content = Files.readAllBytes(path);
        X509Certificate x509Certificate = certificateService.loadCertificate(Base64.getEncoder().encodeToString(content));
        multiDomainCryptoService.addCertificate(domainContextProvider.getCurrentDomain(), Arrays.asList(new CertificateEntry(added_cer, x509Certificate)), true);

        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(4, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(added_cer)));
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore();
    }

    @Test
    public void changedFileAndAddedSameCertificate() throws IOException {
        setClusterDepluymentProperty();
        domibusPropertyProvider.setProperty(CLUSTER_DEPLOYMENT, "true");
        String added_cer = "green_gw";

        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());
        Assertions.assertFalse(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals(added_cer)));
        Assertions.assertFalse(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore();

        // change trust file to simulate a cluster propagation
        String newLoc = location.replace("gateway_truststore.jks", "gateway_truststore_3_certs.jks");
        Files.copy(Paths.get(newLoc), Paths.get(location), REPLACE_EXISTING);

        // file changed but the trust in memory not
        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());
        Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore(false);

        // when adding or removing a cert, the store is read from the disk first to have the latest version
        String fileName = "red_gw.cer";
        Path path = getPath(fileName);
        byte[] content = Files.readAllBytes(path);
        X509Certificate x509Certificate = certificateService.loadCertificate(Base64.getEncoder().encodeToString(content));
        boolean added = multiDomainCryptoService.addCertificate(domainContextProvider.getCurrentDomain(), x509Certificate, added_cer, true);
        Assertions.assertFalse(added);
        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(3, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore();
    }

    @Test
    public void changedFileAndAddedAndRemovedCertificate() throws IOException {
        setClusterDepluymentProperty();

        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        String removed_cer = "red_gw";

        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());
        Assertions.assertTrue(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals("blue_gw")));
        Assertions.assertTrue(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals(removed_cer)));
        checkDiskFileinSyncWithMemoryStore();

        // change trust file to simulate a cluster propagation; new trust has the green_gw cert also
        String newLoc = location.replace("gateway_truststore.jks", "gateway_truststore_3_certs.jks");
        Files.copy(Paths.get(newLoc), Paths.get(location), REPLACE_EXISTING);

        // file changed but the trust in memory not
        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());
        Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals("green_gw")));
        checkDiskFileinSyncWithMemoryStore(false);

        // when adding or removing a cert, the store is read from the disk first to have the latest version
        multiDomainCryptoService.removeCertificate(domainContextProvider.getCurrentDomain(), removed_cer);

        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals("blue_gw")));
        Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(removed_cer)));
        checkDiskFileinSyncWithMemoryStore();
    }

    @Test
    public void multipleChangesToTrustStore() {
        setClusterDepluymentProperty();
        String added_cer = "new_cer_gw";

        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());
        Assertions.assertFalse(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals(added_cer)));
        checkDiskFileinSyncWithMemoryStore();

        List<String> addedCertNames = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            addedCertNames.add("new_cert" + i);
        }
        List<Runnable> tasks = new ArrayList<>();
        for (String name : addedCertNames) {
            tasks.add(() -> addCertificate(name));
        }
        runThreads(tasks);

        List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(12, trustStoreEntries.size());
        for (String name : addedCertNames) {
            Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(name)));
        }
        checkDiskFileinSyncWithMemoryStore();

        tasks = new ArrayList<>();
        for (String name : addedCertNames) {
            tasks.add(() -> removeCertificate(name));
        }
        runThreads(tasks);

        trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, trustStoreEntries.size());
        for (String name : addedCertNames) {
            Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(name)));
        }
        checkDiskFileinSyncWithMemoryStore();
    }

    @Test
    public void tryAddingInvalidCertificate() {
        setClusterDepluymentProperty();
        String alias = "new_cer_gw";

        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        Assertions.assertEquals(2, initialStoreEntries.size());
        Assertions.assertFalse(initialStoreEntries.stream().anyMatch(entry -> entry.getName().equals(alias)));
        checkDiskFileinSyncWithMemoryStore();
        try {
            addCertificate(alias, "invalid_cert.cer");
        } catch (CryptoSpiException ex) {
            List<TrustStoreEntry> trustStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
            Assertions.assertEquals(2, trustStoreEntries.size());
            Assertions.assertFalse(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(alias)));
            checkDiskFileinSyncWithMemoryStore();
            return;
        }
        Assertions.fail();
    }

    private void setClusterDepluymentProperty() {
        domainContextProvider.clearCurrentDomain();
        domibusPropertyProvider.setProperty(CLUSTER_DEPLOYMENT, "true");
    }

    private void removeCertificate(String name) {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        boolean removed = multiDomainCryptoService.removeCertificate(domainContextProvider.getCurrentDomain(), name);
        Assertions.assertTrue(removed);
    }

    private void addCertificate(String alias) {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        String certFileName = "red_gw.cer";
        addCertificate(alias, certFileName);
    }

    private void addCertificate(String alias, String certFileName) {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        try {
            Path path = getPath(certFileName);
            byte[] content = Files.readAllBytes(path);
            X509Certificate x509Certificate = certificateService.loadCertificate(Base64.getEncoder().encodeToString(content));
            boolean added = multiDomainCryptoService.addCertificate(domainContextProvider.getCurrentDomain(), x509Certificate, alias, true);
            Assertions.assertTrue(added);
            checkDiskFileinSyncWithMemoryStore();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void resetInitialTruststore() {
        try {
            String storePassword = "test123";

            String fileName = "gateway_truststore_original.jks";
            Path path = getPath(fileName);
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, "gateway_truststore.jks", content, storePassword);
            multiDomainCryptoService.replaceTrustStore(domain, storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }

    private Date getDate(LocalDateTime localDateTime1) {
        return Date.from(localDateTime1.atZone(ZoneOffset.UTC).toInstant());
    }

    private void runThreads(List<Runnable> tasks) {
        List<Thread> threads = new ArrayList<>();

        for (Runnable task : tasks) {
            Thread t = new Thread(task);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                LOG.error("Thread join stopped", e);
            }
        }

    }

    private void doCheckDiskFileinSyncWithMemoryStore(boolean inSync) {
        KeyStoreContentInfo info = getKeystorePersistenceService.loadStore(getKeystorePersistenceService.getTrustStorePersistenceInfo());
        KeyStore storeOnDisk = certificateService.loadStore(info);
        KeyStore memoryStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertEquals(securityUtil.areKeystoresIdentical(memoryStore, storeOnDisk), inSync);
    }

    private void checkDiskFileinSyncWithMemoryStore() {
        doCheckDiskFileinSyncWithMemoryStore(true);
    }

    private void checkDiskFileinSyncWithMemoryStore(boolean inSync) {
        doCheckDiskFileinSyncWithMemoryStore(inSync);
    }
}
