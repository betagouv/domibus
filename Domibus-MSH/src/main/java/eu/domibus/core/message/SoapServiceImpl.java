package eu.domibus.core.message;

import eu.domibus.common.ErrorCode;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.ObjectFactory;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.neethi.builders.converters.StaxToDOMConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.soap.SOAPException;
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


    public Messaging getMessage(final SoapMessage message) throws IOException, EbMS3Exception {
        final InputStream inputStream = message.getContent(InputStream.class);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy(inputStream, byteArrayOutputStream); //FIXME: do not copy the whole byte[], use SequenceInputstream instead
        final byte[] data = byteArrayOutputStream.toByteArray();
        message.setContent(InputStream.class, new ByteArrayInputStream(data));
        new StaxInInterceptor().handleMessage(message);
        final XMLStreamReader xmlStreamReader = message.getContent(XMLStreamReader.class);
        if (xmlStreamReader == null) {
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0009, "Messaging header is missing!", null, null);
        }
        final Element soapEnvelope = new StaxToDOMConverter().convert(xmlStreamReader);
        message.removeContent(XMLStreamReader.class);
        message.setContent(InputStream.class, new ByteArrayInputStream(data));
        final Node messagingNode = soapEnvelope.getElementsByTagNameNS(ObjectFactory._Messaging_QNAME.getNamespaceURI(), ObjectFactory._Messaging_QNAME.getLocalPart()).item(0);
        if (messagingNode == null) {
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0009, "Messaging header is empty!", null, null);
        }

        try {
            return messageUtil.getMessagingWithDom(messagingNode);
        } catch (SOAPException e) {
            LOG.error("Error unmarshalling Messaging header", e);
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0009, "Messaging header is empty!", null, e);
        }
    }

    public String getMessagingAsRAWXml(final SoapMessage message) throws IOException, EbMS3Exception, TransformerException {
        final InputStream inputStream = message.getContent(InputStream.class);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        copy(inputStream, byteArrayOutputStream);
        final byte[] data = byteArrayOutputStream.toByteArray();
        message.setContent(InputStream.class, new ByteArrayInputStream(data));
        new StaxInInterceptor().handleMessage(message);
        final XMLStreamReader xmlStreamReader = message.getContent(XMLStreamReader.class);
        if (xmlStreamReader == null) {
            throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0009, "Messaging header is missing!", null, null);
        }

        final Element soapEnvelope = new StaxToDOMConverter().convert(xmlStreamReader);
        message.removeContent(XMLStreamReader.class);
        message.setContent(InputStream.class, new ByteArrayInputStream(data));
        final Node messagingNode = soapEnvelope.getElementsByTagNameNS(ObjectFactory._Messaging_QNAME.getNamespaceURI(), ObjectFactory._Messaging_QNAME.getLocalPart()).item(0);

        return soapUtil.getRawXMLMessage(messagingNode);
    }
}


