package eu.domibus.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.crypto.SameResourceCryptoException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.TruststoreResource;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_TRUSTSTORE_LOCATION;
import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TruststoreResourceIT extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TruststoreResourceIT.class);

    @Autowired
    private TruststoreResource truststoreResource;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    private MultiDomainCryptoServiceImpl multiDomainCryptoService;

    @Autowired
    CertificateHelper certificateHelper;

    @BeforeEach
    public void before() {
        resetInitialTruststore();
    }

    @Test
    public void testTruststoreEntries_ok() {
        List<TrustStoreRO> trustStoreROS = truststoreResource.trustStoreEntries();
        for (TrustStoreRO trustStoreRO : trustStoreROS) {
            Assertions.assertNotNull(trustStoreRO.getName(), "Certificate name should be populated in TrustStoreRO:");
            Assertions.assertNotNull(trustStoreRO.getSubject(), "Certificate subject should be populated in TrustStoreRO:");
            Assertions.assertNotNull(trustStoreRO.getIssuer(), "Certificate issuer should be populated in TrustStoreRO:");
            Assertions.assertNotNull(trustStoreRO.getValidFrom(), "Certificate validity from should be populated in TrustStoreRO:");
            Assertions.assertNotNull(trustStoreRO.getValidUntil(), "Certificate validity until should be populated in TrustStoreRO:");
            Assertions.assertNotNull(trustStoreRO.getFingerprints(), "Certificate fingerprints should be populated in TrustStoreRO:");
            Assertions.assertNotNull(trustStoreRO.getCertificateExpiryAlertDays(), "Certificate imminent expiry alert days should be populated in TrustStoreRO:");
            Assertions.assertEquals(60, trustStoreRO.getCertificateExpiryAlertDays(), "Certificate imminent expiry alert days should be populated in TrustStoreRO:");
        }
    }

    @Test
    public void replaceStore() throws IOException {
        List<TrustStoreRO> entries = truststoreResource.trustStoreEntries();

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/gateway_truststore2.jks")) {
            MultipartFile multiPartFile = new MockMultipartFile("gateway_truststore2.jks", "gateway_truststore2.jks",
                    "octetstream", IOUtils.toByteArray(resourceAsStream));

            truststoreResource.uploadTruststoreFile(multiPartFile, "test123");

            List<TrustStoreRO> newEntries = truststoreResource.trustStoreEntries();

            Assertions.assertNotEquals(entries.size(), newEntries.size());
        }
    }

    @Test
    public void replaceStoreWithDifferentType() throws IOException {
        String fileName = "gateway_truststore_p12.p12";
        Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, fileName);
        byte[] content = Files.readAllBytes(path);
        MultipartFile multiPartFile = new MockMultipartFile(fileName, fileName, "octetstream", content);

        truststoreResource.uploadTruststoreFile(multiPartFile, "test123");

        List<TrustStoreRO> newEntries = truststoreResource.trustStoreEntries();

        Assertions.assertEquals(1, newEntries.size());
        // add asserts
    }

    @Test
    public void replaceStoreWithTheSame() throws IOException {
        String fileName = "gateway_truststore2.jks";
        Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, fileName);
        byte[] content = Files.readAllBytes(path);
        MultipartFile multiPartFile = new MockMultipartFile(fileName, fileName, "octetstream", content);

        truststoreResource.uploadTruststoreFile(multiPartFile, "test123");

        List<TrustStoreRO> newEntries = truststoreResource.trustStoreEntries();
        Assertions.assertEquals(9, newEntries.size());

        try {
            truststoreResource.uploadTruststoreFile(multiPartFile, "test123");
        } catch (SameResourceCryptoException ex) {
            Assertions.assertTrue(ex.getMessage().contains("[DOM_001]:Current store [domibus.truststore] was not replaced with the content of the file [gateway_truststore2.jks] because they are identical."));
        }
    }

    @Test
    public void isChangedOnDisk() throws IOException {

        boolean changedOnDisk = truststoreResource.isChangedOnDisk();
        Assertions.assertFalse(changedOnDisk);

        String location = domibusPropertyProvider.getProperty(DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        String back = location.replace("gateway_truststore.jks", "gateway_truststore_back.jks");
        String newLoc = location.replace("gateway_truststore.jks", "gateway_truststore2.jks");
        Files.copy(Paths.get(location), Paths.get(back), REPLACE_EXISTING);
        Files.copy(Paths.get(newLoc), Paths.get(location), REPLACE_EXISTING);
        Files.setLastModifiedTime(Paths.get(location), FileTime.from(new Date().toInstant()));

        changedOnDisk = truststoreResource.isChangedOnDisk();
        Assertions.assertTrue(changedOnDisk);

        Files.copy(Paths.get(back), Paths.get(location), REPLACE_EXISTING);
        changedOnDisk = truststoreResource.isChangedOnDisk();
        Assertions.assertFalse(changedOnDisk);

        Files.delete(Paths.get(back));
    }

    @Test
    public void addSameCertificateWithSecurityProfiles() throws IOException {
        addSameCertificate(true);
    }

    @Test
    public void addSameCertificateWithoutSecurityProfiles() throws IOException {
        addSameCertificate(false);
    }

    protected MultipartFile getMultipartFile() throws IOException {
        String certFileName = "green_gw.cer";
        Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, certFileName);
        byte[] content = Files.readAllBytes(path);

        return new MockMultipartFile(certFileName, certFileName, "octetstream", content);
    }

    public void addSameCertificate(boolean areSecurityProfilesUsed) throws IOException {
        List<TrustStoreRO> initialStoreEntries = truststoreResource.trustStoreEntries();
        Assertions.assertEquals(2, initialStoreEntries.size());
        String alias = areSecurityProfilesUsed ? "green_gw_rsa_encrypt" : "green_gw";

        MultipartFile multiPartFile = getMultipartFile();
        if (areSecurityProfilesUsed) {
            truststoreResource.addDomibusCertificate(multiPartFile, "green_gw", SecurityProfile.RSA, CertificatePurpose.ENCRYPT);
        } else {
            truststoreResource.addDomibusCertificate(multiPartFile, alias);
        }

        List<TrustStoreRO> trustStoreEntries = truststoreResource.trustStoreEntries();
        Assertions.assertEquals(3, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().anyMatch(entry -> entry.getName().equals(alias)));

        try {
            if (areSecurityProfilesUsed) {
                truststoreResource.addDomibusCertificate(multiPartFile, "green_gw", SecurityProfile.RSA, CertificatePurpose.ENCRYPT);
            } else {
                truststoreResource.addDomibusCertificate(multiPartFile, alias);
            }
        } catch (DomibusCertificateException ex) {
            Assertions.assertTrue(ex.getMessage().contains("Certificate [" + alias + "] was not added to the [domibus.truststore] most probably because it already contains the same certificate."));
            trustStoreEntries = truststoreResource.trustStoreEntries();
            Assertions.assertEquals(3, trustStoreEntries.size());
        }
    }

    @Test
    public void removeSameCertificateWithSecurityProfiles() throws IOException {
        removeSameCertificate(true);
    }

    @Test
    public void removeSameCertificateWithoutSecurityProfiles() throws IOException {
        removeSameCertificate(false);
    }

    public void removeSameCertificate(boolean areSecurityProfilesUsed) throws IOException {
        List<TrustStoreRO> trustStoreEntries = truststoreResource.trustStoreEntries();
        Assertions.assertEquals(2, trustStoreEntries.size());

        String alias = areSecurityProfilesUsed ? "red_gw_rsa_encrypt" : "red_gw";

        if (areSecurityProfilesUsed) {
            MultipartFile multiPartFile = getMultipartFile();
            truststoreResource.addDomibusCertificate(multiPartFile, "red_gw", SecurityProfile.RSA, CertificatePurpose.ENCRYPT);
        }

        String res = truststoreResource.removeDomibusCertificate(alias);

        Assertions.assertTrue(res.contains("Certificate [" + alias + "] has been successfully removed from the [domibus.truststore]."));
        trustStoreEntries = truststoreResource.trustStoreEntries();
        int expectedCertificatesNumber = areSecurityProfilesUsed ? 2 : 1;
        Assertions.assertEquals(expectedCertificatesNumber, trustStoreEntries.size());
        Assertions.assertTrue(trustStoreEntries.stream().noneMatch(entry -> entry.getName().equals(alias)));

        try {
            truststoreResource.removeDomibusCertificate(alias);
        } catch (DomibusCertificateException ex) {
            Assertions.assertTrue(ex.getMessage().contains("Certificate [" + alias + "] was not removed from the [domibus.truststore] because it does not exist."));
            trustStoreEntries = truststoreResource.trustStoreEntries();
            Assertions.assertEquals(expectedCertificatesNumber, trustStoreEntries.size());
        }
    }

    private void resetInitialTruststore() {
        try {
            String storePassword = "test123";
            Domain domain = DomainService.DEFAULT_DOMAIN;
            Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, "gateway_truststore_original.jks");
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, "gateway_truststore.jks", content, storePassword);
            multiDomainCryptoService.replaceTrustStore(domain, storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }
}
