package eu.domibus.plugin.ws.webservice.deprecated;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.message.MessagingService;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.webService.generated.LargePayloadType;
import eu.domibus.plugin.webService.generated.RetrieveMessageFault;
import eu.domibus.plugin.webService.generated.RetrieveMessageRequest;
import eu.domibus.plugin.webService.generated.RetrieveMessageResponse;
import eu.domibus.plugin.ws.AbstractBackendWSIT;
import eu.domibus.test.DomibusConditionUtil;
import eu.domibus.test.PModeUtil;
import eu.domibus.test.common.UserMessageSampleUtil;
import eu.domibus.test.common.SoapSampleUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import eu.domibus.plugin.ws.webservice.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Holder;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @deprecated to be removed when deprecated endpoint /backend is removed
 */
@Deprecated
@Disabled("[EDELIVERY-8828] WSPLUGIN: tests for rest methods ignored")
public class RetrieveMessageIT extends AbstractBackendWSIT {

    @Autowired
    JMSManager jmsManager;

    @Autowired
    MessagingService messagingService;

    @Autowired
    protected MessageRetriever messageRetriever;

    @Autowired
    UserMessageLogDefaultService userMessageLogService;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    PModeUtil pModeUtil;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    UserMessageSampleUtil userMessageSampleUtil;

    @Autowired
    MSHWebservice mshWebserviceTest;

    @Autowired
    DomibusConditionUtil domibusConditionUtil;

    @Autowired
    MessageRetentionDefaultService messageRetentionService;

    @BeforeEach
    public void updatePMode(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException, SOAPException, ParserConfigurationException, SAXException {
        pModeUtil.uploadPmode(wmRuntimeInfo.getHttpPort());
    }

    private void receiveMessage(String messageId) throws SOAPException, IOException, ParserConfigurationException, SAXException {
        String filename = "SOAPMessage2.xml";
        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        final SOAPMessage soapResponse = mshWebserviceTest.invoke(soapMessage);

        domibusConditionUtil.waitUntilMessageIsReceived(messageId);
    }

    @Test
    void testMessageIdEmpty() {
        Assertions.assertThrows(RetrieveMessageFault.class, () -> retrieveMessageFail("", "Message ID is empty"));
    }

    @Test
    void testMessageNotFound() {
        Assertions.assertThrows(RetrieveMessageFault.class, () -> retrieveMessageFail("notFound", "Message not found, id [notFound]"));
    }

    @Test
    public void testMessageIdNeedsATrimSpaces() throws Exception {
        retrieveMessage("    13bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu ");
    }

    @Test
    public void testMessageIdNeedsATrimTabs() throws Exception {
        retrieveMessage("\t23bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu\t");
    }

    @Test
    @Disabled("[EDELIVERY-8828] WSPLUGIN: tests for rest methods ignored")
    public void testMessageIdNeedsATrimSpacesAndTabs() throws Exception {
        retrieveMessage(" \t 33bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu \t ");
    }

    @Test
    public void testRetrieveMessageOk() throws Exception {
        retrieveMessage("53bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu");
    }

    private void retrieveMessageFail(String messageId, String errorMessage) throws RetrieveMessageFault {
        RetrieveMessageRequest retrieveMessageRequest = createRetrieveMessageRequest(messageId);

        Holder<RetrieveMessageResponse> retrieveMessageResponse = new Holder<>();
        Holder<Messaging> ebMSHeaderInfo = new Holder<>();

        try {
            backendWebService.retrieveMessage(retrieveMessageRequest, retrieveMessageResponse, ebMSHeaderInfo);
        } catch (RetrieveMessageFault re) {
            Assertions.assertEquals(errorMessage, re.getMessage());
            throw re;
        }
        Assertions.fail("DownloadMessageFault was expected but was not raised");
    }

    private void retrieveMessage(String messageId) throws Exception {
        final String trimmedMessage = StringUtils.trim(messageId);

        receiveMessage(trimmedMessage);

        // requires a time to consume messages from the notification queue
        waitForMessage(trimmedMessage);
        RetrieveMessageRequest retrieveMessageRequest = createRetrieveMessageRequest(messageId);
        Holder<RetrieveMessageResponse> retrieveMessageResponse = new Holder<>();
        Holder<Messaging> ebMSHeaderInfo = new Holder<>();

        try {
            backendWebService.retrieveMessage(retrieveMessageRequest, retrieveMessageResponse, ebMSHeaderInfo);
        } catch (RetrieveMessageFault dmf) {
            Assertions.assertTrue(dmf.getMessage().contains(WebServiceImpl.MESSAGE_NOT_FOUND_ID));
            throw dmf;
        }
        Assertions.assertFalse(retrieveMessageResponse.value.getPayload().isEmpty());
        LargePayloadType payloadType = retrieveMessageResponse.value.getPayload().iterator().next();
        String payload = IOUtils.toString(payloadType.getValue().getDataSource().getInputStream(), Charset.defaultCharset());
        Assertions.assertEquals("", payload);
    }

    private RetrieveMessageRequest createRetrieveMessageRequest(String messageId) {
        RetrieveMessageRequest retrieveMessageRequest = new RetrieveMessageRequest();
        retrieveMessageRequest.setMessageID(messageId);
        return retrieveMessageRequest;
    }
}
