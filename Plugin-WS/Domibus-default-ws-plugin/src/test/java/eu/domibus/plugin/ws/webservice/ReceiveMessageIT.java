package eu.domibus.plugin.ws.webservice;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.ws.AbstractBackendWSIT;
import eu.domibus.plugin.ws.backend.dispatch.WSPluginDispatchClientProvider;
import eu.domibus.test.common.BackendConnectorMock;
import eu.domibus.test.common.SoapSampleUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import java.io.IOException;
import java.util.UUID;


/**
 * This class implements the test cases Receive Message-01 and Receive Message-02.
 *
 * @author draguio
 * @author martifp
 */
@Transactional
public class ReceiveMessageIT extends AbstractBackendWSIT {

    @Autowired
    MSHWebservice mshWebserviceTest;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    MessageRetentionDefaultService messageRetentionDefaultService;

    @Autowired
    WSPluginDispatchClientProvider wsPluginDispatchClientProvider;

    @Autowired
    private BackendConnectorProvider backendConnectorProvider;

    @Autowired
    protected PayloadFileStorageProvider payloadFileStorageProvider;

    @BeforeEach
    public void before(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException {
        payloadFileStorageProvider.initialize();

        Mockito.when(backendConnectorProvider.getBackendConnector(ArgumentMatchers.anyString()))
                .thenReturn(new BackendConnectorMock("name"));

        uploadPmode(wmRuntimeInfo.getHttpPort());
    }

    /**
     * This test invokes the MSHWebService and verifies that the message is stored
     * in the database with the status RECEIVED
     *
     * @throws SOAPException, IOException, SQLException, ParserConfigurationException, SAXException
     *                        <p>
     *                        ref: Receive Message-01
     */
    @Test
    public void testReceiveMessage() throws SOAPException, IOException, ParserConfigurationException, SAXException, InterruptedException {
        String filename = "SOAPMessage2.xml";
        String messageId = UUID.randomUUID() + "@domibus.eu";

        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        mshWebserviceTest.invoke(soapMessage);

        deleteAllMessages(messageId);
    }

    @Test
    public void testDeleteBatch() throws SOAPException, IOException, ParserConfigurationException, SAXException, InterruptedException {
        String filename = "SOAPMessage2.xml";
        String messageId = UUID.randomUUID() + "@domibus.eu";

        Dispatch dispatch = Mockito.mock(Dispatch.class);
        SOAPMessage reply = Mockito.mock(SOAPMessage.class);
        Mockito.when(dispatch.invoke(Mockito.any(SOAPMessage.class)))
                .thenReturn(reply);
        Mockito.when(wsPluginDispatchClientProvider.getClient(Mockito.any(String.class), Mockito.any(String.class)))
                .thenReturn(dispatch);

        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        mshWebserviceTest.invoke(soapMessage);

        deleteAllMessages(messageId);

        Thread.sleep(1000);
    }

    @Test
    public void testReceiveTestMessage() throws Exception {
        String filename = "SOAPTestMessage.xml";
        String messageId = "ping123@domibus.eu";
        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);

        mshWebserviceTest.invoke(soapMessage);

        deleteAllMessages(messageId);
    }
}
