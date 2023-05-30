package eu.domibus.plugin.ws.webservice.deprecated;

import eu.domibus.common.MessageStatus;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.ext.services.*;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.messaging.DuplicateMessageException;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.webService.generated.*;
import eu.domibus.plugin.ws.connector.WSPluginImpl;
import eu.domibus.plugin.ws.property.WSPluginPropertyManager;
import eu.domibus.plugin.ws.message.WSMessageLogDao;
import eu.domibus.plugin.ws.webservice.deprecated.mapper.WSPluginMessagingMapper;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.ws.Holder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 4.0.2
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JMockit.class)
@Deprecated
public class WebServicePluginImplTest {

    public static final String MESSAGE_ID = "messageId";

    @Tested
    private WebServicePluginImpl webServicePlugin;

    @Injectable
    private MessageAcknowledgeExtService messageAcknowledgeExtService;

    @Injectable
    protected WebServicePluginExceptionFactory webServicePluginExceptionFactory;

    @Injectable
    protected WSMessageLogDao wsMessageLogDao;

    @Injectable
    private DomainContextExtService domainContextExtService;

    @Injectable
    protected WSPluginPropertyManager wsPluginPropertyManager;

    @Injectable
    private AuthenticationExtService authenticationExtService;

    @Injectable
    protected MessageExtService messageExtService;

    @Injectable
    private WSPluginImpl wsPlugin;
    @Injectable
    private WSPluginMessagingMapper messagingMapper;


    @Test(expected = SubmitMessageFault.class)
    public void validateSubmitRequestWithPayloadsAndBodyLoad(@Injectable SubmitRequest submitRequest,
                                                             @Injectable LargePayloadType payload1,
                                                             @Injectable LargePayloadType payload2,
                                                             @Injectable LargePayloadType bodyLoad) throws SubmitMessageFault {
        List<LargePayloadType> payloadList = new ArrayList<>();
        payloadList.add(payload1);
        payloadList.add(payload2);

        new Expectations() {{
            submitRequest.getPayload();
            result = payloadList;

            payload1.getPayloadId();
            result = "cid:message1";

            payload2.getPayloadId();
            result = "cid:message2";

            submitRequest.getBodyload();
            result = bodyLoad;

            bodyLoad.getPayloadId();
            result = "null";
        }};

        webServicePlugin.validateSubmitRequest(submitRequest);
    }

    @Test(expected = SubmitMessageFault.class)
    public void validateSubmitRequestWithMissingPayloadIdForPayload(@Injectable SubmitRequest submitRequest,
                                                                    @Injectable LargePayloadType payload1) throws SubmitMessageFault {
        List<LargePayloadType> payloadList = new ArrayList<>();
        payloadList.add(payload1);

        new Expectations() {{
            submitRequest.getPayload();
            result = payloadList;

            payload1.getPayloadId();
            result = null;
        }};

        webServicePlugin.validateSubmitRequest(submitRequest);
    }

    @Test(expected = SubmitMessageFault.class)
    public void validateSubmitRequestWithPayloadIdAddedForBodyLoad(@Injectable SubmitRequest submitRequest,
                                                                   @Injectable LargePayloadType bodyLoad) throws SubmitMessageFault {

        new Expectations() {{
            submitRequest.getBodyload();
            result = bodyLoad;

            bodyLoad.getPayloadId();
            result = "cid:message";
        }};

        webServicePlugin.validateSubmitRequest(submitRequest);
    }

    @Test
    public void test_SubmitMessage_MessageIdHavingEmptySpaces(@Injectable SubmitRequest submitRequest,
                                                              @Injectable RetrieveMessageResponse retrieveMessageResponse,
                                                              @Injectable Messaging ebMSHeaderInfo) throws SubmitMessageFault {
        new Expectations() {{
            ebMSHeaderInfo.getUserMessage().getMessageInfo().getMessageId();
            result = "-Dom137-- ";

        }};

        // backendWebService.retrieveMessage(retrieveMessageRequest, new Holder<RetrieveMessageResponse>(retrieveMessageResponse), new Holder<>(ebMSHeaderInfo));

        webServicePlugin.submitMessage(submitRequest, ebMSHeaderInfo);

        new Verifications() {{
            String messageId;
            messageExtService.cleanMessageIdentifier(messageId = withCapture());
            assertEquals("The message identifier should have been cleaned before retrieving the message", "-Dom137-- ", messageId);
        }};
    }

    @Test
    public void cleansTheMessageIdentifierBeforeRetrievingTheMessageByItsIdentifier(@Injectable RetrieveMessageRequest retrieveMessageRequest,
                                                                                    @Injectable RetrieveMessageResponse retrieveMessageResponse,
                                                                                    @Injectable Messaging ebMSHeaderInfo) throws RetrieveMessageFault {
        new Expectations(webServicePlugin) {{
            retrieveMessageRequest.getMessageID();
            result = "-Dom137--";
        }};

        webServicePlugin.retrieveMessage(retrieveMessageRequest, new Holder<>(retrieveMessageResponse), new Holder<>(ebMSHeaderInfo));

        new Verifications() {{
            String messageId;
            messageExtService.cleanMessageIdentifier(messageId = withCapture());
            assertEquals("The message identifier should have been cleaned before retrieving the message", "-Dom137--", messageId);
        }};
    }

    @Test
    public void cleansTheMessageIdentifierBeforeRetrievingTheStatusOfAMessageByItsIdentifier(
            @Injectable StatusRequest statusRequest,
            @Injectable MessageRetriever messageRetriever) throws StatusFault, MessageNotFoundException, DuplicateMessageException {
        new Expectations() {{
            statusRequest.getMessageID();
            result = MESSAGE_ID;
            times = 2;

            messageExtService.cleanMessageIdentifier(MESSAGE_ID);
            result = MESSAGE_ID;
            times = 1;

            wsPlugin.getMessageRetriever();
            result = messageRetriever;
            times = 1;

            messageRetriever.getStatus(MESSAGE_ID);
            result = MessageStatus.ACKNOWLEDGED;
            times = 1;
        }};

        webServicePlugin.getStatus(statusRequest);

        new FullVerifications() {};
    }

}
