package eu.domibus.core.ebms3;

import eu.domibus.api.exceptions.XmlProcessingException;
import eu.domibus.api.util.SoapElementsExtractorUtil;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * Utility class for extracting various elements from the SoapMessage
 *
 * @author Lucian Furca
 * @since 5.2
 */
@Component
public class SoapElementsExtractorUtilImpl implements SoapElementsExtractorUtil {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SoapElementsExtractorUtilImpl.class);

    protected final XMLUtil xmlUtil;
    public static final String WSSE_SECURITY = "wsse:Security";
    public static final String ALGORITHM_ATTRIBUTE = "Algorithm";
    public static final String ENCRYPTION_METHOD = "EncryptionMethod";
    public static final String AGREEMENT_METHOD = "AgreementMethod";
    public static final QName KEY_INFO = new QName("http://www.w3.org/2000/09/xmldsig#", "KeyInfo");
    public static final String SIGNATURE = "Signature";
    public static final String SIGNED_INFO = "SignedInfo";
    public static final String SIGNATURE_METHOD = "SignatureMethod";


    public SoapElementsExtractorUtilImpl(XMLUtil xmlUtil) {
        this.xmlUtil = xmlUtil;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEncryptionAlgorithm(SoapMessage soapMessage) throws WSSecurityException, XmlProcessingException {
        Node soapSecurityHeader = extractSecurityHeaderElement(soapMessage);

        Element encryptedKeyElement = getChildElementByName(soapSecurityHeader, WSConstants.ENCRYPTED_KEY.getLocalPart());
        Element encryptionMethodElement = getChildElementByName(encryptedKeyElement, ENCRYPTION_METHOD);
        String encryptionMethodAlgorithm = encryptionMethodElement.getAttributes().getNamedItem(ALGORITHM_ATTRIBUTE).getNodeValue();
        if (encryptionMethodAlgorithm.equalsIgnoreCase(ENCRYPTION_METHOD_ALGORITHM_RSA)) {
            //RSA
            return encryptionMethodAlgorithm;
        } else if (encryptionMethodAlgorithm.equalsIgnoreCase(ENCRYPTION_METHOD_ALGORITHM_ECC)) {
            //ECC
            Element keyInfoElement = getChildElementByName(encryptedKeyElement, KEY_INFO.getLocalPart());
            Element agreementMethodElement = getChildElementByName(keyInfoElement, AGREEMENT_METHOD);
            return agreementMethodElement.getAttributes().getNamedItem(ALGORITHM_ATTRIBUTE).getLocalName();
        } else {
            LOG.error("Invalid encryption method algorithm: [{}]", encryptionMethodAlgorithm);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "Invalid encryption method algorithm: " + encryptionMethodAlgorithm);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSignatureAlgorithm(SoapMessage soapMessage) throws WSSecurityException, XmlProcessingException {
        Node soapSecurityHeader = extractSecurityHeaderElement(soapMessage);

        Element signatureElement = getChildElementByName(soapSecurityHeader, SIGNATURE);
        Element signedInfoElement = getChildElementByName(signatureElement, SIGNED_INFO);
        Element signatureMethodElement = getChildElementByName(signedInfoElement, SIGNATURE_METHOD);

        String signatureMethodAlgorithm = signatureMethodElement.getAttributes().getNamedItem(ALGORITHM_ATTRIBUTE).getNodeValue();
        switch (signatureMethodAlgorithm) {
            case WSS4JConstants.RSA_SHA256:
            case WSS4JConstants.ECDSA_SHA256:
                return signatureMethodAlgorithm;
            default:
                LOG.error("Invalid signature method algorithm: [{}]", signatureMethodAlgorithm);
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "Invalid signature method algorithm: " + signatureMethodAlgorithm);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element extractSecurityHeaderElement(SoapMessage soapMessage) throws WSSecurityException, XmlProcessingException {
        String messageAsXmlString = getSoapMessageAsString(soapMessage);
        try (StringReader stringReader = new StringReader(messageAsXmlString)){
            DocumentBuilderFactory dbFactory = xmlUtil.getDocumentBuilderFactoryNamespaceAware();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();

            InputSource inputSource = new InputSource(stringReader);
            Document doc = builder.parse(inputSource);
            doc.getDocumentElement().normalize();

            Element securityHeader = (Element) doc.getDocumentElement().getElementsByTagName(WSSE_SECURITY).item(0);
            if (securityHeader == null) {
                String errorMessage = "Soap Security Header is null";
                LOG.error(errorMessage);
                throw new XmlProcessingException(errorMessage);
            }
            return securityHeader;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Could not extract security header from Soap Message: ", e);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "Could not retrieve security header from Soap Message");
        }
    }

    protected Element getChildElementByName(Node parentNode, String nodeName) throws XmlProcessingException {
        for (Node currentChild = parentNode.getFirstChild(); currentChild != null; currentChild = currentChild.getNextSibling()) {
            if (Node.ELEMENT_NODE == currentChild.getNodeType() && nodeName.equalsIgnoreCase(currentChild.getLocalName())) {
                return (Element) currentChild;
            }
        }
        LOG.error("[{}] element is null", nodeName);
        throw new XmlProcessingException("The following xml element is null: " + nodeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSoapMessageAsString(SoapMessage soapMessage) {
        String soapMessageAsText = null;
        try (InputStream inputStream = soapMessage.getContent(InputStream.class);
             CachedOutputStream outputStream = new CachedOutputStream()) {
            if (inputStream != null) {
                IOUtils.copy(inputStream, outputStream);

                outputStream.flush();
                inputStream.close();

                soapMessage.setContent(InputStream.class, outputStream.getInputStream());
                outputStream.close();

                String rawMessage = new String(outputStream.getBytes());
                soapMessageAsText = rawMessage.substring(rawMessage.indexOf("<env:Envelope"),
                        rawMessage.indexOf("</env:Envelope>") + "</env:Envelope>".length());
            }
        } catch (IOException e) {
            LOG.error("Could not retrieve the SOAP Message as an XML", e);
            return null;
        }
        return soapMessageAsText;
    }
}