package eu.domibus.plugin;

import eu.domibus.AbstractBackendWSIT;
import eu.domibus.messaging.XmlProcessingException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import java.io.IOException;


/**
 * This class implements the test cases Receive Message-01 and Receive Message-02.
 *
 * @author draguio
 * @author martifp
 */
@DirtiesContext
@Rollback
public class ReceiveMessageIT extends AbstractBackendWSIT {

    @Autowired
    Provider<SOAPMessage> mshWebserviceTest;

    @Before
    public void before() throws IOException, XmlProcessingException {
        uploadPmode(wireMockRule.port());
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
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void testReceiveMessage() throws SOAPException, IOException, ParserConfigurationException, SAXException {
        String filename = "SOAPMessage2.xml";
        String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";
        SOAPMessage soapMessage = createSOAPMessage(filename);
        mshWebserviceTest.invoke(soapMessage);

        waitUntilMessageIsReceived(messageId);
    }

    @Test
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void testReceiveTestMessage() throws Exception {
        String filename = "SOAPTestMessage.xml";
        String messageId = "ping123@domibus.eu";
        SOAPMessage soapMessage = createSOAPMessage(filename);

        mshWebserviceTest.invoke(soapMessage);
        waitUntilMessageIsReceived(messageId);
    }

}
