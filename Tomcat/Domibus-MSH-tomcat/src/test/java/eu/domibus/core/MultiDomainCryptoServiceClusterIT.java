package eu.domibus.core;

import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.core.crypto.spi.CryptoSpiException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author François Gautier
 * @since 5.2
 */
//@TestPropertySource(properties = {"domibus.deployment.clustered=true"})

@Disabled("EDELIVERY-6896") //// TODO: FGA 2023-07-13 test is passing with domibus.deployment.clustered=false
public class MultiDomainCryptoServiceClusterIT extends MultiDomainCryptoServiceBase {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MultiDomainCryptoServiceClusterIT.class);

    @Test
    public void changedFileAndAddedCertificate() throws IOException {
        
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
    public void changedFileAndAddedAndRemovedCertificate() throws IOException {

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
    public void tryAddingInvalidCertificate() {
        
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
    @Test
    public void multipleChangesToTrustStore() {
        
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
    public void changedFileAndAddedSameCertificate() throws IOException {
        
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
}
