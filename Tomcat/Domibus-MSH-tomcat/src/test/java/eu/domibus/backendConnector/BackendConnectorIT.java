package eu.domibus.backendConnector;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.message.DeleteMessageAbstractIT;
import eu.domibus.core.message.MessageLogInfo;
import eu.domibus.core.plugin.BackendConnectorHelper;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.core.plugin.routing.RoutingService;
import eu.domibus.core.property.DomibusPropertiesPropertySource;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.test.common.SoapSampleUtil;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.*;

import static eu.domibus.backendConnector.TestFSPluginMock.TEST_FS_PLUGIN;
import static eu.domibus.backendConnector.TestFSPluginPropertyManager.TEST_FSPLUGIN_DOMAIN_ENABLED;
import static eu.domibus.backendConnector.TestWSPluginMock.TEST_WS_PLUGIN;
import static eu.domibus.backendConnector.TestWSPluginPropertyManager.TEST_WSPLUGIN_DOMAIN_ENABLED;
import static eu.domibus.common.NotificationType.DEFAULT_PUSH_NOTIFICATIONS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ion perpegel
 * @since 5.2
 */

@Transactional
public class BackendConnectorIT extends DeleteMessageAbstractIT {

    @Autowired
    BackendConnectorProvider backendConnectorProvider;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    MSHWebservice mshWebserviceTest;

    @Autowired
    MessageUtil messageUtil;

    @Autowired
    protected BackendConnectorHelper backendConnectorHelper;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    TestWSPluginMock testWSPluginMock;

    @Autowired
    TestFSPluginMock testFSPluginMock;

    @Autowired
    RoutingService routingService;

    @Autowired
    DomibusLocalCacheService domibusLocalCacheService;

    @Autowired
    ConfigurableEnvironment environment;

    String messageId, filename;

    @BeforeEach
    public void before() throws XmlProcessingException, IOException {
        super.before();

        messageId = getMessageId();
        filename = "SOAPMessage2.xml";

        uploadPMode();

        Mockito.when(backendConnectorProvider.getBackendConnector(TEST_WS_PLUGIN))
                .thenReturn(testWSPluginMock);

        Mockito.when(backendConnectorProvider.getBackendConnector(TEST_FS_PLUGIN))
                .thenReturn(testFSPluginMock);

        Mockito.when(backendConnectorProvider.getEnableAwares())
                .thenReturn(Arrays.asList(testWSPluginMock, testFSPluginMock));

        Mockito.when(backendConnectorHelper.getRequiredNotificationTypeList(Mockito.any(BackendConnector.class)))
                .thenReturn(DEFAULT_PUSH_NOTIFICATIONS);

        routingService.invalidateBackendFiltersCache();
        domibusLocalCacheService.clearCache(DomibusLocalCacheService.DOMIBUS_PROPERTY_CACHE);

        // set like this to void property change listeners to fire
        setValueInDomibusPropertySource("default." + TEST_WSPLUGIN_DOMAIN_ENABLED, "true");
        setValueInDomibusPropertySource("default." + TEST_FSPLUGIN_DOMAIN_ENABLED, "true");

        testWSPluginMock.clear();
        testFSPluginMock.clear();
    }

    private static String getMessageId() {
        return UUID.randomUUID() + "@domibus.eu";
    }

