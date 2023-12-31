package eu.domibus.core.ebms3.receiver.interceptor;

import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.core.crypto.Wss4JMultiDomainCryptoProvider;
import eu.domibus.core.ebms3.receiver.token.BinarySecurityTokenReference;
import eu.domibus.core.ebms3.receiver.token.TokenReferenceExtractor;
import eu.domibus.core.ebms3.sender.interceptor.SoapInterceptorTest;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.test.common.PKIUtil;
import eu.domibus.test.common.SoapSampleUtil;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING;

/**
 * @author idragusa
 * @since 4.0
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked"})
@ExtendWith(JMockitExtension.class)
public class TrustSenderInterceptorTest extends SoapInterceptorTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TrustSenderInterceptorTest.class);

    private static final String X_509_V_3 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3";

    @Injectable
    CertificateService certificateService;

    @Injectable
    protected JAXBContext jaxbContextEBMS;

    @Injectable
    TokenReferenceExtractor tokenReferenceExtractor;

    @Injectable
    Wss4JMultiDomainCryptoProvider wss4JMultiDomainCryptoProvider;

    @Tested
    TrustSenderInterceptor trustSenderInterceptor;

    PKIUtil pkiUtil = new PKIUtil();

    @Test
    public void testHandleMessageBinaryToken(@Injectable SpringContextProvider springContextProvider, @Injectable final Element securityHeader, @Injectable final BinarySecurityTokenReference binarySecurityTokenReference, @Injectable X509Certificate x509Certificate) throws XMLStreamException, ParserConfigurationException, SOAPException, WSSecurityException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");

        new Expectations() {{
            tokenReferenceExtractor.extractTokenReference(withAny(securityHeader));
            result = binarySecurityTokenReference;
            binarySecurityTokenReference.getUri();
            result = "#X509-99bde7b7-932f-4dbd-82dd-3539ba51791b";
            binarySecurityTokenReference.getValueType();
            result = X_509_V_3;
            certificateService.extractLeafCertificateFromChain((List<X509Certificate>) any);
            result = x509Certificate;
        }};
        testHandleMessage(doc);
    }

    @Test
    void testSenderTrustFault(@Injectable final Element securityHeader,
                              @Injectable final BinarySecurityTokenReference binarySecurityTokenReference,
                              @Injectable X509Certificate x509Certificate) throws XMLStreamException, ParserConfigurationException, WSSecurityException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");

        new Expectations() {{
            tokenReferenceExtractor.extractTokenReference(withAny(securityHeader));
            result = binarySecurityTokenReference;
            binarySecurityTokenReference.getUri();
            result = "#X509-99bde7b7-932f-4dbd-82dd-3539ba51791b";
            binarySecurityTokenReference.getValueType();
            result = X_509_V_3;
            certificateService.isCertificateChainValid((List<Certificate>) any);
            result = false;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING);
            result = true;
            certificateService.extractLeafCertificateFromChain((List<X509Certificate>) any);
            result = x509Certificate;
        }};
        Assertions.assertThrows(org.apache.cxf.interceptor.Fault.class, () -> testHandleMessage(doc));
    }

    @Test
    public void testSenderTrustNoSenderVerification(@Injectable final Element securityHeader, @Injectable BinarySecurityTokenReference binarySecurityTokenReference, @Injectable X509Certificate x509Certificate) throws XMLStreamException, ParserConfigurationException, SOAPException, WSSecurityException {
        Document doc = readDocument("dataset/as4/SoapRequestBinaryToken.xml");

        new Expectations() {{
            tokenReferenceExtractor.extractTokenReference(withAny(securityHeader));
            result = binarySecurityTokenReference;
            binarySecurityTokenReference.getUri();
            result = "#X509-99bde7b7-932f-4dbd-82dd-3539ba51791b";
            binarySecurityTokenReference.getValueType();
            result = X_509_V_3;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING);
            result = false;
            certificateService.extractLeafCertificateFromChain((List<X509Certificate>) any);
            result = x509Certificate;
        }};
        testHandleMessage(doc);
        new Verifications() {{
            certificateService.isCertificateChainValid((List<Certificate>) any);
            times = 0;
        }};
    }

    @Test
    public void testGetCertificateFromBinarySecurityTokenX509v3(@Injectable final BinarySecurityTokenReference binarySecurityTokenReference) throws XMLStreamException, ParserConfigurationException, WSSecurityException, CertificateException, NoSuchProviderException, URISyntaxException {
        new Expectations() {{
            binarySecurityTokenReference.getUri();
            result = "#X509-9973d6a2-7819-4de2-a3d2-1bbdb2506df8";
            binarySecurityTokenReference.getValueType();
            result = X_509_V_3;
        }};
        Document doc = readDocument("dataset/as4/RawXMLMessageWithSpaces.xml");
        final List<? extends Certificate> xc = trustSenderInterceptor.getCertificateFromBinarySecurityToken(doc.getDocumentElement(), binarySecurityTokenReference);
        Assertions.assertNotNull(xc.get(0));
        Assertions.assertNotNull(((X509Certificate) xc.get(0)).getIssuerDN());
    }

    @Test
    public void testGetCertificateFromBinarySecurityTokenX509PKIPathv1(@Injectable final BinarySecurityTokenReference binarySecurityTokenReference) throws XMLStreamException, ParserConfigurationException, WSSecurityException, CertificateException, NoSuchProviderException, URISyntaxException {
        new Expectations() {{
            binarySecurityTokenReference.getUri();
            result = "#X509-9973d6a2-7819-4de2-a3d2-1bbdb2506df8";
            binarySecurityTokenReference.getValueType();
            result = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1";
        }};
        Document doc = readDocument("dataset/as4/RawXMLMessageWithSpacesAndPkiPath.xml");
        final List<? extends Certificate> certificateFromBinarySecurityToken = trustSenderInterceptor.getCertificateFromBinarySecurityToken(doc.getDocumentElement(), binarySecurityTokenReference);
        Assertions.assertNotNull(certificateFromBinarySecurityToken.get(0));
        Assertions.assertNotNull(((X509Certificate) certificateFromBinarySecurityToken.get(0)).getIssuerDN());
    }


    protected void testHandleMessage(Document doc) throws SOAPException {
        SoapMessage soapMessage = getSoapMessageForDom(doc);

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING);
            result = true;
        }};
        trustSenderInterceptor.handleMessage(soapMessage);
        String senderPartyName = LOG.getMDC(DomibusLogger.MDC_FROM);
        String receiverPartyName = LOG.getMDC(DomibusLogger.MDC_TO);

        Assertions.assertEquals("blue_gw", senderPartyName);
        Assertions.assertEquals("red_gw", receiverPartyName);

    }

    @Test
    public void testCheckCertificateValidityEnabled() throws Exception {
        final X509Certificate certificate = pkiUtil.createCertificate(BigInteger.ONE, null);
        final X509Certificate expiredCertificate = pkiUtil.createCertificate(BigInteger.ONE, Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusDays(2).toInstant()), Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant()), null);
        List<Certificate> certificateChain = new ArrayList<>();
        certificateChain.add(certificate);
        List<Certificate> expiredCertificateChain = new ArrayList<>();
        expiredCertificateChain.add(expiredCertificate);

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING);
            result = true;
            certificateService.isCertificateChainValid(certificateChain);
            result = true;
            certificateService.isCertificateChainValid(expiredCertificateChain);
            result = false;

        }};

        Assertions.assertTrue(trustSenderInterceptor.checkCertificateValidity(certificateChain, "test sender", false));
        Assertions.assertFalse(trustSenderInterceptor.checkCertificateValidity(expiredCertificateChain, "test sender", false));
    }

    @Test
    public void testCheckCertificateValidityDisabled() throws Exception {
        final X509Certificate expiredCertificate = pkiUtil.createCertificate(BigInteger.ONE, Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusDays(2).toInstant()), Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant()), null);
        List<Certificate> expiredCertificateChain = new ArrayList<>();
        expiredCertificateChain.add(expiredCertificate);

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONRECEIVING);
            result = false;
        }};
        Assertions.assertTrue(trustSenderInterceptor.checkCertificateValidity(expiredCertificateChain, "test sender", false));
    }

    @Test
    public void testHandleOneTestActivated() throws IOException {
        SoapMessage message = new SoapSampleUtil().createSoapMessage("SOAPMessage2.xml", "id");
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_ONRECEIVING);
            result = false;
        }};
        trustSenderInterceptor.handleMessage(message);
        new FullVerifications() {
        };
    }
}
