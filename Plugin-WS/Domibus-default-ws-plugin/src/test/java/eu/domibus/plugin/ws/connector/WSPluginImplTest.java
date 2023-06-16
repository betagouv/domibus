package eu.domibus.plugin.ws.connector;

import eu.domibus.common.*;
import eu.domibus.ext.services.*;
import eu.domibus.plugin.handler.MessagePuller;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.plugin.ws.initialize.WSPluginInitializer;
import eu.domibus.plugin.ws.message.WSMessageLogService;
import eu.domibus.plugin.ws.backend.dispatch.WSPluginBackendService;
import eu.domibus.plugin.ws.message.WSMessageLogEntity;
import eu.domibus.plugin.ws.property.WSPluginPropertyManager;
import eu.domibus.plugin.ws.webservice.StubDtoTransformer;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static eu.domibus.plugin.ws.backend.WSBackendMessageType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.domibus.plugin.ws.backend.reliability.queue.*;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class WSPluginImplTest {

    public static final String MESSAGE_ID = "messageId";

    @Tested
    private WSPluginImpl wsPlugin;

    @Injectable
    private StubDtoTransformer defaultTransformer;

    @Injectable
    private WSMessageLogService wsMessageLogService;

    @Injectable
    private WSPluginBackendService wsPluginBackendService;

    /**
     * {@link eu.domibus.plugin.AbstractBackendConnector} dependencies
     **/
    @Injectable
    protected MessageRetriever messageRetriever;

    @Injectable
    protected MessageSubmitter messageSubmitter;

    @Injectable
    protected MessagePuller messagePuller;

    @Injectable
    protected MessageExtService messageExtService;

    @Injectable
    MessageRetrieverExtService messageRetrieverExtService;

    @Injectable
    MessageSubmitterExtService messageSubmitterExtService;

    @Injectable
    MessagePullerExtService messagePullerExtService;

    @Injectable
    WSPluginPropertyManager wsPluginPropertyManager;

    @Injectable
    BackendConnectorProviderExtService backendConnectorProviderExtService;

    @Injectable
    DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    DomainContextExtService domainContextExtService;

    @Injectable
    DomainExtService domainExtService;

    @Injectable
    WSSendMessageListenerContainer wsSendMessageListenerContainer;

    @Injectable
    WSPluginInitializer wsPluginInitializer;

    @Test
    public void deliverMessage(@Injectable DeliverMessageEvent deliverMessageEvent,
                               @Injectable WSMessageLogEntity wsMessageLogEntity) {
        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();

            deliverMessageEvent.getMessageId();
            result = MESSAGE_ID;
        }};

        wsPlugin.deliverMessage(deliverMessageEvent);

        new Verifications() {{
            wsMessageLogService.create(withAny(wsMessageLogEntity));
            times = 1;

            //wsPluginBackendService.send(MESSAGE_ID, wsPluginBackendService.userMessageExtService.getFinalRecipient(MESSAGE_ID), wsPluginBackendService.userMessageExtService.getOriginalSender(MESSAGE_ID), SUBMIT_MESSAGE, RECEIVE_SUCCESS);
            times = 1;
        }};
    }

    @Test
    public void sendSuccess(@Injectable MessageSendSuccessEvent event) {
        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();

            event.getMessageId();
            result = MESSAGE_ID;
        }};
        wsPlugin.messageSendSuccess(event);

        new FullVerifications() {{
            wsPluginBackendService.send(event, SEND_SUCCESS);
            times = 1;
        }};
    }

    @Test
    public void messageReceiveFailed(@Injectable MessageReceiveFailureEvent event) {
        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();
        }};

        wsPlugin.messageReceiveFailed(event);

        new Verifications() {{
            wsPluginBackendService.send(event, RECEIVE_FAIL);
            times = 1;
        }};
    }

    @Test
    public void messageSendFailed(@Injectable MessageSendFailedEvent event) {
        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();
        }};

        wsPlugin.messageSendFailed(event);

        new Verifications() {{
            wsPluginBackendService.send(event, SEND_FAILURE);
            times = 1;
        }};
    }

    @Test
    public void messageStatusChanged(@Injectable MessageStatusChangeEvent event) {
        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();
        }};

        wsPlugin.messageStatusChanged(event);

        new Verifications() {{
            wsPluginBackendService.send(event, MESSAGE_STATUS_CHANGE);
            times = 1;
        }};
    }

    @Test
    public void messageDeletedBatchEvent(@Injectable MessageDeletedBatchEvent event) {
        List<String> messageIds = new ArrayList<>();

        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();
        }};

        wsPlugin.messageDeletedBatchEvent(event);

        new Verifications() {{
            wsMessageLogService.deleteByMessageIds(messageIds);
            times = 1;

            wsPluginBackendService.send(event, DELETED_BATCH);
            times = 1;
        }};
    }

    @Test
    public void messageDeletedEvent(@Injectable MessageDeletedEvent event) {
        new Expectations(wsPlugin) {{
            wsPlugin.checkEnabled();

            event.getMessageId();
            result = MESSAGE_ID;
        }};

        wsPlugin.messageDeletedEvent(event);

        new FullVerifications() {{
            wsMessageLogService.deleteByMessageId(MESSAGE_ID);
            times = 1;

            wsPluginBackendService.send(event, DELETED);
            times = 1;
        }};
    }

    @Test
    public void getMessageSubmissionTransformer() {
        assertEquals(defaultTransformer, wsPlugin.getMessageSubmissionTransformer());
    }

    @Test
    public void getMessageRetrievalTransformer() {
        assertEquals(defaultTransformer, wsPlugin.getMessageRetrievalTransformer());
    }
}
