package eu.domibus.core.security;

import eu.domibus.api.security.AuthenticationException;
import eu.domibus.core.certificate.CertificateServiceImpl;
import eu.domibus.core.certificate.crl.CRLService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.cert.X509Certificate;

import static eu.domibus.core.certificate.CertificateTestUtils.loadCertificateFromJKSFile;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author idragusa
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class X509CertificateServiceImplTest {

    @Injectable
    private CertificateServiceImpl certificateService;

    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/ebms3/common/dao/DynamicDiscoveryPModeProviderTest/";
    private static final String TEST_KEYSTORE = "testkeystore.jks";
    private static final String ALIAS_CN_AVAILABLE = "cn_available";
    private static final String TEST_KEYSTORE_PASSWORD = "1234";

    private static final String EXPIRED_KEYSTORE = "expired_gateway_keystore.jks";
    private static final String EXPIRED_ALIAS = "blue_gw";
    private static final String EXPIRED_KEYSTORE_PASSWORD = "test123";


    @Tested
    X509CertificateServiceImpl securityX509CertificateServiceImpl;

    @Injectable
    CRLService crlService;

    @Test
    public void verifyCertificateTest() {
        X509Certificate[] certificates = createCertificates(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        new Expectations(){{
            crlService.isCertificateRevoked(certificates[0]);
            result = false;
            times = 1;
        }};

        securityX509CertificateServiceImpl.validateClientX509Certificates(certificates);

        new FullVerifications() {};
    }

    @Test
    void verifyCertificateRevokedTest() {
        X509Certificate[] certificates = createCertificates(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        new Expectations(){{
            crlService.isCertificateRevoked(certificates[0]);
            result = true;
            times = 1;
        }};

        Assertions.assertThrows(AuthenticationException. class,
        () -> securityX509CertificateServiceImpl.validateClientX509Certificates(certificates));

        new FullVerifications() {};
    }

    @Test
    void verifyCertificateExpiredTest() {
        X509Certificate[] certificates = createCertificates(RESOURCE_PATH + EXPIRED_KEYSTORE, EXPIRED_ALIAS, EXPIRED_KEYSTORE_PASSWORD);
        Assertions.assertThrows(AuthenticationException. class,
        () -> securityX509CertificateServiceImpl.validateClientX509Certificates(certificates));

        new Verifications() {{
            crlService.isCertificateRevoked(certificates[0]);
            times = 0;
        }};
    }

    private X509Certificate[] createCertificates(String keystore_path, String alias, String password) {
        X509Certificate certificate = loadCertificateFromJKSFile(keystore_path, alias, password);
        assertNotNull(certificate);
        X509Certificate[] certificates = new X509Certificate[1];
        certificates[0] = certificate;
        return certificates;
    }

}
