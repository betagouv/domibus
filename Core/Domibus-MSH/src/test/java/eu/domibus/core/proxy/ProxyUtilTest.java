package eu.domibus.core.proxy;

import eu.domibus.api.proxy.DomibusProxy;
import eu.domibus.api.proxy.DomibusProxyService;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author idragusa
 * @since 4.0
 */
public class ProxyUtilTest {

    @Tested
    ProxyUtil proxyUtil;

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
    public void getConfiguredCredentialsProvider() {
        new Expectations() {{
            domibusProxyService.getDomibusProxy();
            result = domibusProxy;

            domibusProxyService.useProxy();
            result = true;

            domibusProxyService.isProxyUserSet();
            result = true;
        }};

        CredentialsProvider credentialsProvider = proxyUtil.getConfiguredCredentialsProvider();
        Assertions.assertEquals("someuser",credentialsProvider.getCredentials(AuthScope.ANY).getUserPrincipal().getName());

        new FullVerifications() {};
    }

    @Test
    public void getConfiguredCredentialsProvider_noProxy() {
        new Expectations() {{
            domibusProxyService.useProxy();
            result = false;
        }};

        CredentialsProvider credentialsProvider = proxyUtil.getConfiguredCredentialsProvider();
        Assertions.assertNull(credentialsProvider);

        new FullVerifications() {};
    }

    @Test
    public void getConfiguredCredentialsProvider_noProxyUserSet() {
        new Expectations() {{
            domibusProxyService.useProxy();
            result = true;

            domibusProxyService.isProxyUserSet();
            result = false;
        }};

        CredentialsProvider credentialsProvider = proxyUtil.getConfiguredCredentialsProvider();
        Assertions.assertNull(credentialsProvider);

        new FullVerifications() {};
    }

    @Test
    public void getConfiguredProxy() {
        new Expectations() {{
            domibusProxyService.getDomibusProxy();
            result = domibusProxy;

            domibusProxyService.useProxy();
            result = true;
        }};

        HttpHost httpHost = proxyUtil.getConfiguredProxy();
        Assertions.assertEquals(8280, httpHost.getPort());
        Assertions.assertEquals("somehost", httpHost.getHostName());

        new FullVerifications() {};
    }

    @Test
    public void getConfiguredProxy_null() {
        new Expectations() {{
            domibusProxyService.useProxy();
            result = false;
        }};

        HttpHost httpHost = proxyUtil.getConfiguredProxy();
        Assertions.assertNull(httpHost);

        new FullVerifications() {};
    }

}

