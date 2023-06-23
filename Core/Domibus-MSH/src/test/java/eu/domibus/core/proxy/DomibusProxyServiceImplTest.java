package eu.domibus.core.proxy;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author idragusa
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class DomibusProxyServiceImplTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusProxyServiceImplTest.class);

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Tested
    DomibusProxyServiceImpl domibusProxyService;

    @Test
    void initDomibusProxyMissingHostTest() {
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

        }};
        Assertions.assertThrows(DomibusCoreException.class, () -> domibusProxyService.initDomibusProxy());
    }

    @Test
    void initDomibusProxyMissingPortTest() {
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

        }};
        Assertions.assertThrows(DomibusCoreException.class, () -> domibusProxyService.initDomibusProxy());
    }

    @Test
    public void initDomibusProxyTest() {
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROXY_HTTP_PORT);
            result = 8012;

        }};
        domibusProxyService.initDomibusProxy();
    }

    @Test
    void initDomibusProxyMissingPasswordTest() {
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROXY_HTTP_PORT);
            result = 8012;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_USER);
            result = "idragusa";

        }};
        Assertions.assertThrows(DomibusCoreException. class,() -> domibusProxyService.initDomibusProxy());
    }

    @Test
    public void initDomibusProxyAuthTest() {
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROXY_ENABLED);
            result = true;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_HTTP_HOST);
            result = "12.13.14.15";

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROXY_HTTP_PORT);
            result = 8012;

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_USER);
            result = "idragusa";

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_PASSWORD);
            result = "pass";

            domibusPropertyProvider.getProperty(DOMIBUS_PROXY_NON_PROXY_HOSTS);
            result = "localhost";


        }};
        domibusProxyService.initDomibusProxy();
        Assertions.assertTrue(domibusProxyService.getDomibusProxy().isEnabled());
        Assertions.assertEquals("12.13.14.15", domibusProxyService.getDomibusProxy().getHttpProxyHost());
        Assertions.assertEquals(new Integer("8012"), domibusProxyService.getDomibusProxy().getHttpProxyPort());
        Assertions.assertEquals("idragusa", domibusProxyService.getDomibusProxy().getHttpProxyUser());
        Assertions.assertEquals("pass", domibusProxyService.getDomibusProxy().getHttpProxyPassword());
        Assertions.assertEquals("localhost", domibusProxyService.getDomibusProxy().getNonProxyHosts());
    }
}


