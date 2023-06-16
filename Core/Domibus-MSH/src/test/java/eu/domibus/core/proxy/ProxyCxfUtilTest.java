package eu.domibus.core.proxy;

import eu.domibus.api.proxy.DomibusProxy;
import eu.domibus.api.proxy.DomibusProxyService;
import mockit.*;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author idragusa
 * @since 4.0
 */
public class ProxyCxfUtilTest {

    @Tested
    ProxyCxfUtil proxyCxfUtil;

    @Injectable
    protected DomibusProxyService domibusProxyService;
    private DomibusProxy domibusProxy;

    @BeforeEach
    public void setUp() {
        domibusProxy = new DomibusProxy();
        domibusProxy.setEnabled(true);
        domibusProxy.setHttpProxyHost("somehost");
        domibusProxy.setHttpProxyPort(8280);
        domibusProxy.setHttpProxyUser("someuser");
        domibusProxy.setHttpProxyPassword("somepassword");
        domibusProxy.setNonProxyHosts("NonProxyHosts");
    }

    @Test
    public void configureProxy(@Mocked HTTPClientPolicy httpClientPolicy,
                               @Mocked HTTPConduit httpConduit) {
        new Expectations() {{
            domibusProxyService.useProxy();
            result = true;

            domibusProxyService.isProxyUserSet();
            result = true;

            domibusProxyService.getDomibusProxy();
            result = domibusProxy;
        }};

        proxyCxfUtil.configureProxy(httpClientPolicy, httpConduit);

        new FullVerifications() {{
            httpClientPolicy.setProxyServer(domibusProxy.getHttpProxyHost());
            httpClientPolicy.setProxyServerPort(domibusProxy.getHttpProxyPort());
            httpClientPolicy.setProxyServerType(org.apache.cxf.transports.http.configuration.ProxyServerType.HTTP);

            httpClientPolicy.setNonProxyHosts(domibusProxy.getNonProxyHosts());

            ProxyAuthorizationPolicy proxyAuthorizationPolicy;
            httpConduit.setProxyAuthorization(proxyAuthorizationPolicy = withCapture());

            Assertions.assertEquals(domibusProxy.getHttpProxyPassword(), proxyAuthorizationPolicy.getPassword());
            Assertions.assertEquals(domibusProxy.getHttpProxyUser(), proxyAuthorizationPolicy.getUserName());
        }};
    }

    @Test
    public void configureProxy_noProxy(
            @Mocked HTTPClientPolicy httpClientPolicy,
            @Mocked HTTPConduit httpConduit) {
        new Expectations() {{
            domibusProxyService.useProxy();
            result = false;
        }};

        proxyCxfUtil.configureProxy(httpClientPolicy, httpConduit);

        new FullVerifications() {};
    }

}

