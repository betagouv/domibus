package eu.domibus.core;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.pki.KeystorePersistenceInfo;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;

/**
 * @author Ion Perpegel
 * @since 5.0
 */

//@TestPropertySource(properties = {"domibus.deployment.clustered=false"})
public class MultiDomainCryptoServiceIT extends MultiDomainCryptoServiceBase {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MultiDomainCryptoServiceIT.class);

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

    @Test
    public void replaceTrustStoreWithDifferentTypeAndPassword() throws IOException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String initialLocation = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        String initialType = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_TYPE);
        String initialPassword = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD);

        String newStoreName = "gateway_truststore_diffPass.p12";
        String newStorePassword = "test1234";

        Path path = getPath(newStoreName);
        byte[] content = Files.readAllBytes(path);
        KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, newStoreName, content, newStorePassword);

        KeyStore initialStore = multiDomainCryptoService.getTrustStore(domain);
        KeyStoreContentInfo initialStoreContent = multiDomainCryptoService.getTrustStoreContent(domain);
        List<TrustStoreEntry> initialStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);

        multiDomainCryptoService.replaceTrustStore(domain, storeInfo);

        String newLocation = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        String newType = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_TYPE);
        String newPassword = domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD);

        // initial properties didn't change
        Assertions.assertEquals(initialLocation, newLocation);
        Assertions.assertEquals(initialType, newType);
        Assertions.assertEquals(initialPassword, newPassword);

        // can still open the store
        KeyStore newStore = multiDomainCryptoService.getTrustStore(domain);
        List<TrustStoreEntry> newStoreEntries = multiDomainCryptoService.getTrustStoreEntries(domain);
        KeyStoreContentInfo newStoreContent = multiDomainCryptoService.getTrustStoreContent(domain);

        Assertions.assertNotEquals(initialStore, newStore);
        Assertions.assertNotEquals(initialStoreContent.getContent(), newStoreContent.getContent());
        Assertions.assertNotEquals(initialStoreEntries.size(), newStoreEntries.size());
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


}
