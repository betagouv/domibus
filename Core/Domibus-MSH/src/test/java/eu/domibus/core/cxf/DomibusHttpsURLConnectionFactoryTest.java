package eu.domibus.core.cxf;

import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.https.HttpsURLConnectionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_CONNECTION_CXF_SSL_OFFLOAD_ENABLE;

@ExtendWith(JMockitExtension.class)
public class DomibusHttpsURLConnectionFactoryTest {

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private TLSClientParameters tlsClientParameters;

    @Injectable
    private Proxy proxy;

    @Injectable
    private URL url;

    @Injectable
    private HttpURLConnection httpURLConnection;

    @Tested
    private DomibusHttpsURLConnectionFactory domibusHttpsURLConnectionFactory;

    @Test
    public void createConnection() throws Exception {
        new MockUp<HttpsURLConnectionFactory>() {
            @Mock
            public HttpURLConnection createConnection(TLSClientParameters tlsClientParameters,
                                                      Proxy proxy, URL url) throws IOException {
                return httpURLConnection;
            }
        };

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_CONNECTION_CXF_SSL_OFFLOAD_ENABLE); result = Boolean.FALSE;
        }};

        HttpURLConnection connection = domibusHttpsURLConnectionFactory.createConnection(tlsClientParameters, proxy, url);

        new FullVerifications() {{
            url.openConnection(proxy); times = 0;
            Assertions.assertSame(httpURLConnection, connection, "Should have returned the correct HTTP URL connection");
        }};
    }

    @Test
    public void createConnection_sslOffloading() throws Exception {
        new MockUp<HttpsURLConnectionFactory>() {
            @Mock
            public HttpURLConnection createConnection(TLSClientParameters tlsClientParameters,
                                                      Proxy proxy, URL url) throws IOException {
                Assertions.fail("Should have not called the super create connection when SSL offloading");
                return null;
            }
        };

        new Expectations() {{
            url.openConnection(proxy); result = httpURLConnection;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_CONNECTION_CXF_SSL_OFFLOAD_ENABLE); result = Boolean.TRUE;
        }};

        HttpURLConnection connection = domibusHttpsURLConnectionFactory.createConnection(tlsClientParameters, proxy, url);

        Assertions.assertSame(httpURLConnection, connection, "Should have returned the correct HTTP URL connection");
    }
}
