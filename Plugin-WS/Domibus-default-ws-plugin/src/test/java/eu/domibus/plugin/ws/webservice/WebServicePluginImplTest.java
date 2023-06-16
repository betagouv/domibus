package eu.domibus.plugin.ws.webservice;

import eu.domibus.api.util.DateUtil;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.*;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogService;
import eu.domibus.plugin.ws.connector.WSPluginImpl;
import eu.domibus.plugin.ws.generated.*;
import eu.domibus.plugin.ws.generated.body.*;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import eu.domibus.plugin.ws.message.WSMessageLogService;
import eu.domibus.plugin.ws.property.WSPluginPropertyManager;
import eu.domibus.messaging.MessageNotFoundException;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.ws.Holder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static eu.domibus.plugin.ws.property.WSPluginPropertyManager.PROP_LIST_REPUSH_MESSAGES_MAXCOUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.0.2
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class WebServicePluginImplTest {

    public static final String MESSAGE_ID = "messageId";

    @Tested
    private WebServiceImpl webServicePlugin;

    @Injectable
    private MessageAcknowledgeExtService messageAcknowledgeExtService;

    @Injectable
    private WSBackendMessageLogService wsBackendMessageLogService;

    @Injectable
    protected WebServiceExceptionFactory webServicePluginExceptionFactory;

    @Injectable
    protected WSMessageLogService wsMessageLogService;

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
    private DateExtService dateUtil;


    @Test
    void validateSubmitRequestWithPayloadsAndBodyLoad(@Injectable SubmitRequest submitRequest,
                                                      @Injectable LargePayloadType payload1,
                                                      @Injectable LargePayloadType payload2,
                                                      @Injectable LargePayloadType bodyLoad) {
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

        Assertions.assertThrows(SubmitMessageFault.class, () -> webServicePlugin.validateSubmitRequest(submitRequest));
    }

    @Test
    void validateSubmitRequestWithMissingPayloadIdForPayload(@Injectable SubmitRequest submitRequest,
                                                             @Injectable LargePayloadType payload1) {
        List<LargePayloadType> payloadList = new ArrayList<>();
        payloadList.add(payload1);

        new Expectations() {{
            submitRequest.getPayload();
            result = payloadList;

            payload1.getPayloadId();
            result = null;
        }};

        Assertions.assertThrows(SubmitMessageFault.class, () -> webServicePlugin.validateSubmitRequest(submitRequest));
    }

    @Test
    void validateSubmitRequestWithPayloadIdAddedForBodyLoad(@Injectable SubmitRequest submitRequest,
                                                            @Injectable LargePayloadType bodyLoad) throws SubmitMessageFault {

        new Expectations() {{
            submitRequest.getBodyload();
            result = bodyLoad;

            bodyLoad.getPayloadId();
            result = "cid:message";
        }};

        Assertions.assertThrows(SubmitMessageFault.class, () -> webServicePlugin.validateSubmitRequest(submitRequest));
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
            assertEquals("-Dom137-- ", messageId, "The message identifier should have been cleaned before retrieving the message");
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

//        webServicePlugin.setLister(lister);
        webServicePlugin.retrieveMessage(retrieveMessageRequest, new Holder<>(retrieveMessageResponse), new Holder<>(ebMSHeaderInfo));

        new Verifications() {{
            String messageId;
            messageExtService.cleanMessageIdentifier(messageId = withCapture());
            assertEquals("-Dom137--", messageId, "The message identifier should have been cleaned before retrieving the message");
        }};
    }

    @Test
    public void cleansTheMessageIdentifierBeforeRetrievingTheStatusOfAMessageByItsIdentifier(
            @Injectable StatusRequestWithAccessPointRole statusRequest,
            @Injectable MessageRetriever messageRetriever) throws StatusFault, MessageNotFoundException {
        new Expectations() {{

            messageExtService.cleanMessageIdentifier(MESSAGE_ID);
            result = MESSAGE_ID;

            statusRequest.getMessageID();
            result = MESSAGE_ID;

            statusRequest.getAccessPointRole();
            result = MshRole.RECEIVING;

            wsPlugin.getMessageRetriever();
            result = messageRetriever;

            messageRetriever.getStatus(MESSAGE_ID, MSHRole.RECEIVING);
            result = MessageStatus.ACKNOWLEDGED;

            messageExtService.isTrimmedStringLengthLongerThanDefaultMaxLength(MESSAGE_ID);
            result = false;

        }};
        webServicePlugin.getStatusWithAccessPointRole(statusRequest);
        new FullVerifications() {
        };
    }

    @Test
    public void validateAccessPointRole(
            @Injectable StatusRequestWithAccessPointRole statusRequest) {

        new Expectations() {
            {
                statusRequest.getAccessPointRole();
                result = null;
            }
        };

        try {
            webServicePlugin.validateAccessPointRole(statusRequest.getAccessPointRole());
            Assertions.fail();
        } catch (StatusFault statusFault) {
            assertEquals("Access point role is invalid", statusFault.getMessage());
        }
    }


    @Test
    public void validateMessageId(
            @Injectable StatusRequestWithAccessPointRole statusRequest) {

        new Expectations() {
            {
                statusRequest.getMessageID();
                result = "";
            }
        };

        try {
            webServicePlugin.validateMessageId(statusRequest.getMessageID());
            Assertions.fail();
        } catch (StatusFault statusFault) {
            assertEquals(statusFault.getMessage(), "Message ID is empty");
        }
    }

    @Test
    public void listPushFailedMessagesEmptyMessageId(@Injectable DomainDTO domainDTO, @Injectable ListPushFailedMessagesRequest listPushFailedMessagesRequest) {

        new Expectations() {
            {
                domainContextExtService.getCurrentDomainSafely();
                result = domainDTO;

                listPushFailedMessagesRequest.getMessageId();
                result = "";
            }
        };

        try {
            webServicePlugin.listPushFailedMessages(listPushFailedMessagesRequest);
            Assertions.fail();
        } catch (ListPushFailedMessagesFault listPushFailedMessagesFault) {
            assertEquals(listPushFailedMessagesFault.getMessage(), "Message ID is empty");
        }
    }

    @Test
    public void rePushFailedMessages(@Injectable DomainDTO domainDTO, @Injectable RePushFailedMessagesRequest rePushFailedMessagesRequest) {
        String messageId = StringUtils.repeat("X", 256);
        List<String> messageIds = new ArrayList<>();
        messageIds.add(messageId);
        new Expectations() {
            {
                domainContextExtService.getCurrentDomainSafely();
                result = domainDTO;

                wsPluginPropertyManager.getKnownIntegerPropertyValue(PROP_LIST_REPUSH_MESSAGES_MAXCOUNT);
                result = 2;

                messageExtService.cleanMessageIdentifier(messageId);
                result = messageId;

                rePushFailedMessagesRequest.getMessageID();
                result = messageIds;
            }
        };

        try {
            webServicePlugin.rePushFailedMessages(rePushFailedMessagesRequest);
            Assertions.fail();
        } catch (RePushFailedMessagesFault rePushFailedMessagesFault) {
            assertEquals(rePushFailedMessagesFault.getMessage(), "Invalid Message Id. ");
        }
    }

}
