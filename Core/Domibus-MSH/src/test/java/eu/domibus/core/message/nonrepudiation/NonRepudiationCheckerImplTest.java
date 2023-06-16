package eu.domibus.core.message.nonrepudiation;

import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.core.util.xml.XMLUtilImpl;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.apache.wss4j.dom.WSConstants;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 3.3.1
 */
@ExtendWith(JMockitExtension.class)
public class NonRepudiationCheckerImplTest {

    NonRepudiationCheckerImpl nonRepudiationChecker = new NonRepudiationCheckerImpl();

    static MessageFactory messageFactory = null;

    

    @BeforeAll
    public static void init() throws SOAPException {
        messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
    }

    @Test
    public void testGetNonRepudiationNodeListFromRequest() throws Exception {
        final List<String> referencesFromSecurityHeader = getNonRepudiationNodeListFromRequest("dataset/as4/MSHAS4Request.xml");
        assertEquals(referencesFromSecurityHeader.size(), 6);
    }

    @Test
    public void testGetNonRepudiationNodeListFromResponse() throws Exception {
        final List<String> referencesFromNonRepudiationInformation = getNonRepudiationListFromResponse("dataset/as4/MSHAS4Response.xml");
        assertEquals(referencesFromNonRepudiationInformation.size(), 6);
    }

    @Test
    public void testGetNonRepudiationNodeListFromRequestSignOnly() throws Exception {
        final List<String> referencesFromSecurityHeader = getNonRepudiationNodeListFromRequest("dataset/as4/MSHAS4Request-signOnly.xml");
        assertEquals(referencesFromSecurityHeader.size(), 6);
    }

    @Test
    public void testGetNonRepudiationNodeListFromResponseSignOnly() throws Exception {
        final List<String> referencesFromNonRepudiationInformation = getNonRepudiationListFromResponse("dataset/as4/MSHAS4Response-signOnly.xml");
        assertEquals(referencesFromNonRepudiationInformation.size(), 6);
    }

    @Test
    public void compareUnorderedReferenceNodeLists() throws Exception {
        final List<String> referencesFromSecurityHeader = getNonRepudiationNodeListFromRequest("dataset/as4/MSHAS4Request.xml");
        final List<String> referencesFromNonRepudiationInformation = getNonRepudiationListFromResponse("dataset/as4/MSHAS4Response.xml");
        final boolean compareUnorderedReferenceNodeListsResult = nonRepudiationChecker.compareUnorderedReferenceNodeLists(referencesFromSecurityHeader, referencesFromNonRepudiationInformation);
        Assertions.assertTrue(compareUnorderedReferenceNodeListsResult);
    }

    @Test
    public void getNonRepudiationDetailsFromReceiptWithNullArgument() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> nonRepudiationChecker.getNonRepudiationDetailsFromReceipt(null));
        MatcherAssert.assertThat(ebMS3Exception.getMessage(), Matchers.containsString("Not found NonRepudiationDetails element."));
    }

    @Test
    public void compareUnorderedReferenceNodeListsSignOnly() throws Exception {
        final List<String> referencesFromSecurityHeader = getNonRepudiationNodeListFromRequest("dataset/as4/MSHAS4Request-signOnly.xml");
        final List<String> referencesFromNonRepudiationInformation = getNonRepudiationListFromResponse("dataset/as4/MSHAS4Response-signOnly.xml");
        final boolean compareUnorderedReferenceNodeListsResult = nonRepudiationChecker.compareUnorderedReferenceNodeLists(referencesFromSecurityHeader, referencesFromNonRepudiationInformation);
        Assertions.assertTrue(compareUnorderedReferenceNodeListsResult);
    }

    protected List<String> getNonRepudiationNodeListFromRequest(String path) throws Exception {
        SOAPMessage request = getSoapUtil().createSOAPMessage(IOUtils.toString(new ClassPathResource(path).getInputStream()));
        return nonRepudiationChecker.getNonRepudiationDetailsFromSecurityInfoNode(request.getSOAPHeader().getElementsByTagNameNS(WSConstants.SIG_NS, WSConstants.SIG_INFO_LN).item(0));
    }

    protected List<String> getNonRepudiationListFromResponse(String path) throws Exception {
        SOAPMessage response = getSoapUtil().createSOAPMessage(IOUtils.toString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8));
        return nonRepudiationChecker.getNonRepudiationDetailsFromReceipt(response.getSOAPHeader().getElementsByTagNameNS(NonRepudiationConstants.NS_NRR, NonRepudiationConstants.NRR_LN).item(0));
    }

    protected SoapUtil getSoapUtil() {
        return new SoapUtil(null, new XMLUtilImpl(null));
    }

}
