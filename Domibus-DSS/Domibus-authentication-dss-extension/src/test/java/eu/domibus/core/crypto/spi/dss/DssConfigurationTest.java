package eu.domibus.core.crypto.spi.dss;

import eu.domibus.ext.services.*;
import eu.europa.esig.dss.validation.CertificateVerifier;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Enumeration;

@ExtendWith(JMockitExtension.class)
public class DssConfigurationTest {

    @Injectable
    String dssTlsTrustStorePassword = "pwd";

    @Injectable
    private DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    private DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    private CommandExtService commandExtService;

    @Injectable
    private DssRefreshCommand dssRefreshCommand;

    @Injectable
    private ObjectProvider<CustomTrustedLists> otherTrustedListObjectProvider;

    @Injectable
    protected ObjectProvider<CertificateVerifier> certificateVerifierObjectProvider;

    @Injectable
    private ServerInfoExtService serverInfoExtService;

    @Injectable
    private DomainExtService domainExtService;

    @Injectable
    private PasswordEncryptionExtService passwordEncryptionService;

    @Injectable
    private DssExtensionPropertyManager propertyManager;

    @Tested
    private DssConfiguration dssConfiguration;

    @Test
    @Disabled("EDELIVERY-6896")
    public void mergeCustomTlsTrustStoreWithCacert(
            @Mocked KeyStore customTlsTrustStore,
            @Mocked KeyStore cacertTrustStore,
            @Mocked Enumeration enumeration,
            @Mocked Certificate cert) throws KeyStoreException {
        final String cacertAlias = "cacertAlias";
        new Expectations(dssConfiguration) {{
            KeyStore.getInstance("${domibus.dss.ssl.trust.store.type}");
            result = customTlsTrustStore;
            new MockUp<FileInputStream>() {
                @Mock
                void $init(String fileName) {
                }

                @Mock
                int read() {
                    return 123;
                }

                @Mock
                public void close() throws IOException {
                    System.out.println("Closing file input stream");
                }

                ;
            };
            dssConfiguration.loadCacertTrustStore();
            result = cacertTrustStore;
            cacertTrustStore.aliases();
            result = enumeration;

            enumeration.hasMoreElements();
            returns(true, false);
            enumeration.nextElement();
            result = cacertAlias;
            cacertTrustStore.getCertificate(cacertAlias);
            result = cert;
        }};
        dssConfiguration.trustedListTrustStore();
        new Verifications() {{
            customTlsTrustStore.setCertificateEntry(cacertAlias, cert);
            times = 1;
        }};
    }


    @Test
    public void loadCacertTrustStoreFromDefaultLocation(@Mocked KeyStore keyStore) {
        final String cacertPath = "";
        ReflectionTestUtils.setField(dssConfiguration, "cacertPath", cacertPath);
        final String cacertType = "cacertType";
        ReflectionTestUtils.setField(dssConfiguration, cacertType, cacertType);
        final String cacertPassword = "cacertPassword";
        ReflectionTestUtils.setField(dssConfiguration, cacertPassword, cacertPassword);

        new Expectations(dssConfiguration) {{
            dssConfiguration.getJavaHome();
            result = "\\home";
            dssConfiguration.loadKeystore(anyString, cacertType, cacertPassword);times=1;
            result = keyStore;
        }};
        dssConfiguration.loadCacertTrustStore();

    }

    @Test
    public void loadCacertTrustStoreFromCustomLocation(@Mocked KeyStore keyStore) {

        final String cacertPath = "cacertPath";
        ReflectionTestUtils.setField(dssConfiguration, cacertPath, cacertPath);
        final String cacertType = "cacertType";
        ReflectionTestUtils.setField(dssConfiguration, cacertType, cacertType);
        final String cacertPassword = "cacertPassword";
        ReflectionTestUtils.setField(dssConfiguration, cacertPassword, cacertPassword);
        new Expectations(dssConfiguration) {{
            dssConfiguration.loadKeystore(cacertPath, cacertType, cacertPassword);times=1;
            result = keyStore;
        }};
        dssConfiguration.loadCacertTrustStore();
    }

}
