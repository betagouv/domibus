package eu.domibus.backendConnector;

import eu.domibus.common.*;
import eu.domibus.plugin.AbstractBackendConnector;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;

/**
 * @author Ion perpegel
 * @since 5.2
 */
public class BackendConnectorBaseMock extends AbstractBackendConnector {

    private MessageReceiveFailureEvent messageReceiveFailureEvent;
    private PayloadSubmittedEvent payloadSubmittedEvent;
    private PayloadProcessedEvent payloadProcessedEvent;
    private MessageDeletedBatchEvent messageDeletedBatchEvent;
    private DeliverMessageEvent deliverMessageEvent;
    private MessageSendFailedEvent messageSendFailedEvent;
    private MessageSendSuccessEvent messageSendSuccessEvent;

    private MessageResponseSentEvent messageResponseSentEvent;

    public BackendConnectorBaseMock(String name) {
        super(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void messageReceiveFailed(MessageReceiveFailureEvent messageReceiveFailureEvent) {
        this.messageReceiveFailureEvent = messageReceiveFailureEvent;
    }

    @Override
    public void messageResponseSent(final MessageResponseSentEvent messageResponseSentEvent) {
        this.messageResponseSentEvent = messageResponseSentEvent;
    }

    @Override
    public void payloadSubmittedEvent(PayloadSubmittedEvent payloadSubmittedEvent) {
        this.payloadSubmittedEvent = payloadSubmittedEvent;
    }

    @Override
    public void payloadProcessedEvent(PayloadProcessedEvent payloadProcessedEvent) {
        this.payloadProcessedEvent = payloadProcessedEvent;
    }

    @Override
    public void messageDeletedBatchEvent(final MessageDeletedBatchEvent messageDeletedBatchEvent) {
        this.messageDeletedBatchEvent = messageDeletedBatchEvent;
    }

    @Override
    public MessageSubmissionTransformer getMessageSubmissionTransformer() {
        return new MessageSubmissionTransformer() {
            @Override
            public Submission transformToSubmission(Object messageData) {
                Submission submission = new Submission();
                return submission;
            }
        };
    }


    @Override
    public MessageRetrievalTransformer getMessageRetrievalTransformer() {
        return null;
    }

    @Override
    public void deliverMessage(final DeliverMessageEvent deliverMessageEvent) {
        this.deliverMessageEvent = deliverMessageEvent;
    }

    @Override
    public void messageSendFailed(final MessageSendFailedEvent messageSendFailedEvent) {
        this.messageSendFailedEvent = messageSendFailedEvent;
    }

    @Override
    public void messageSendSuccess(final MessageSendSuccessEvent messageSendSuccessEvent) {
        this.messageSendSuccessEvent = messageSendSuccessEvent;
    }

    public void clear() {
        this.messageReceiveFailureEvent = null;
        this.payloadSubmittedEvent = null;
        this.payloadProcessedEvent = null;
        this.messageDeletedBatchEvent = null;
        this.deliverMessageEvent = null;
        this.messageSendFailedEvent = null;
        this.messageSendSuccessEvent = null;
        this.messageResponseSentEvent = null;
    }

    public MessageReceiveFailureEvent getMessageReceiveFailureEvent() {
        return messageReceiveFailureEvent;
    }

    public PayloadSubmittedEvent getPayloadSubmittedEvent() {
        return payloadSubmittedEvent;
    }

    public PayloadProcessedEvent getPayloadProcessedEvent() {
        return payloadProcessedEvent;
    }

    public MessageDeletedBatchEvent getMessageDeletedBatchEvent() {
        return messageDeletedBatchEvent;
    }

    public DeliverMessageEvent getDeliverMessageEvent() {
        return deliverMessageEvent;
    }

    public MessageSendFailedEvent getMessageSendFailedEvent() {
        return messageSendFailedEvent;
    }

    public MessageSendSuccessEvent getMessageSendSuccessEvent() {
        return messageSendSuccessEvent;
    }

    public MessageResponseSentEvent getMessageReceiveReplySentEvent() {
        return messageResponseSentEvent;
    }

}
