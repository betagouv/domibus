package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.api.util.MultiPartFileUtil;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.TLSCertificateManagerImpl;
import eu.domibus.ext.rest.util.RestUtil;
import eu.domibus.ext.rest.util.RestUtilBase;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Enumeration;
import java.util.List;

import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Soumya
 * @author Ion Perpegel
 * @since 5.0
 */
public class TLSTruststoreResourceIT extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TLSTruststoreResourceIT.class);

    public static final String TEST_ENDPOINT_RESOURCE = "/rest/tlstruststore";
    public static final String TEST_ENDPOINT_ADD_WITH_SECURITY_PROFILES = TEST_ENDPOINT_RESOURCE + "/certificates";
    public static final String TEST_ENDPOINT_DELETE_WITH_SECURITY_PROFILES = TEST_ENDPOINT_RESOURCE + "/certificates/{partyName:.+}";

    @Autowired
    private TLSTruststoreResource tlsTruststoreResource;

    @Autowired
    protected MultiPartFileUtil multiPartFileUtil;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    CertificateHelper certificateHelper;

    @Autowired
    TLSCertificateManagerImpl tlsCertificateManager;

    private MockMvc mockMvc;

    protected RestUtilBase restUtil;

    @BeforeEach
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(tlsTruststoreResource).build();
        resetInitalTruststore();
        restUtil = new RestUtil(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD, mockMvc);
    }

    @Test
    public void testTruststoreEntries_ok() throws IOException {
        List<TrustStoreRO> trustStoreROS = tlsTruststoreResource.getTLSTruststoreEntries();

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
    public void replaceTrust_EmptyPass() {
        byte[] content = {1, 0, 1};
        String filename = "file";
        MockMultipartFile truststoreFile = new MockMultipartFile("file", filename, "octetstream", content);
        try {
            tlsTruststoreResource.uploadTLSTruststoreFile(truststoreFile, "", true);
            Assertions.fail();
        } catch (RequestValidationException ex) {
            Assertions.assertEquals("[DOM_001]:Failed to upload the truststoreFile file since its password was empty.", ex.getMessage());
        }
    }

    @Test
    public void replaceTrust_NotValid() {
        byte[] content = {1, 0, 1};
        String filename = "file.jks";
        MockMultipartFile truststoreFile = new MockMultipartFile("file", filename, "octetstream", content);
        try {
            tlsTruststoreResource.uploadTLSTruststoreFile(truststoreFile, "test123", true);
            Assertions.fail();
        } catch (CryptoException ex) {
            Assertions.assertTrue(ex.getMessage().contains("[DOM_001]:Error while replacing the store [TLS.truststore] with content of the file named [file.jks]."));
        }
    }

    @Test
    public void replaceExisting() throws IOException {
        List<TrustStoreRO> entries = tlsTruststoreResource.getTLSTruststoreEntries();

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/gateway_truststore2.jks")) {
            MultipartFile multiPartFile = new MockMultipartFile("gateway_truststore2.jks", "gateway_truststore2.jks",
                    "octetstream", IOUtils.toByteArray(resourceAsStream));
            tlsTruststoreResource.uploadTLSTruststoreFile(multiPartFile, "test123", true);

            List<TrustStoreRO> newEntries = tlsTruststoreResource.getTLSTruststoreEntries();

            Assertions.assertNotEquals(entries.size(), newEntries.size());
        }
    }

    @Test
    public void setAnew() throws IOException {
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/gateway_truststore.jks")) {
            MultipartFile multiPartFile = new MockMultipartFile("gateway_truststore.jks", "gateway_truststore.jks",
                    "octetstream", IOUtils.toByteArray(resourceAsStream));
            try {
                tlsTruststoreResource.uploadTLSTruststoreFile(multiPartFile, "test123", true);
                Assertions.fail();
            } catch (CryptoException ex) {
                Assertions.assertTrue(ex.getMessage().contains("[DOM_001]:Current store [TLS.truststore] was not replaced with the content of the file [gateway_truststore.jks] because they are identical."));
            }
        }
    }

    @Test()
    public void downloadTrust() {
        ResponseEntity<ByteArrayResource> res = tlsTruststoreResource.downloadTLSTrustStore();
        Assertions.assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void uploadTrustStore() throws Exception {
        uploadTrustStore("keystores/default.jks", "default.jks");

        MvcResult result;
        result = uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks");
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("TLS truststore file has been successfully replaced.", content);
    }

    private static String getAliases(KeyStore keystore) throws KeyStoreException {
        StringBuilder stringBuilder = new StringBuilder();
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            stringBuilder.append(aliases.nextElement()).append(",");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    private MvcResult uploadTrustStore(String name, String originalFilename) throws Exception {
        MvcResult result;
        KeyStore keystore;
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            keystore = KeyStore.getInstance("jks");
            keystore.load(resourceAsStream, "test123".toCharArray());
        }
        LOG.info("upload truststore with aliases [{}]", getAliases(keystore));

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            MockMultipartFile multiPartFile = getMultiPartFile(originalFilename, resourceAsStream);

            result = mockMvc.perform(multipart(TEST_ENDPOINT_RESOURCE)
                            .file(multiPartFile)
                            .param("password", "test123")
                            .param("allowChangingDiskStoreProps", "false")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
        return result;
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void downloadTrustStore() throws Exception {
        uploadTrustStore("keystores/default.jks", "default.jks");

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_RESOURCE)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
    }

    @Test
    void addTLSCertificateWithSecurityProfiles() {
        Assertions.assertThrows(DomibusCertificateException.class, () -> addTLSCertificate(true));
    }

    @Test
    void addTLSCertificateWithoutSecurityProfiles() {
        Assertions.assertThrows(DomibusCertificateException.class, () -> addTLSCertificate(false));
    }

    protected void addTLSCertificate(boolean areSecurityProfilesUsed) {
        byte[] content = {1, 0, 1};
        String filename = "file";
        MockMultipartFile truststoreFile = new MockMultipartFile("file", filename, "octetstream", content);
        if (areSecurityProfilesUsed) {
            tlsTruststoreResource.addTLSCertificate(truststoreFile, "red_gw", SecurityProfile.RSA, CertificatePurpose.ENCRYPT);
        } else {
            tlsTruststoreResource.addTLSCertificate(truststoreFile, "tlscert");
        }
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void addWithSecurityProfiles() throws Exception {
        //given
        uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks");

        //when
        MvcResult result = restUtil.addCertificateToStore(TEST_ENDPOINT_ADD_WITH_SECURITY_PROFILES);

        //then
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [red_gw_rsa_decrypt] has been successfully added to the [TLS.truststore].", content);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void deleteWithSecurityProfiles() throws Exception {
        //given
        uploadTrustStore("keystores/default.jks", "default.jks");
        MvcResult result = restUtil.addCertificateToStore(TEST_ENDPOINT_ADD_WITH_SECURITY_PROFILES);
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [red_gw_rsa_decrypt] has been successfully added to the [TLS.truststore].", content);

        //when
        result = restUtil.deleteCertificateFromStore(TEST_ENDPOINT_DELETE_WITH_SECURITY_PROFILES);

        //then
        content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [red_gw_rsa_decrypt] has been successfully removed from the [TLS.truststore].", content);
    }

    @Test
    void removeTLSCertificateWithSecurityProfiles() {
        Assertions.assertThrows(DomibusCertificateException.class, () -> removeTLSCertificate(true));
    }

    @Test
    void removeTLSCertificateWithoutSecurityProfiles() {
        Assertions.assertThrows(DomibusCertificateException.class, () -> removeTLSCertificate(false));
    }

    public void removeTLSCertificate(boolean areSecurityProfilesUsed) {
        if (areSecurityProfilesUsed) {
            tlsTruststoreResource.removeTLSCertificate("tlscert", SecurityProfile.RSA, CertificatePurpose.ENCRYPT);
        } else {
            tlsTruststoreResource.removeTLSCertificate("tlscert");
        }
    }

    private void resetInitalTruststore() {
        try {
            String storePassword = "test123";
            Domain domain = DomainService.DEFAULT_DOMAIN;
            Path path = Paths.get(domibusConfigurationService.getConfigLocation(), KEYSTORES, "gateway_truststore_original.jks");
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, "gateway_truststore.jks", content, storePassword);
            tlsCertificateManager.replaceTrustStore(storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }
}