    @AfterEach
    public void after() {
        List<MessageLogInfo> list = userMessageLogDao.findAllInfoPaged(0, 100, "ID_PK", true, new HashMap<>(), new ArrayList<>());
        list.forEach(el -> {
            UserMessageLog res = userMessageLogDao.findByMessageId(el.getMessageId(), el.getMshRole());
            userMessageLogDao.deleteMessageLogs(Arrays.asList(res.getEntityId()));
        });
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testNotifyFirstEnabledPlugin() throws SOAPException, IOException, ParserConfigurationException, SAXException, EbMS3Exception {
        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        final SOAPMessage soapResponse = mshWebserviceTest.invoke(soapMessage);

        assertEquals(testWSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);
        assertNull(testFSPluginMock.getDeliverMessageEvent());

        final Ebms3Messaging ebms3Messaging = messageUtil.getMessagingWithDom(soapResponse);
        assertNotNull(ebms3Messaging);

        assertEquals(testWSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);
    }

    @Test
    @Disabled
    public void testNotifySingleEnabledPlugin() throws SOAPException, IOException, ParserConfigurationException, SAXException, EbMS3Exception {
        // ws plugin not ebabled so the FS will receive the message
        domibusPropertyProvider.setProperty(TEST_WSPLUGIN_DOMAIN_ENABLED, "false");

        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        SOAPMessage soapResponse = mshWebserviceTest.invoke(soapMessage);

        assertEquals(testFSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);
        assertNull(testWSPluginMock.getDeliverMessageEvent());

        final Ebms3Messaging ebms3Messaging = messageUtil.getMessagingWithDom(soapResponse);
        assertNotNull(ebms3Messaging);

        assertEquals(testFSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);

        testWSPluginMock.clear();
        testFSPluginMock.clear();
        domibusPropertyProvider.setProperty(TEST_WSPLUGIN_DOMAIN_ENABLED, "true");
        messageId = getMessageId();
        // now ws plugin is re-enabled, so it should receive the message this time
        soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        soapResponse = mshWebserviceTest.invoke(soapMessage);

        assertEquals(testWSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);
        assertNull(testFSPluginMock.getDeliverMessageEvent());
    }

    @Test
    public void testNotifyDisabledPlugin() throws SOAPException, IOException, ParserConfigurationException, SAXException {
        // set like this to void property change listeners to fire and validate that at least one active plugin exists per domain
        setValueInDomibusPropertySource("default." + TEST_WSPLUGIN_DOMAIN_ENABLED, "false");
        setValueInDomibusPropertySource("default." + TEST_FSPLUGIN_DOMAIN_ENABLED, "false");

        try {
            SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
            mshWebserviceTest.invoke(soapMessage);
            Assertions.fail();
        } catch (javax.xml.ws.WebServiceException ex) {
            Assertions.assertTrue(ex.getMessage().contains("Could not find matching backend filter"));
        }
    }

    @Test
    public void testTryDisableAllPlugins() {
        domibusPropertyProvider.setProperty(TEST_WSPLUGIN_DOMAIN_ENABLED, "false");
        Assertions.assertFalse(domibusPropertyProvider.getBooleanProperty(TEST_WSPLUGIN_DOMAIN_ENABLED));
        try {
            domibusPropertyProvider.setProperty(TEST_FSPLUGIN_DOMAIN_ENABLED, "false");
            Assertions.fail();
        } catch (DomibusPropertyException ex) {
            Assertions.assertTrue(domibusPropertyProvider.getBooleanProperty(TEST_FSPLUGIN_DOMAIN_ENABLED));
            Assertions.assertTrue(ex.getCause().getMessage().contains("Cannot disable the plugin [fsPlugin] on domain [default] because there won't remain any enabled plugins"));
        }
    }

    @Test
    public void testTrySubmitWithDisabledPlugin() {
        domibusPropertyProvider.setProperty(TEST_FSPLUGIN_DOMAIN_ENABLED, "false");
        Assertions.assertFalse(domibusPropertyProvider.getBooleanProperty(TEST_FSPLUGIN_DOMAIN_ENABLED));
        try {
            testFSPluginMock.submit(new Object());
            Assertions.fail();
        } catch (DomibusCoreException ex) {
            Assertions.assertTrue(ex.getMessage().contains("Backend connector [fsPlugin] is not enabled; Cancelling submit"));
        } catch (MessagingProcessingException e) {
            Assertions.fail();
        }
    }

    private void setValueInDomibusPropertySource(String propertyKey, String propertyValue) {
        MutablePropertySources propertySources = environment.getPropertySources();
        DomibusPropertiesPropertySource domibusPropertiesPropertySource = (DomibusPropertiesPropertySource) propertySources.get(DomibusPropertiesPropertySource.NAME);
        domibusPropertiesPropertySource.setProperty(propertyKey, propertyValue);

        DomibusPropertiesPropertySource updatedDomibusPropertiesSource = (DomibusPropertiesPropertySource) propertySources.get(DomibusPropertiesPropertySource.UPDATED_PROPERTIES_NAME);
        updatedDomibusPropertiesSource.setProperty(propertyKey, propertyValue);
    }
}
