package eu.domibus.common;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Basic implementation of eu.domibus.common.ErrorResult
 *
 * @author Christian Koch, Stefan Mueller
 */
public class ErrorResultImpl implements ErrorResult {

    private MSHRole mshRole;
    private String messageInErrorId;
    private ErrorCode errorCode;

    private String errorCodeAsString;
    private String ErrorDetail;
    private Date timestamp;
    private Date notified;

    //Required for JAXB
    public ErrorResultImpl() {
    }

    @Override
    public MSHRole getMshRole() {
        return mshRole;
    }

    public void setMshRole(MSHRole mshRole) {
        this.mshRole = mshRole;
    }

    @Override
    public String getMessageInErrorId() {
        return messageInErrorId;
    }

    public void setMessageInErrorId(String messageInErrorId) {
        this.messageInErrorId = messageInErrorId;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorCodeAsString() {
        if (StringUtils.isNotBlank(errorCodeAsString)) {
            return errorCodeAsString;
        }
        return ErrorResult.super.getErrorCodeAsString();
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorDetail() {
        return ErrorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        ErrorDetail = errorDetail;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Date getNotified() {
        return notified;
    }

    public void setNotified(Date notified) {
        this.notified = notified;
    }

    public void setErrorCodeAsString(String errorCodeAsString) {
        this.errorCodeAsString = errorCodeAsString;
    }
}
