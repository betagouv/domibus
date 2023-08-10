package eu.domibus.core.crypto.spi.dss;

import eu.domibus.ext.services.*;
import eu.europa.esig.dss.validation.CertificateVerifier;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

@ExtendWith(JMockitExtension.class)
class DssConfigurationTest {

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
    void mergeCustomTlsTrustStoreWithCacert(
            @Injectable KeyStore customTlsTrustStore,
            @Injectable KeyStore cacertTrustStore,
            @Injectable Certificate cert) throws KeyStoreException {
        final String cacertAlias = "cacertAlias";

        String gateway_truststore = getClass().getClassLoader().getResource("gateway_truststore.jks").getPath();
        ReflectionTestUtils.setField(dssConfiguration, "dssTlsTrustStorePath", gateway_truststore);
        new Expectations(dssConfiguration) {{
            new MockUp<KeyStore>() {
                @Mock
                public KeyStore getInstance(String type) {
                    return customTlsTrustStore;
                }
            };
            dssConfiguration.loadCacertTrustStore();
            result = cacertTrustStore;
            cacertTrustStore.aliases();
            result = getStringEnumeration(cacertAlias);
            cacertTrustStore.getCertificate(cacertAlias);
            result = cert;
        }};
        dssConfiguration.trustedListTrustStore();
        new Verifications() {{
            customTlsTrustStore.setCertificateEntry(cacertAlias, cert);
            times = 1;
        }};
    }

    private static Enumeration<String> getStringEnumeration(String cacertAlias) {
        Enumeration<String> enumeration = new Enumeration<String>() {
            final Iterator<String> a = Arrays.asList(cacertAlias).iterator();

            @Override
            public boolean hasMoreElements() {
                return a.hasNext();
            }

            @Override
            public String nextElement() {
                return a.next();
            }
        };
        return enumeration;
    }


    @Test
    void loadCacertTrustStoreFromDefaultLocation(@Injectable KeyStore keyStore) {
        final String cacertPath = "";
        ReflectionTestUtils.setField(dssConfiguration, "cacertPath", cacertPath);
        final String cacertType = "cacertType";
        ReflectionTestUtils.setField(dssConfiguration, cacertType, cacertType);
        final String cacertPassword = "cacertPassword";
        ReflectionTestUtils.setField(dssConfiguration, cacertPassword, cacertPassword);

        new Expectations(dssConfiguration) {{
            dssConfiguration.getJavaHome();
            result = "\\home";
            dssConfiguration.loadKeystore(anyString, cacertType, cacertPassword);
            times=1;
            result = keyStore;
        }};
        dssConfiguration.loadCacertTrustStore();

    }

    @Test
    void loadCacertTrustStoreFromCustomLocation(@Injectable KeyStore keyStore) {

        final String cacertPath = "cacertPath";
        ReflectionTestUtils.setField(dssConfiguration, cacertPath, cacertPath);
        final String cacertType = "cacertType";
        ReflectionTestUtils.setField(dssConfiguration, cacertType, cacertType);
        final String cacertPassword = "cacertPassword";
        ReflectionTestUtils.setField(dssConfiguration, cacertPassword, cacertPassword);
        new Expectations(dssConfiguration) {{
            dssConfiguration.loadKeystore(cacertPath, cacertType, cacertPassword);
            times=1;
            result = keyStore;
        }};
        dssConfiguration.loadCacertTrustStore();
    }

}
