package eu.domibus.core.ebms3.receiver.policy;

import eu.domibus.AbstractIT;
import eu.domibus.core.ebms3.receiver.leg.MessageLegConfigurationFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.common.SoapSampleUtil;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;


/**
 * @author draguio
 * @since 3.3
 */
@Transactional
public class SetPolicyInInterceptorIT extends AbstractIT {

    @Autowired
    SoapSampleUtil soapSampleUtil;
    @Autowired
    SetPolicyInServerInterceptor setPolicyInInterceptorServer;

    @Autowired
    MessageLegConfigurationFactory serverInMessageLegConfigurationFactory;

    @BeforeEach
    public void before() throws IOException, XmlProcessingException {
        uploadPMode(SERVICE_PORT);
    }

    @Test
    public void testHandleMessage() throws  IOException {
        String expectedPolicy = "eDeliveryAS4Policy";
        String expectedSecurityAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

        String filename = "SOAPMessage2.xml";

        SoapMessage sm = soapSampleUtil.createSoapMessage(filename, UUID.randomUUID() + "@domibus.eu");

        setPolicyInInterceptorServer.handleMessage(sm);

        Assertions.assertEquals(expectedPolicy, ((Policy) sm.get(PolicyConstants.POLICY_OVERRIDE)).getId());
        Assertions.assertEquals(expectedSecurityAlgorithm, sm.get(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM));
    }

    @Test
    void testHandleMessageNull() throws IOException {

        String filename = "SOAPMessageNoMessaging.xml";
        SoapMessage sm = soapSampleUtil.createSoapMessage(filename, UUID.randomUUID() + "@domibus.eu");

        // handle message without adding any content
        Assertions.assertThrows(org.apache.cxf.interceptor.Fault. class,() -> setPolicyInInterceptorServer.handleMessage(sm));
    }



    @Test
    public void testHandleGetVerb() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();
        String filename = "SOAPMessageNoMessaging.xml";
        SoapMessage sm = soapSampleUtil.createSoapMessage(filename, UUID.randomUUID() + "@domibus.eu");
        sm.put("org.apache.cxf.request.method", "GET");
        sm.put(AbstractHTTPDestination.HTTP_RESPONSE, response);

        // handle message without adding any content
        setPolicyInInterceptorServer.handleMessage(sm);

        try {
            String reply = ((MockHttpServletResponse) sm.get(AbstractHTTPDestination.HTTP_RESPONSE)).getContentAsString();

            Assertions.assertTrue(reply.contains("domibus-MSH"));

        } catch (UnsupportedEncodingException e) {
            Assertions.fail();
        }
    }
}
