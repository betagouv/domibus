package eu.domibus.core.crypto;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.pki.KeystorePersistenceService;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.crypto.api.DomainCryptoService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class MultiDomainCryptoServiceImplTest {

    @Tested
    private MultiDomainCryptoServiceImpl mdCryptoService;

    @Injectable
    private DomainCryptoServiceFactory domainCryptoServiceFactory;

    @Injectable
    private DomibusLocalCacheService domibusLocalCacheService;

    @Injectable
    private CertificateService certificateService;

    @Injectable
    private CertificateHelper certificateHelper;

    @Injectable
    private DomainService domainService;

    @Injectable
    private KeystorePersistenceService keystorePersistenceService;

    @Injectable
    private ObjectProvider<DomibusCryptoType> domibusCryptoTypes;

    @Test
    public void getX509Certificates(@Mocked DomainCryptoServiceImpl cryptoService) throws WSSecurityException {
        Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        DomainCryptoService res = mdCryptoService.getDomainCertificateProvider(domain);
        assertEquals(cryptoService, res);

        DomainCryptoService res2 = mdCryptoService.getDomainCertificateProvider(domain);
        assertEquals(cryptoService, res2);

        new Verifications() {
        };
    }

    @Test
    public void getPrivateKeyPassword(@Mocked DomainCryptoServiceImpl cryptoService) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String privateKeyAlias = "blue_gw";

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.getPrivateKeyPassword(domain, privateKeyAlias);

        new Verifications() {{
            cryptoService.getPrivateKeyPassword(privateKeyAlias);
        }};
    }

    @Test
    public void refreshTrustStore(@Mocked DomainCryptoServiceImpl cryptoService) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.resetTrustStore(domain);

        new Verifications() {{
            cryptoService.resetTrustStore();
        }};
    }

    @Test
    public void replaceTrustStore(@Mocked DomainCryptoServiceImpl cryptoService, @Injectable KeyStoreContentInfo storeInfo) {
        Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.replaceTrustStore(domain, storeInfo);

        new Verifications() {{
            cryptoService.replaceTrustStore(storeInfo);
        }};
    }

    @Test
    public void getKeyStore(@Mocked DomainCryptoServiceImpl cryptoService) {
        Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.getKeyStore(domain);

        new Verifications() {{
            cryptoService.getKeyStore();
        }};
    }

    @Test
    public void getTrustStore(@Mocked DomainCryptoServiceImpl cryptoService) {
        Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.getTrustStore(domain);

        new Verifications() {{
            cryptoService.getTrustStore();
        }};
    }

    @Test
    public void isCertificateChainValid(@Mocked DomainCryptoServiceImpl cryptoService) {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String alias = "blue_gw";

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.isCertificateChainValid(domain, alias);

        new Verifications() {{
            cryptoService.isCertificateChainValid(alias);
        }};
    }

    @Test
    public void getCertificateFromKeystore(@Mocked DomainCryptoServiceImpl cryptoService) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String alias = "blue_gw";

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.getCertificateFromKeystore(domain, alias);

        new Verifications() {{
            cryptoService.getCertificateFromKeyStore(alias);
        }};
    }

    @Test
    public void addCertificate(@Mocked DomainCryptoServiceImpl cryptoService, @Mocked X509Certificate certificate) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String alias = "blue_gw";
        boolean overwrite = true;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.addCertificate(domain, certificate, alias, overwrite);

        new Verifications() {{
            cryptoService.addCertificate(certificate, alias, overwrite);
        }};
    }

    @Test
    public void addCertificates(@Mocked DomainCryptoServiceImpl cryptoService) {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        boolean overwrite = true;

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};
        List<CertificateEntry> certificates = new ArrayList<>();
        mdCryptoService.addCertificate(domain, certificates, overwrite);

        new Verifications() {{
            cryptoService.addCertificate(certificates, overwrite);
        }};
    }

    @Test
    public void getCertificateFromTruststore(@Mocked DomainCryptoServiceImpl cryptoService) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String alias = "blue_gw";

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.getCertificateFromTruststore(domain, alias);

        new Verifications() {{
            cryptoService.getCertificateFromTrustStore(alias);
        }};
    }

    @Test
    public void removeCertificate(@Mocked DomainCryptoServiceImpl cryptoService) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        String alias = "blue_gw";

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.removeCertificate(domain, alias);

        new Verifications() {{
            cryptoService.removeCertificate(alias);
        }};
    }

    @Test
    public void removeCertificates(@Mocked DomainCryptoServiceImpl cryptoService) throws KeyStoreException {
        Domain domain = DomainService.DEFAULT_DOMAIN;
        List<String> aliases = Arrays.asList("blue_gw", "red_gw");

        new Expectations() {{
            domainCryptoServiceFactory.domainCryptoService(domain);
            result = cryptoService;
        }};

        mdCryptoService.removeCertificate(domain, aliases);

        new Verifications() {{
            cryptoService.removeCertificate(aliases);
        }};
    }
}
