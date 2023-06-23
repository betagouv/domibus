package eu.domibus.core.crypto.spi.dss.listeners;

import com.google.common.collect.Sets;
import eu.domibus.core.crypto.spi.dss.ProxyHelper;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static eu.domibus.core.crypto.spi.dss.DssExtensionPropertyManager.*;
import static eu.domibus.core.crypto.spi.dss.DssExtensionPropertyManager.AUTHENTICATION_DSS_PROXY_HTTPS_EXCLUDEDHOSTS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Dussart
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class NetworkConfigurationListenerTest {

    @Test
    public void handlesProperty(@Mocked final CommonsDataLoader dataLoader, @Mocked final ProxyHelper proxyHelper) {
        Set<String> properties = Sets.newHashSet(
                AUTHENTICATION_DSS_PROXY_HTTP_HOST,
                AUTHENTICATION_DSS_PROXY_HTTP_PORT,
                AUTHENTICATION_DSS_PROXY_HTTP_USER,
                AUTHENTICATION_DSS_PROXY_HTTP_PASSWORD,
                AUTHENTICATION_DSS_PROXY_HTTP_EXCLUDEDHOSTS,
                AUTHENTICATION_DSS_PROXY_HTTPS_HOST,
                AUTHENTICATION_DSS_PROXY_HTTPS_PORT,
                AUTHENTICATION_DSS_PROXY_HTTPS_USER,
                AUTHENTICATION_DSS_PROXY_HTTPS_PASSWORD,
                AUTHENTICATION_DSS_PROXY_HTTPS_EXCLUDEDHOSTS);
        NetworkConfigurationListener networkConfigurationListener = new NetworkConfigurationListener(dataLoader, proxyHelper);
        for (String property : properties) {
            assertTrue(networkConfigurationListener.handlesProperty(property));
        }
        assertFalse(networkConfigurationListener.handlesProperty("any other property"));
    }

    @Test
    public void propertyValueChanged(@Mocked final CommonsDataLoader dataLoader, @Mocked final ProxyHelper proxyHelper) {
        ProxyConfig proxyConfig=new ProxyConfig();
        new Expectations(){{
            proxyHelper.getProxyConfig();
            result=proxyConfig;
        }};
        NetworkConfigurationListener networkConfigurationListener = new NetworkConfigurationListener(dataLoader, proxyHelper);
        networkConfigurationListener.propertyValueChanged(null,null,null);
        new Verifications(){{
            dataLoader.setProxyConfig(proxyConfig);
        }};

    }
}
