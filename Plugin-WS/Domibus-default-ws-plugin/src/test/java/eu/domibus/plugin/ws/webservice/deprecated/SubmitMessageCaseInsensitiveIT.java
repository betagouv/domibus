package eu.domibus.plugin.ws.webservice.deprecated;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
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
import java.util.UUID;

/**
 * @author venugar
 * @since 3.3
 * @deprecated to be removed when deprecated endpoint /backend is removed
 */
@Deprecated
@Disabled("EDELIVERY-6896")
public class SubmitMessageCaseInsensitiveIT extends AbstractBackendWSIT {


    @Autowired
    JMSManager jmsManager;

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
    @Disabled("[EDELIVERY-8828] WSPLUGIN: tests for rest methods ignored")
    public void testSubmitMessageOK() throws SubmitMessageFault {
        String payloadHref = "cid:message";
        SubmitRequest submitRequest = createSubmitRequest(payloadHref);

        super.prepareSendMessage("validAS4Response.xml", Pair.of("MESSAGE_ID", UUID.randomUUID()+"@domibus.eu"), Pair.of("REF_MESSAGE_ID", UUID.randomUUID() + "@domibus.eu"));

        final Messaging messaging = createMessageHeader(payloadHref);
        messaging.getUserMessage().getCollaborationInfo().setAction("TC3Leg1");

        SubmitResponse response = backendWebService.submitMessage(submitRequest, messaging);
        verifySendMessageAck(response);
    }
}
