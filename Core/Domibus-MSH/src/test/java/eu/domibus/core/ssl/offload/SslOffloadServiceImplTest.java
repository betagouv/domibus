package eu.domibus.core.ssl.offload;

import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.transport.http.Address;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_CONNECTION_CXF_SSL_OFFLOAD_ENABLE;

/**
 * @author Sebastian-Ion TINCU
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class SslOffloadServiceImplTest {

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Tested
    private SslOffloadServiceImpl sslOffloadService;

    @Test
    public void testIsSslOffloadEnabled_nullUrl() {
        // WHEN
        boolean sslOffloadEnabled = sslOffloadService.isSslOffloadEnabled(null);

        // THEN
        Assertions.assertFalse(sslOffloadEnabled, "Should have returned false if passing null when checking whether the SSL offload is enabled or not");
    }

    @Test
    public void testIsSslOffloadEnabled_httpUrl() throws Exception {
        // GIVEN
        URL unsecureUrl = new URL("http://ec.europa.eu");

        // WHEN
        boolean sslOffloadEnabled = sslOffloadService.isSslOffloadEnabled(unsecureUrl);

        // THEN
        Assertions.assertFalse(sslOffloadEnabled, "Should have returned false if passing an HTTP URL when checking whether the SSL offload is enabled or not");
    }

    @Test
    public void testIsSslOffloadEnabled_sslDomibusPropertyOff() throws Exception {
        // GIVEN
        URL secureUrl = new URL("https://ec.europa.eu");
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_CONNECTION_CXF_SSL_OFFLOAD_ENABLE);
            result = Boolean.FALSE;
        }};

        // WHEN
        boolean sslOffloadEnabled = sslOffloadService.isSslOffloadEnabled(secureUrl);

        // THEN
        Assertions.assertFalse(sslOffloadEnabled, "Should have returned false if the SSL Domibus property is off (even if passing an HTTPS URL) when checking whether the SSL offload is enabled or not");
    }

    @Test
    public void testIsSslOffloadEnabled() throws Exception {
        // GIVEN
        URL secureUrl = new URL("https://ec.europa.eu");
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_CONNECTION_CXF_SSL_OFFLOAD_ENABLE);
            result = Boolean.TRUE;
        }};

        // WHEN
        boolean sslOffloadEnabled = sslOffloadService.isSslOffloadEnabled(secureUrl);

        // THEN
        Assertions.assertTrue(sslOffloadEnabled, "Should have returned true if the SSL Domibus property is on and if passing an HTTPS URL when checking whether the SSL offload is enabled or not");
    }

    @Test
    public void offloadAddress_replacesAddress() throws Exception {
        // GIVEN
        Address secureAddress = new Address("https://ec.europa.eu");

        // WHEN
        Address result = sslOffloadService.offload(secureAddress);

        // THEN
        Assertions.assertNotSame(secureAddress, result, "Should have replaced the address when offloading");
    }

    @Test
    public void offloadAddress_switchesAddressToHttp() throws Exception {
        // GIVEN
        Address secureAddress = new Address("https://ec.europa.eu");

        // WHEN
        Address result = sslOffloadService.offload(secureAddress);

        // THEN
        Assertions.assertEquals("http://ec.europa.eu", result.getString(), "Should have switched the address URL to HTTP when offloading");
    }

    @Test
    public void offloadAddress_revertsProtocolToHttps() throws Exception {
        // GIVEN
        Address secureAddress = new Address("https://ec.europa.eu");

        // WHEN
        Address result = sslOffloadService.offload(secureAddress);

        // THEN
        Assertions.assertEquals("https", result.getURL().getProtocol(), "Should have reverted the address URL protocol back to HTTPS");
    }

}
