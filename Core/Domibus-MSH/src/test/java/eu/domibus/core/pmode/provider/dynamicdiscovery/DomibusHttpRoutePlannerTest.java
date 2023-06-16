package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.core.ssl.offload.SslOffloadService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetAddress;
import java.net.URL;

/**
 * @author Sebastian-Ion TINCU
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class DomibusHttpRoutePlannerTest {

    @Mocked
    private HttpRoute route;

    @Injectable
    private HttpHost host;

    @Injectable
    private HttpRequest request;

    @Injectable
    private HttpContext context;

    @Injectable
    private SslOffloadService sslOffloadService;

    @Tested
    private DomibusHttpRoutePlanner domibusHttpRoutePlanner;

    @BeforeEach
    public void stubSuperCallToDetermineRoute() {
        new MockUp<DefaultRoutePlanner>() {
            @Mock public HttpRoute determineRoute(final HttpHost host, final HttpRequest request, final HttpContext context) throws HttpException {
                return route;
            }
        };
    }

    @Test
    public void testDetermineRoute_sslOffloadDisabled() throws Exception {
        // GIVEN
        final String unsecuredTargetUri = "http://ec.europa.eu";
        new Expectations() {{
            host.toURI(); result = unsecuredTargetUri;
            sslOffloadService.isSslOffloadEnabled(new URL(unsecuredTargetUri)); result = Boolean.FALSE;
        }};

        // WHEN
        HttpRoute result = domibusHttpRoutePlanner.determineRoute(host, request, context);

        // THEN
        Assertions.assertSame(route, result, "Should have not replaced the initial route when SSL offloading is disabled");
    }

    @Test
    public void testDetermineRoute_sslOffloadDisabledForMalformedUrl() throws Exception {
        // GIVEN
        new Expectations() {{
            host.toURI(); result = null;
            sslOffloadService.isSslOffloadEnabled(null); result = Boolean.FALSE;
        }};

        // WHEN
        HttpRoute result = domibusHttpRoutePlanner.determineRoute(host, request, context);

        // THEN
        Assertions.assertSame(route, result, "Should have not replaced the initial route when SSL offloading is disabled because the target URI is malformed");
    }


    @Test
    public void testDetermineRoute_sslOffloadEnabled() throws Exception {
        // GIVEN
        final String unsecuredTargetUri = "https://ec.europa.eu";
        new Expectations() {{
            host.toURI(); result = unsecuredTargetUri;
            sslOffloadService.isSslOffloadEnabled(new URL(unsecuredTargetUri)); result = Boolean.TRUE;
        }};

        // WHEN
        HttpRoute result = domibusHttpRoutePlanner.determineRoute(host, request, context);

        // THEN
        Assertions.assertNotSame(route, result, "Should have replaced the initial route when SSL offloading is enabled");
    }

    @Test
    public void testDetermineRoute_sslOffloadEnabled_routeWithoutProxy(@Injectable HttpHost initialTargetHost,
                                                                       @Injectable InetAddress initialLocalAddress) throws Exception {
        // GIVEN
        final String unsecuredTargetUri = "https://ec.europa.eu";
        new Expectations() {{
            host.toURI(); result = unsecuredTargetUri;
            sslOffloadService.isSslOffloadEnabled(new URL(unsecuredTargetUri)); result = Boolean.TRUE;
            route.getProxyHost(); result = null;
            route.getTargetHost(); result = initialTargetHost;
            route.getLocalAddress(); result = initialLocalAddress;
        }};

        // WHEN
        domibusHttpRoutePlanner.determineRoute(host, request, context);

        // THEN
        new Verifications() {{
            new HttpRoute(initialTargetHost, initialLocalAddress, false); times = 1;
        }};
    }

    @Test
    public void testDetermineRoute_sslOffloadEnabled_routeWithProxy(@Injectable HttpHost initialTargetHost,
                                                                    @Injectable InetAddress initialLocalAddress,
                                                                    @Injectable HttpHost initialProxyHost) throws Exception {
        // GIVEN
        final String unsecuredTargetUri = "https://ec.europa.eu";
        new Expectations() {{
            host.toURI(); result = unsecuredTargetUri;
            sslOffloadService.isSslOffloadEnabled(new URL(unsecuredTargetUri)); result = Boolean.TRUE;
            route.getProxyHost(); result = initialProxyHost;
            route.getTargetHost(); result = initialTargetHost;
            route.getLocalAddress(); result = initialLocalAddress;
        }};

        // WHEN
        domibusHttpRoutePlanner.determineRoute(host, request, context);

        new Verifications() {{
            new HttpRoute(initialTargetHost, initialLocalAddress, initialProxyHost, false); times = 1;
        }};
    }
}
