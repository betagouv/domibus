package eu.domibus.core.ebms3.receiver.token;

import com.sun.xml.messaging.saaj.soap.impl.TextImpl;
import eu.domibus.core.ebms3.receiver.interceptor.TrustSenderInterceptor;
import eu.domibus.test.common.MessageTestUtility;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.dom4j.dom.DOMText;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.stream.XMLStreamException;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "UnusedAssignment"})
@ExtendWith(JMockitExtension.class)
public class TokenReferenceExtractorTest {

    @Tested
    private TokenReferenceExtractor tokenReferenceExtractor;

    @Test
    public void testX509PKIPathV1TokenReference(@Injectable final Element securityHeader,
                                                @Injectable final Element signature,
                                                @Injectable final Element keyInfo,
                                                @Injectable final Element securityTokenReference,
                                                @Injectable final Node uriNode,
                                                @Injectable final Node valueTypeNode) throws WSSecurityException {

        final String uri = "#X509-5f905b9f-f1e2-4f05-8369-123f330455d1";
        final String valueType = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1";

        new Expectations() {{
            securityHeader.getFirstChild();
            result = signature;
            signature.getLocalName();
            result = WSConstants.SIGNATURE.getLocalPart();
            signature.getNamespaceURI();
            result = WSConstants.SIGNATURE.getNamespaceURI();
            signature.getFirstChild();
            result = keyInfo;
            keyInfo.getLocalName();
            result = TrustSenderInterceptor.KEYINFO.getLocalPart();
            keyInfo.getNamespaceURI();
            result = TrustSenderInterceptor.KEYINFO.getNamespaceURI();
            keyInfo.getFirstChild();
            result = securityTokenReference;
            securityTokenReference.getNodeType();
            result = Node.ELEMENT_NODE;
            securityTokenReference.getChildNodes().getLength();
            result = 1;
            securityTokenReference.getChildNodes().item(0).getLocalName();
            result = TokenReferenceExtractor.REFERENCE;
            securityTokenReference.getChildNodes().item(0).getAttributes().getNamedItem(TokenReferenceExtractor.URI);
            result = uriNode;
            uriNode.getNodeValue();
            result = uri;
            securityTokenReference.getChildNodes().item(0).getAttributes().getNamedItem(TokenReferenceExtractor.VALUE_TYPE);
            result = valueTypeNode;
            valueTypeNode.getNodeValue();
            result = valueType;

        }};
        final BinarySecurityTokenReference tokenReference = (BinarySecurityTokenReference) tokenReferenceExtractor.extractTokenReference(securityHeader);
        Assertions.assertEquals(uri, tokenReference.getUri());
        Assertions.assertEquals(valueType, tokenReference.getValueType());

    }

    @Test
    public void testKeySubjectIdendtifier(@Injectable final Element securityHeader,
                                          @Injectable final Element signature,
                                          @Injectable final Element keyInfo,
                                          @Injectable final Element securityTokenrRefecence) throws WSSecurityException {

        new Expectations() {{
            securityHeader.getFirstChild();
            result = signature;
            signature.getLocalName();
            result = WSConstants.SIGNATURE.getLocalPart();
            signature.getNamespaceURI();
            result = WSConstants.SIGNATURE.getNamespaceURI();
            signature.getFirstChild();
            result = keyInfo;
            keyInfo.getLocalName();
            result = TrustSenderInterceptor.KEYINFO.getLocalPart();
            keyInfo.getNamespaceURI();
            result = TrustSenderInterceptor.KEYINFO.getNamespaceURI();
            keyInfo.getFirstChild();
            result = securityTokenrRefecence;
            securityTokenrRefecence.getNodeType();
            result = Node.ELEMENT_NODE;
            securityTokenrRefecence.getChildNodes().getLength();
            result = 1;
            securityTokenrRefecence.getChildNodes().item(0).getLocalName();
            result = "KeyIdentifier";

        }};
        final BinarySecurityTokenReference tokenReference = (BinarySecurityTokenReference) tokenReferenceExtractor.extractTokenReference(securityHeader);
        Assertions.assertNull(tokenReference);

    }

    @Test
    public void testGetSecTokenRefWithSpacesWithMock(@Injectable final Element securityHeader,
                                                     @Injectable final Element signature,
                                                     @Injectable final Element keyInfo,
                                                     @Injectable final Node textNode,
                                                     @Injectable final Element securityTokenrRefecence) {

        new Expectations() {{
            securityHeader.getFirstChild();
            result = signature;
            signature.getLocalName();
            result = WSConstants.SIGNATURE.getLocalPart();
            signature.getNamespaceURI();
            result = WSConstants.SIGNATURE.getNamespaceURI();
            signature.getFirstChild();
            result = keyInfo;
            keyInfo.getLocalName();
            result = TrustSenderInterceptor.KEYINFO.getLocalPart();
            keyInfo.getNamespaceURI();
            result = TrustSenderInterceptor.KEYINFO.getNamespaceURI();
            keyInfo.getFirstChild();
            result = textNode;
            textNode.getNodeType();
            result = Node.TEXT_NODE;
            textNode.getNextSibling();
            result = securityTokenrRefecence;
            securityTokenrRefecence.getNodeType();
            result = Node.ELEMENT_NODE;
        }};
        final Element resultReferenceElement = tokenReferenceExtractor.getSecTokenRef(securityHeader);
        Assertions.assertEquals(securityTokenrRefecence, resultReferenceElement);

    }

    @Test
    public void testGetSecTokenFromXMLWithSpaces() throws WSSecurityException, XMLStreamException {
        Document doc = MessageTestUtility.readDocument("/dataset/as4/RawXMLMessageWithSpaces.xml");
        final Element soapHeader = WSSecurityUtil.getSOAPHeader(doc);

        final Element securityHeader = WSSecurityUtil.getSecurityHeader(soapHeader, null, true);

        Element getToken = tokenReferenceExtractor.getSecTokenRef(securityHeader);
        Assertions.assertNotNull(getToken);
    }

    @Test
    public void testGetFirstChildElement(@Injectable final Node parentNode,
                                         @Injectable final Node someNode,
                                         @Injectable final TextImpl<DOMText> textNode,
                                         @Injectable final Element resultElement) {

        new Expectations() {{
            parentNode.getFirstChild();
            result = someNode;
            someNode.getNodeType();
            result = Node.TEXT_NODE;
            someNode.getNextSibling();
            result = textNode;
            textNode.getNodeType();
            result = Node.TEXT_NODE;
            textNode.getNextSibling();
            result = resultElement;
            resultElement.getNodeType();
            result = Node.ELEMENT_NODE;
        }};

        Element targetElement = tokenReferenceExtractor.getFirstChildElement(parentNode);
        Assertions.assertEquals(resultElement, targetElement);
    }
}
