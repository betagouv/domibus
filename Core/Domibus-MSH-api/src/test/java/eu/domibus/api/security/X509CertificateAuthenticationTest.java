package eu.domibus.api.security;

import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * @author idragusa
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class X509CertificateAuthenticationTest {

    private static final String RESOURCE_PATH = "src/test/resources/testkeystore.jks";
    private static final String ALIAS_CN_AVAILABLE = "cn_available";
    private static final String TEST_KEYSTORE_PASSWORD = "1234";

    @Injectable
    private X509Certificate[] certificates;

    @Tested
    X509CertificateAuthentication authentication;

    @Test
    public void certificateIdTest() {
        String expectedCertId = "CN=DONOTUSE_TEST,O=TEST_ORGANIZATION,C=DE:0000001469789933";

        String certificateId = authentication.getCertificateId();
        System.out.println(certificateId);

        Assertions.assertEquals(expectedCertId, certificateId);

    }

    @BeforeEach
    public void loadCertificateFromJKSFile() {
        String filePath = RESOURCE_PATH;
        String alias = ALIAS_CN_AVAILABLE;
        String password = TEST_KEYSTORE_PASSWORD;
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fileInputStream, password.toCharArray());
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            certificates = new X509Certificate[1];
            certificates[0] = cert;
        } catch (KeyStoreException | java.security.cert.CertificateException | NoSuchAlgorithmException | IOException e) {
            System.out.println("Could not load certificate from file " + filePath + ", alias " + alias + "pass " + password);
        }
    }


}
