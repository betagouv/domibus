package eu.domibus.plugin.ws.backend;

import eu.domibus.common.MessageStatus;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.ws.AbstractWSEntity;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.Date;

/**
 * @author François Gautier
 * @since 5.0
 */
@Entity
@Table(name = "WS_PLUGIN_TB_BACKEND_MSG_LOG")
@NamedQuery(name = "WSBackendMessageLogEntity.findByMessageId",
        query = "select wsBackendMessageLogEntity " +
                "from WSBackendMessageLogEntity wsBackendMessageLogEntity " +
                "where wsBackendMessageLogEntity.messageId=:MESSAGE_ID")
@NamedQuery(name = "WSBackendMessageLogEntity.findRetryMessages",
        query = "select backendMessage " +
                "from WSBackendMessageLogEntity backendMessage " +
                "where backendMessage.backendMessageStatus = :BACKEND_MESSAGE_STATUS " +
                "and backendMessage.nextAttempt < :CURRENT_TIMESTAMP " +
                "and 1 <= backendMessage.sendAttempts " +
                "and backendMessage.sendAttempts <= backendMessage.sendAttemptsMax " +
                "and (backendMessage.scheduled is null or backendMessage.scheduled=false)")
public class WSBackendMessageLogEntity extends AbstractWSEntity {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WSBackendMessageLogEntity.class);

    @Column(name = "CREATION_TIME", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;

    @Column(name = "MODIFICATION_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modificationTime;

    @Column(name = "CREATED_BY", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "MODIFIED_BY")
    private String modifiedBy;

    @Column(name = "MESSAGE_ID", nullable = false)
    private String messageId;

    @Column(name = "MESSAGE_IDS", nullable = true)
    private String messageIds;

    @Column(name = "MESSAGE_ENTITY_ID", nullable = false)
    private long messageEntityId;

    @Column(name = "FINAL_RECIPIENT")
    private String finalRecipient;

    @Column(name = "ORIGINAL_SENDER")
    private String originalSender;

    @Column(name = "BACKEND_MESSAGE_STATUS")
    @Enumerated(EnumType.STRING)
    private WSBackendMessageStatus backendMessageStatus;

    @Column(name = "MESSAGE_STATUS")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "BACKEND_MESSAGE_TYPE")
    private WSBackendMessageType type;

    @Column(name = "RULE_NAME")
    private String ruleName;

    @Column(name = "SENT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sent;

    @Column(name = "FAILED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date failed;

    @Column(name = "SEND_ATTEMPTS")
    private int sendAttempts;

    @Column(name = "SEND_ATTEMPTS_MAX")
    private int sendAttemptsMax;

    @Column(name = "NEXT_ATTEMPT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextAttempt;

    @Column(name = "SCHEDULED")
    protected Boolean scheduled;

    public WSBackendMessageLogEntity() {
        String user = LOG.getMDC(DomibusLogger.MDC_USER);
        if (StringUtils.isBlank(user)) {
            user = "wsplugin_default";
        }
        setCreatedBy(user);
        setSent(new Date());
        setCreationTime(new Date());
        setModificationTime(new Date());
        setSendAttempts(0);
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getMessageId() {
        if (type == WSBackendMessageType.DELETED_BATCH) {
            return messageIds;
        }
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(String messageIds) {
        this.messageIds = messageIds;
    }

    public long getMessageEntityId() {
        return messageEntityId;
    }

    public void setMessageEntityId(long messageEntityId) {
        this.messageEntityId = messageEntityId;
    }

    public String getFinalRecipient() {
        return finalRecipient;
    }

    public void setFinalRecipient(String finalRecipient) {
        this.finalRecipient = finalRecipient;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public WSBackendMessageLogEntity setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
        return this;
    }

    public WSBackendMessageStatus getBackendMessageStatus() {
        return backendMessageStatus;
    }

    public void setBackendMessageStatus(WSBackendMessageStatus backendMessageStatus) {
        this.backendMessageStatus = backendMessageStatus;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public WSBackendMessageType getType() {
        return type;
    }

    public void setType(WSBackendMessageType type) {
        this.type = type;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String endpoint) {
        this.ruleName = endpoint;
    }

    public Date getSent() {
        return sent;
    }

    public void setSent(Date sent) {
        this.sent = sent;
    }

    public Date getFailed() {
        return failed;
    }

    public void setFailed(Date failed) {
        this.failed = failed;
    }

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

    public Boolean getScheduled() {
        return scheduled;
    }

    public void setScheduled(Boolean scheduled) {
        this.scheduled = scheduled;
    }

    @Override
    public String toString() {
        return "WSBackendMessageLogEntity{" +
                "creationTime=" + creationTime +
                ", modificationTime=" + modificationTime +
                ", createdBy='" + createdBy + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", messageId='" + messageId + '\'' +
                ", messageEntityId='" + messageEntityId + '\'' +
                ", finalRecipient='" + finalRecipient + '\'' +
                ", originalSender='" + originalSender + '\'' +
                ", backendMessageStatus=" + backendMessageStatus +
                ", messageStatus=" + messageStatus +
                ", type=" + type +
                ", ruleName='" + ruleName + '\'' +
                ", sent=" + sent +
                ", failed=" + failed +
                ", sendAttempts=" + sendAttempts +
                ", sendAttemptsMax=" + sendAttemptsMax +
                ", nextAttempt=" + nextAttempt +
                ", scheduled=" + scheduled +
                "} " + super.toString();
    }
}
