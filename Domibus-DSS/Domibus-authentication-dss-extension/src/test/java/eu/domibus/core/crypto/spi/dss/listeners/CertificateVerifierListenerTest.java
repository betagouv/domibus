package eu.domibus.core.crypto.spi.dss.listeners;

import eu.domibus.core.crypto.spi.dss.CertificateVerifierService;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.core.crypto.spi.dss.DssExtensionPropertyManager.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Dussart
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class CertificateVerifierListenerTest {

    @Test
    public void handlesProperty(@Mocked final CertificateVerifierService certificateVerifierService) {
        CertificateVerifierListener certificateVerifierListener = new CertificateVerifierListener(certificateVerifierService);
        assertTrue(certificateVerifierListener.handlesProperty(DSS_PERFORM_CRL_CHECK));
        assertTrue(certificateVerifierListener.handlesProperty(AUTHENTICATION_DSS_CHECK_REVOCATION_FOR_UNTRUSTED_CHAINS));
        assertTrue(certificateVerifierListener.handlesProperty(AUTHENTICATION_DSS_EXCEPTION_ON_MISSING_REVOCATION_DATA));
        assertFalse(certificateVerifierListener.handlesProperty("any other property"));
    }

    @Test
    public void propertyValueChanged(@Mocked final CertificateVerifierService certificateVerifierService) {
        CertificateVerifierListener certificateVerifierListener = new CertificateVerifierListener(certificateVerifierService);
        certificateVerifierListener.propertyValueChanged(null, null, null);
        new Verifications() {{
            certificateVerifierService.clearCertificateVerifier();
        }};
    }
}
