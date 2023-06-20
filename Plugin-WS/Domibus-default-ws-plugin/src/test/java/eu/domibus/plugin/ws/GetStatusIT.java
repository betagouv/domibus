package eu.domibus.plugin.ws;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.common.JPAConstants;
import eu.domibus.core.ebms3.receiver.MSHWebservice;
import eu.domibus.core.message.MessagingService;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.ws.generated.StatusFault;
import eu.domibus.plugin.ws.generated.body.MessageStatus;
import eu.domibus.plugin.ws.generated.body.MshRole;
import eu.domibus.plugin.ws.generated.body.StatusRequestWithAccessPointRole;
import eu.domibus.test.DomibusConditionUtil;
import eu.domibus.test.PModeUtil;
import eu.domibus.test.common.BackendConnectorMock;
import eu.domibus.test.common.SoapSampleUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;

public class GetStatusIT extends AbstractBackendWSIT {

    @Autowired
    MessagingService messagingService;

    @Autowired
    MSHWebservice mshWebserviceTest;

    @Autowired
    DomibusConditionUtil domibusConditionUtil;

    @Autowired
    private BackendConnectorProvider backendConnectorProvider;

    @Autowired
    private PayloadFileStorageProvider payloadFileStorageProvider;

    @Autowired
    PModeUtil pModeUtil;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @BeforeEach
    public void before(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException {
        payloadFileStorageProvider.initialize();

        Mockito.when(backendConnectorProvider.getBackendConnector(ArgumentMatchers.anyString()))
                .thenReturn(new BackendConnectorMock("name"));

        pModeUtil.uploadPmode(wmRuntimeInfo.getHttpPort());
    }

    @Test
    public void testGetStatusReceived() throws StatusFault, IOException, SOAPException, SAXException, ParserConfigurationException {
        String filename = "SOAPMessage2.xml";
        String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";
        SOAPMessage soapMessage = soapSampleUtil.createSOAPMessage(filename, messageId);
        mshWebserviceTest.invoke(soapMessage);

        StatusRequestWithAccessPointRole messageStatusRequest = createMessageStatusRequest(messageId, MshRole.RECEIVING);
        MessageStatus response = webServicePluginInterface.getStatusWithAccessPointRole(messageStatusRequest);
        Assertions.assertEquals(MessageStatus.RECEIVED, response);
    }

    @Test
    public void testGetStatusInvalidId() throws StatusFault {
        String invalidMessageId = "invalid";
        StatusRequestWithAccessPointRole messageStatusRequest = createMessageStatusRequest(invalidMessageId, MshRole.RECEIVING);
        MessageStatus response = webServicePluginInterface.getStatusWithAccessPointRole(messageStatusRequest);
        Assertions.assertEquals(MessageStatus.NOT_FOUND, response);
    }

    @Test
    public void testGetStatusEmptyMessageId() {
        String emptyMessageId = "";
        StatusRequestWithAccessPointRole messageStatusRequest = createMessageStatusRequest(emptyMessageId, MshRole.RECEIVING);
        try {
            webServicePluginInterface.getStatusWithAccessPointRole(messageStatusRequest);
            Assertions.fail();
        } catch (StatusFault statusFault) {
            String message = "Message ID is empty";
            Assertions.assertEquals(message, statusFault.getMessage());
        }
    }

    private StatusRequestWithAccessPointRole createMessageStatusRequest(final String messageId, MshRole role) {
        StatusRequestWithAccessPointRole statusRequest = new StatusRequestWithAccessPointRole();
        statusRequest.setMessageID(messageId);
        statusRequest.setAccessPointRole(role);
        return statusRequest;
    }
}
