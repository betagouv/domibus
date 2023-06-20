package eu.domibus.plugin.ws.webservice.deprecated;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.core.message.nonrepudiation.NonRepudiationChecker;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.webService.generated.SubmitMessageFault;
import eu.domibus.plugin.webService.generated.SubmitRequest;
import eu.domibus.plugin.webService.generated.SubmitResponse;
import eu.domibus.plugin.ws.AbstractBackendWSIT;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by draguio on 17/02/2016.
 * @deprecated to be removed when deprecated endpoint /backend is removed
 */
@Deprecated
public class SubmitMessageSignOnlyIT extends AbstractBackendWSIT {

    @Autowired
    NonRepudiationChecker nonRepudiationChecker;

    @BeforeEach
    public void before(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException {
        uploadPmode(wmRuntimeInfo.getHttpPort());
    }

    /**
     * Test for the backend sendMessage service with payload profile enabled
     */
    @Test
    @Disabled("[EDELIVERY-8828] WSPLUGIN: tests for rest methods ignored")
    public void testSubmitMessageValid() throws SubmitMessageFault {
        String payloadHref = "cid:message";
        SubmitRequest submitRequest = createSubmitRequest(payloadHref);
        Messaging ebMSHeaderInfo = createMessageHeader(payloadHref);
        ebMSHeaderInfo.getUserMessage().getCollaborationInfo().setAction("TC4Leg1");

        super.prepareSendMessage("validAS4Response.xml", Pair.of("MESSAGE_ID", UUID.randomUUID()+"@domibus.eu"), Pair.of("REF_MESSAGE_ID", UUID.randomUUID() + "@domibus.eu"));
        SubmitResponse response = backendWebService.submitMessage(submitRequest, ebMSHeaderInfo);

        final List<String> messageID = response.getMessageID();
        assertNotNull(response);
        assertNotNull(messageID);
        assertEquals(1, messageID.size());
        final String messageId = messageID.iterator().next();

        //message will fail as the response message does not contain the right security details(signature, etc)
        waitUntilMessageIsInWaitingForRetry(messageId);

        verify(postRequestedFor(urlMatching("/domibus/services/msh"))
                .withRequestBody(containing("DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\""))
                .withHeader("Content-Type", notMatching("application/soap+xml")));
    }
}
