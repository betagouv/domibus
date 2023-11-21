package eu.domibus.core.ebms3.receiver;

import eu.domibus.AbstractIT;
import eu.domibus.core.ebms3.receiver.interceptor.AbortChainInterceptor;
import eu.domibus.test.common.SoapSampleUtil;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * @author Lucian Furca
 * @since 5.2
 */
public class AbortChainInterceptorIT extends AbstractIT {

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    AbortChainInterceptor abortChainInterceptor;

    @Test
    public void testHandleGetVerb() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();
        String filename = "SOAPMessageNoMessaging.xml";
        SoapMessage sm = soapSampleUtil.createSoapMessage(filename, UUID.randomUUID() + "@domibus.eu");
        sm.put("org.apache.cxf.request.method", "GET");
        sm.put(AbstractHTTPDestination.HTTP_RESPONSE, response);

        // handle message without adding any content
        abortChainInterceptor.handleMessage(sm);

        try {
            String reply = ((MockHttpServletResponse) sm.get(AbstractHTTPDestination.HTTP_RESPONSE)).getContentAsString();

            Assertions.assertTrue(reply.contains("domibus-MSH"));

        } catch (UnsupportedEncodingException e) {
            Assertions.fail();
        }
    }
}
