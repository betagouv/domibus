package eu.domibus.web.rest.ro;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.MessageType;
import eu.domibus.api.model.NotificationStatus;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
public class MessageLogRO implements Serializable {

    private String messageId;

    private String conversationId;

    private String fromPartyId;

    private String toPartyId;

    private MessageStatus messageStatus;

    private NotificationStatus notificationStatus;

    private MSHRole mshRole;

    private MessageType messageType;

    private Date deleted;

    private Date received;

    private Date downloaded;

    private int sendAttempts;

    private int sendAttemptsMax;

    private Date nextAttempt;

    private String nextAttemptTimezoneId;

    private int nextAttemptOffsetSeconds;

    private String originalSender;

    private String finalRecipient;

    private String refToMessageId;

    private Date failed;

    private Date restored;

    private Boolean testMessage;

    private boolean messageFragment;

    private boolean sourceMessage;

    private String action;

    private String serviceType;

    private String serviceValue;

    private String pluginType;

    private Long partLength;

    private Boolean canDownloadMessage;

    private Boolean canDownloadEnvelope;

    private Date archived;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getFromPartyId() {
        return fromPartyId;
    }

    public void setFromPartyId(String fromPartyId) {
        this.fromPartyId = fromPartyId;
    }

    public String getToPartyId() {
        return toPartyId;
    }

    public void setToPartyId(String toPartyId) {
        this.toPartyId = toPartyId;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public MSHRole getMshRole() {
        return mshRole;
    }

    public void setMshRole(MSHRole mshRole) {
        this.mshRole = mshRole;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Date getDeleted() {
        return deleted;
    }

    public void setDeleted(Date deleted) {
        this.deleted = deleted;
    }

    public Date getReceived() {
        return received;
    }

    public void setReceived(Date received) {
        this.received = received;
    }

    public Date getDownloaded() { return downloaded; }

    public void setDownloaded(Date downloaded) { this.downloaded = downloaded; }

    public int getSendAttempts() {
        return sendAttempts;
    }

    public void setSendAttempts(int sendAttempts) {
        this.sendAttempts = sendAttempts;
    }

    public int getSendAttemptsMax() {
        return sendAttemptsMax;
    }

    public void setSendAttemptsMax(int sendAttemptsMax) {
        this.sendAttemptsMax = sendAttemptsMax;
    }

    public Date getNextAttempt() {
        return nextAttempt;
    }

    public void setNextAttempt(Date nextAttempt) {
        this.nextAttempt = nextAttempt;
    }

    public String getNextAttemptTimezoneId() {
        return nextAttemptTimezoneId;
    }

    public void setNextAttemptTimezoneId(String nextAttemptTimezoneId) {
        this.nextAttemptTimezoneId = nextAttemptTimezoneId;
    }

    public int getNextAttemptOffsetSeconds() {
        return nextAttemptOffsetSeconds;
    }

    public void setNextAttemptOffsetSeconds(int nextAttemptOffsetSeconds) {
        this.nextAttemptOffsetSeconds = nextAttemptOffsetSeconds;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public void setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
    }

    public String getFinalRecipient() {
        return finalRecipient;
    }

    public void setFinalRecipient(String finalRecipient) {
        this.finalRecipient = finalRecipient;
    }

    public String getRefToMessageId() {
        return refToMessageId;
    }

    public void setRefToMessageId(String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    public Date getFailed() {
        return failed;
    }

    public void setFailed(Date failed) {
        this.failed = failed;
    }

    public Date getRestored() {
        return restored;
    }

    public void setRestored(Date restored) {
        this.restored = restored;
    }

    public Boolean getTestMessage() {
        return testMessage;
    }

    public void setTestMessage(Boolean testMessage) {
        this.testMessage = testMessage;
    }

    public boolean getMessageFragment() {
        return messageFragment;
    }

    public void setMessageFragment(boolean messageFragment) {
        this.messageFragment = messageFragment;
    }

    public boolean getSourceMessage() {
        return sourceMessage;
    }

    public void setSourceMessage(boolean sourceMessage) {
        this.sourceMessage = sourceMessage;
    }

    public String getAction() {
        return action;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getServiceValue() {
        return serviceValue;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setServiceValue(String serviceValue) {
        this.serviceValue = serviceValue;
    }

    public String getPluginType() {
        return pluginType;
    }

    public void setPluginType(String pluginType) {
        this.pluginType = pluginType;
    }

    public Long getPartLength() {
        return partLength;
    }

    public void setPartLength(Long partLength) {
        this.partLength = partLength;
    }

    public Boolean getCanDownloadMessage() {
        return canDownloadMessage;
    }

    public void setCanDownloadMessage(Boolean canDownloadMessage) {
        this.canDownloadMessage = canDownloadMessage;
    }

    public Boolean getCanDownloadEnvelope() {
        return canDownloadEnvelope;
    }

    public void setCanDownloadEnvelope(Boolean canDownloadEnvelope) {
        this.canDownloadEnvelope = canDownloadEnvelope;
    }

    public boolean isSplitAndJoin() {
        return this.messageFragment || this.sourceMessage;
    }
    
    public Date getArchived() {
        return archived;
    }

    public void setArchived(Date archived) {
        this.archived = archived;
    }
}
