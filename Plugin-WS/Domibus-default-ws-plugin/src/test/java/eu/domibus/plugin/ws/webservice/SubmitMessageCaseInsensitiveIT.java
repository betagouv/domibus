package eu.domibus.plugin.ws.webservice;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.ws.AbstractBackendWSIT;
import eu.domibus.plugin.ws.generated.SubmitMessageFault;
import eu.domibus.plugin.ws.generated.body.SubmitRequest;
import eu.domibus.plugin.ws.generated.body.SubmitResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author venugar
 * @since 3.3
 */
public class SubmitMessageCaseInsensitiveIT extends AbstractBackendWSIT {

    @Autowired
    MessageRetentionDefaultService messageRetentionService;

    @BeforeEach
    public void updatePMode(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException {
        uploadPmode(wmRuntimeInfo.getHttpPort());
    }

    /**
     * Sample example of a test for the backend sendMessage service.
     * The message components should be case insensitive from the PMode data
     *
     */
    @Test
    @Disabled("will be fixed by EDELIVERY-11139") //TODO
    public void testSubmitMessageOK() throws SubmitMessageFault {
        String payloadHref = "cid:message";
        SubmitRequest submitRequest = createSubmitRequestWs(payloadHref);

        Pair<String, String> message_id = Pair.of("MESSAGE_ID", UUID.randomUUID() + "@domibus.eu");
        super.prepareSendMessage("validAS4Response.xml", message_id, Pair.of("REF_MESSAGE_ID", UUID.randomUUID() + "@domibus.eu"));

        final eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging messaging = createMessageHeaderWs(payloadHref);
        messaging.getUserMessage().getCollaborationInfo().setAction("TC3Leg1");

        SubmitResponse response = webServicePluginInterface.submitMessage(submitRequest, messaging);

        final List<String> messageID = response.getMessageID();
        assertNotNull(response);
        assertNotNull(messageID);
        assertEquals(1, messageID.size());
        final String messageId = messageID.iterator().next();
        //message will fail as the response message does not contain the right security details(signature, etc)
        waitUntilMessageIsInWaitingForRetry(messageId);

        deleteAllMessages(message_id.getRight(), messageId);
    }
}
