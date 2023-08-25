package eu.domibus.core.message;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.ObjectFactory;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.neethi.builders.converters.ConverterException;
import org.apache.neethi.builders.converters.StaxToDOMConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.cxf.helpers.IOUtils.copy;


/**
 * @author Thomas Dussart
 * @since 3.3
 */

@Service
public class SoapServiceImpl implements SoapService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SoapServiceImpl.class);

    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    protected SoapUtil soapUtil;

    @Autowired
    protected XMLUtil xmlUtil;


    public Ebms3Messaging getMessage(final SoapMessage message) throws IOException, EbMS3Exception {
        final Node messagingNode = getMessagingNode(message);
        if (messagingNode == null) {
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0009)
                    .message("Messaging header is empty!")
                    .build();
        }

        try {
            return messageUtil.getMessagingWithDom(messagingNode);
        } catch (SOAPException e) {
            LOG.error("Error unmarshalling Messaging header", e);
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0009)
                    .message("Messaging header is empty!")
                    .cause(e)
                    .build();
        }
    }

    public String getMessagingAsRAWXml(final SoapMessage message) throws IOException, EbMS3Exception, TransformerException {
        final Node messagingNode = getMessagingNode(message);

        return soapUtil.getRawXMLMessage(messagingNode);
    }

    private Node getMessagingNode(SoapMessage message) throws IOException, EbMS3Exception {
        final InputStream inputStream = message.getContent(InputStream.class);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //we use apache cxf IOUtils.copy intentionally here - do not replace it with other libraries
        copy(inputStream, byteArrayOutputStream);
        final byte[] data = byteArrayOutputStream.toByteArray();
        message.setContent(InputStream.class, new ByteArrayInputStream(data));
        new StaxInInterceptor().handleMessage(message);
        final XMLStreamReader xmlStreamReader = message.getContent(XMLStreamReader.class);
        if (xmlStreamReader == null) {
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0009)
                    .message("Messaging header is missing!")
                    .build();
        }
        final Element soapEnvelope = convert(xmlStreamReader);
        message.removeContent(XMLStreamReader.class);
        message.setContent(InputStream.class, new ByteArrayInputStream(data));
        return soapEnvelope.getElementsByTagNameNS(ObjectFactory._Messaging_QNAME.getNamespaceURI(), ObjectFactory._Messaging_QNAME.getLocalPart()).item(0);
    }

    protected Element convert(XMLStreamReader reader) {
        try {
            DocumentBuilderFactory dbf = xmlUtil.getDocumentBuilderFactory();
            Document doc = dbf.newDocumentBuilder().newDocument();
            StaxToDOMConverter.readDocElements(doc, doc, reader);
            return doc.getDocumentElement();
        } catch (ParserConfigurationException | XMLStreamException ex) {
            throw new ConverterException(ex);
        }
    }
}


