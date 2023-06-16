package eu.domibus.core.ebms3.sender.interceptor;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.api.spring.SpringContextProvider;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.binding.soap.SoapMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * @author idragusa
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class PrepareAttachmentInterceptorTest extends SoapInterceptorTest {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PrepareAttachmentInterceptorTest.class);

    @Tested
    PrepareAttachmentInterceptor prepareAttachmentInterceptor;

    @Test
    public void testHandleMessage() throws XMLStreamException, ParserConfigurationException, SOAPException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");
        SoapMessage soapMessage = getSoapMessageForDom(doc);
        prepareAttachmentInterceptor.handleMessage(soapMessage);
        Assertions.assertNotNull(soapMessage.getAttachments());
        Assertions.assertEquals(1, soapMessage.getAttachments().size());
    }
}
