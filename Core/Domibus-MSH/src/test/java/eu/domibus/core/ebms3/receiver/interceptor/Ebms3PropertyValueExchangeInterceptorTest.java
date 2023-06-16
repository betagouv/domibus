package eu.domibus.core.ebms3.receiver.interceptor;

import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.core.ebms3.sender.interceptor.SoapInterceptorTest;
import eu.domibus.api.model.MessageType;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
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
public class Ebms3PropertyValueExchangeInterceptorTest extends SoapInterceptorTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(Ebms3PropertyValueExchangeInterceptorTest.class);

    @Tested
    PropertyValueExchangeInterceptor propertyValueExchangeInterceptor;

    @Test
    public void testHandleMessage() throws XMLStreamException, ParserConfigurationException, JAXBException, IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, SOAPException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");
        SoapMessage soapMessage = getSoapMessageForDom(doc);
        propertyValueExchangeInterceptor.handleMessage(soapMessage);
        Assertions.assertEquals(soapMessage.get(MSHDispatcher.MESSAGE_TYPE_OUT), MessageType.USER_MESSAGE);
    }
}
