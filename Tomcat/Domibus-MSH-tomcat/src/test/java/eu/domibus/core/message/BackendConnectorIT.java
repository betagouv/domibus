package eu.domibus.core.message;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.*;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.plugin.BackendConnectorHelper;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.core.plugin.routing.RoutingService;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.backendConnector.TestFSPluginMock;
import eu.domibus.backendConnector.TestWSPluginMock;
import eu.domibus.test.common.BackendConnectorMock;
import eu.domibus.test.common.SoapSampleUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.*;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_CACHE_LOCATION;
import static eu.domibus.backendConnector.TestFSPluginPropertyManager.TEST_FSPLUGIN_DOMAIN_ENABLED;
import static eu.domibus.backendConnector.TestWSPluginPropertyManager.TEST_WSPLUGIN_DOMAIN_ENABLED;
import static eu.domibus.common.NotificationType.DEFAULT_PUSH_NOTIFICATIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    String messageId, filename;

    @Transactional
    @Before
    public void before() throws XmlProcessingException, IOException {
        super.before();
        messageId = BackendConnectorMock.MESSAGE_ID;
        filename = "SOAPMessage2.xml";

        uploadPMode();

        Mockito.when(backendConnectorProvider.getBackendConnector("wsPlugin"))
                .thenReturn(testWSPluginMock);

        Mockito.when(backendConnectorProvider.getBackendConnector("fsPlugin"))
                .thenReturn(testFSPluginMock);

        Mockito.when(backendConnectorHelper.getRequiredNotificationTypeList(Mockito.any(BackendConnector.class)))
                .thenReturn(DEFAULT_PUSH_NOTIFICATIONS);

        routingService.invalidateBackendFiltersCache();
        domibusLocalCacheService.clearCache(DomibusLocalCacheService.DOMIBUS_PROPERTY_CACHE);
    }

    @Transactional
    @After
    public void after() {
        testWSPluginMock.clear();
        List<MessageLogInfo> list = userMessageLogDao.findAllInfoPaged(0, 100, "ID_PK", true, new HashMap<>());
        if (list.size() > 0) {
            list.forEach(el -> {
                UserMessageLog res = userMessageLogDao.findByMessageId(el.getMessageId(), el.getMshRole());
                userMessageLogDao.deleteMessageLogs(Arrays.asList(res.getEntityId()));
            });
        }
    }

    @Test
    @Transactional
    public void testNotifyEnabledPlugin() throws SOAPException, IOException, ParserConfigurationException, SAXException, EbMS3Exception {

        domibusPropertyProvider.setProperty(TEST_WSPLUGIN_DOMAIN_ENABLED, "true");
        domibusPropertyProvider.setProperty(TEST_FSPLUGIN_DOMAIN_ENABLED, "true");

        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        final SOAPMessage soapResponse = mshWebserviceTest.invoke(soapMessage);

        assertEquals(testWSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);

        final Ebms3Messaging ebms3Messaging = messageUtil.getMessagingWithDom(soapResponse);
        assertNotNull(ebms3Messaging);

        assertEquals(testWSPluginMock.getDeliverMessageEvent().getMessageId(), messageId);
    }

    @Test
    @Transactional
    public void testNotifyDisabledPlugin() throws SOAPException, IOException, ParserConfigurationException, SAXException {
        // set like this to void property change listeners to fire and validate that at least one active plugin exists per domain
        System.setProperty("default." + TEST_WSPLUGIN_DOMAIN_ENABLED, "false");
        System.setProperty("default." + TEST_FSPLUGIN_DOMAIN_ENABLED, "false");

        try {
            SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
            final SOAPMessage soapResponse = mshWebserviceTest.invoke(soapMessage);
            Assert.fail();
        } catch (javax.xml.ws.WebServiceException ex) {
            Assert.assertTrue(ex.getMessage().contains("Could not find matching backend filter"));
        }
    }

    @Test
    @Transactional
    public void testTryDisableAllPlugins() {
        System.setProperty("default." + TEST_WSPLUGIN_DOMAIN_ENABLED, "true");
        System.setProperty("default." + TEST_FSPLUGIN_DOMAIN_ENABLED, "true");

        domibusPropertyProvider.setProperty(TEST_WSPLUGIN_DOMAIN_ENABLED, "false");
        try {
            domibusPropertyProvider.setProperty(TEST_FSPLUGIN_DOMAIN_ENABLED, "false");
            Assert.fail();
        } catch (DomibusPropertyException ex) {
            Assert.assertTrue(ex.getCause().getMessage().contains("Cannot disable the plugin [testFSPlugin] on domain [default] because there won't remain any enabled plugins"));
        }
    }

}
