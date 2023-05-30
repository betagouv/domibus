package eu.domibus.archive.client.webhook.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
public class BatchNotification {

    String batchId;
    BatchRequestType requestType;
    BatchStatusType status;
    LocalDateTime timestamp;
    LocalDateTime messageStartDate;
    LocalDateTime messageEndDate;
    List<String> messages;
    @Deprecated
    String errorCode;
    @Deprecated
    String errorDescription;
    String code;
    String message;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public BatchRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(BatchRequestType requestType) {
        this.requestType = requestType;
    }

    public BatchStatusType getStatus() {
        return status;
    }

    public void setStatus(BatchStatusType status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getMessageStartDate() {
        return messageStartDate;
    }

    public void setMessageStartDate(LocalDateTime messageStartDate) {
        this.messageStartDate = messageStartDate;
    }

    public LocalDateTime getMessageEndDate() {
        return messageEndDate;
    }

    public void setMessageEndDate(LocalDateTime messageEndDate) {
        this.messageEndDate = messageEndDate;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    @Deprecated
    public String getErrorCode() {
        return errorCode;
    }

    @Deprecated
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Deprecated
    public String getErrorDescription() {
        return errorDescription;
    }

    @Deprecated
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
