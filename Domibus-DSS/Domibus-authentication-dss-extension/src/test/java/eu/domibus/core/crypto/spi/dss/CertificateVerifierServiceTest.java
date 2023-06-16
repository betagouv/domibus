package eu.domibus.core.crypto.spi.dss;

import eu.europa.esig.dss.validation.CertificateVerifier;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Thomas Dussart
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class CertificateVerifierServiceTest {

    @Injectable
    private DssCache dssCache;

    @Injectable
    private ObjectProvider<CertificateVerifier> certificateVerifierObjectProvider;

    @Tested
    private CertificateVerifierService certificateVerifierService;

    @Test
    public void getCertificateVerifierWithRefresh() {
        certificateVerifierService.getCertificateVerifier();
        new Verifications() {{
            certificateVerifierObjectProvider.getObject();
            times = 1;
        }};
    }

    @Test
    public void clearCertificateVerifier() {
        certificateVerifierService.clearCertificateVerifier();
        new Verifications() {{
            dssCache.clear();
        }};
    }
}
