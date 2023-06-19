package eu.domibus.plugin.ws.webservice;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.message.MessagingService;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.ws.AbstractBackendWSIT;
import eu.domibus.plugin.ws.connector.WSPluginImpl;
import eu.domibus.plugin.ws.generated.RetrieveMessageFault;
import eu.domibus.plugin.ws.generated.body.RetrieveMessageRequest;
import eu.domibus.plugin.ws.generated.body.RetrieveMessageResponse;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import eu.domibus.test.common.SoapSampleUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Holder;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Disabled("EDELIVERY-6896")
public class RetrieveMessageIT extends AbstractBackendWSIT {

    @Autowired
    JMSManager jmsManager;

    @Autowired
    MessagingService messagingService;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    protected MessageRetriever messageRetriever;

    @Autowired
    UserMessageLogDefaultService userMessageLogService;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    MSHWebservice mshWebserviceTest;

    @Autowired
    WSPluginImpl wsPlugin;

    @Autowired
    MessageRetentionDefaultService messageRetentionDefaultService;

    @BeforeEach
    public void updatePMode(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException {
        uploadPmode(wmRuntimeInfo.getHttpPort());
    }

    @Test
    void testMessageIdEmpty() {
        Assertions.assertThrows(RetrieveMessageFault.class, () -> retrieveMessageFail("", "Message ID is empty"));
    }

    @Test
    void testMessageNotFound() {
        Assertions.assertThrows(RetrieveMessageFault.class, () -> retrieveMessageFail("notFound", "Message not found, id [notFound]"));
    }

    @Disabled("will be fixed by EDELIVERY-11139") //TODO
    @Test
    public void testRetrieveMessageOk() throws Exception {
        String filename = "SOAPMessage2.xml";
        String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";
        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        SOAPMessage soapResponse = mshWebserviceTest.invoke(soapMessage);

        waitForMessage(messageId);

        RetrieveMessageRequest retrieveMessageRequest = createRetrieveMessageRequest(messageId);
        Holder<RetrieveMessageResponse> retrieveMessageResponse = new Holder<>();
        Holder<eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging> ebMSHeaderInfo = new Holder<>();

        try {
            webServicePluginInterface.retrieveMessage(retrieveMessageRequest, retrieveMessageResponse, ebMSHeaderInfo);
        } catch (RetrieveMessageFault dmf) {
            Assertions.assertTrue(dmf.getMessage().contains(WebServiceImpl.MESSAGE_NOT_FOUND_ID));
            throw dmf;
        }

        final Messaging messaging = ebMSHeaderInfo.value;
        final UserMessage userMessage = messaging.getUserMessage();
        assertEquals(messageId, userMessage.getMessageInfo().getMessageId());
        assertEquals(2, userMessage.getMessageProperties().getProperty().size());

        deleteAllMessages(messageId);
    }

    private void retrieveMessageFail(String messageId, String errorMessage) throws RetrieveMessageFault {
        RetrieveMessageRequest retrieveMessageRequest = createRetrieveMessageRequest(messageId);

        Holder<RetrieveMessageResponse> retrieveMessageResponse = new Holder<>();
        Holder<eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging> ebMSHeaderInfo = new Holder<>();

        try {
            webServicePluginInterface.retrieveMessage(retrieveMessageRequest, retrieveMessageResponse, ebMSHeaderInfo);
        } catch (RetrieveMessageFault re) {
            assertEquals(errorMessage, re.getMessage());
            throw re;
        }
        Assertions.fail("DownloadMessageFault was expected but was not raised");
    }


    private RetrieveMessageRequest createRetrieveMessageRequest(String messageId) {
        RetrieveMessageRequest retrieveMessageRequest = new RetrieveMessageRequest();
        retrieveMessageRequest.setMessageID(messageId);
        return retrieveMessageRequest;
    }
}
