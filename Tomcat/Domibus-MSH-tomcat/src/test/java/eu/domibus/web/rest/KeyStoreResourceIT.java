package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.test.common.BrokenMockMultipartFile;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_FILE_UPLOAD_MAX_SIZE;
import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;

public class KeyStoreResourceIT extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TruststoreResourceIT.class);

    @Autowired
    private KeystoreResource storeResource;

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
        resetInitialStore();
    }

    @Test
    public void replaceTrustStore() throws IOException {
        List<TrustStoreRO> entries = storeResource.listEntries();

        String fileName = "gateway_keystore2.jks";
        Path path = getPath(fileName);
        byte[] content = Files.readAllBytes(path);
        MultipartFile multiPartFile = new MockMultipartFile(fileName, fileName, "octetstream", content);

        storeResource.uploadKeystoreFile(multiPartFile, "test123", true);

        List<TrustStoreRO> newEntries = storeResource.listEntries();

        Assertions.assertNotEquals(entries.size(), newEntries.size());

    }

    @Test
    public void isChangedObDisk() throws IOException {
        List<TrustStoreRO> trustStore = storeResource.listEntries();
        Assertions.assertEquals(2, trustStore.size());

        boolean isChangedOnDisk = storeResource.isChangedOnDisk();
        Assertions.assertFalse(isChangedOnDisk);

        Path path = getPath("gateway_keystore2.jks");
        byte[] content = Files.readAllBytes(path);
        Path currentPath = getPath("gateway_keystore.jks");
        Files.write(currentPath, content, StandardOpenOption.WRITE);

        isChangedOnDisk = storeResource.isChangedOnDisk();
        Assertions.assertTrue(isChangedOnDisk);

        trustStore = storeResource.listEntries();
        Assertions.assertEquals(2, trustStore.size());

        storeResource.reset();
        trustStore = storeResource.listEntries();
        Assertions.assertEquals(9, trustStore.size());
    }

    private Path getPath(String newStoreName) {
        return Paths.get(domibusConfigurationService.getConfigLocation(), "domains", domainContextProvider.getCurrentDomainSafely().getCode(), KEYSTORES, newStoreName);
    }

    @Test
    public void testEmptyUpload() {
        String fileName = "gateway_keystore2.jks";
        byte[] content = new byte[]{};
        MultipartFile multiPartFile = new MockMultipartFile(fileName, fileName, "octetstream", content);

        try {
            storeResource.uploadKeystoreFile(multiPartFile, "test123", false);
            Assertions.fail("Expected exception was not raised!");
        } catch (RequestValidationException ex) {
            Assertions.assertTrue(ex.getMessage().contains("it was empty"));
        }
    }

    @Test
    public void testUploadWithIOException() {
        byte[] content = new byte[]{1};
        MultipartFile multiPartFile = new BrokenMockMultipartFile("file.jks", content);
        try {
            storeResource.uploadKeystoreFile(multiPartFile, "test123", false);
            Assertions.fail("Expected exception was not raised!");
        } catch (RequestValidationException ex) {
            Assertions.assertTrue(ex.getMessage().contains("could not read the content"));
        }
    }

    @Test
    public void testUploadWithMaxSize() throws IOException {
        Domain defaultDomain = new Domain("default", "default");
        String previousFileUploadMaxSize = domibusPropertyProvider.getProperty(defaultDomain, DOMIBUS_FILE_UPLOAD_MAX_SIZE);

        try {
            domibusPropertyProvider.setProperty(defaultDomain, DOMIBUS_FILE_UPLOAD_MAX_SIZE, "100", false);
            String fileName = "gateway_keystore2.jks";
            Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, fileName);
            byte[] content = Files.readAllBytes(path);
            MultipartFile multiPartFile = new MockMultipartFile(fileName, fileName, "octetstream", content);

            storeResource.uploadKeystoreFile(multiPartFile, "test123", false);
            Assertions.fail("Expected exception was not raised!");
        } catch (RequestValidationException ex) {
            Assertions.assertTrue(ex.getMessage().contains("exceeds the maximum size limit"));
        }
        finally {
            domibusPropertyProvider.setProperty(defaultDomain, DOMIBUS_FILE_UPLOAD_MAX_SIZE, previousFileUploadMaxSize, false);
        }
    }

    private void resetInitialStore() {
        try {
            String storePassword = "test123";
            Domain domain = DomainService.DEFAULT_DOMAIN;
            Path path = getPath("gateway_keystore_original.jks");
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, "gateway_keystore.jks", content, storePassword);
            multiDomainCryptoService.replaceKeyStore(domain, storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }
}
