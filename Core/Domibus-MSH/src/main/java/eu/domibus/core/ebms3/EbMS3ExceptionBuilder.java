package eu.domibus.core.ebms3;

import eu.domibus.api.model.MSHRole;
import eu.domibus.common.ErrorCode;

import static eu.domibus.core.ebms3.EbMS3Exception.DEFAULT_RECOVERABLE;

/**
 * @author François Gautier
 * @since 4.2
 */
public class EbMS3ExceptionBuilder {

    private ErrorCode.EbMS3ErrorCode ebMS3ErrorCode;
    private Throwable cause;
    private String message;
    private String refToMessageId;
    private MSHRole mshRole;
    private boolean recoverable = DEFAULT_RECOVERABLE;
    private String signalMessageId;

    protected String origin;
    protected String errorCode;
    protected String severity;
    protected String category;
    protected String shortDescription;

    public static EbMS3ExceptionBuilder getInstance() {
        return new EbMS3ExceptionBuilder();
    }

    public EbMS3Exception build() {
        EbMS3Exception ebMS3Exception = new EbMS3Exception(ebMS3ErrorCode, message, cause);
        ebMS3Exception.setErrorDetail(message);
        ebMS3Exception.setRefToMessageId(refToMessageId);
        ebMS3Exception.setMshRole(mshRole);
        ebMS3Exception.setRecoverable(recoverable);
        ebMS3Exception.setSignalMessageId(signalMessageId);

        ebMS3Exception.setOrigin(origin);
        ebMS3Exception.setErrorCode(errorCode);
        ebMS3Exception.setSeverity(severity);
        ebMS3Exception.setCategory(category);
        ebMS3Exception.setShortDescription(shortDescription);

        return ebMS3Exception;
    }

    public Throwable getCause() {
        return cause;
    }

    public EbMS3ExceptionBuilder cause(Throwable cause) {
        this.cause = cause;
        return this;
    }

    public ErrorCode.EbMS3ErrorCode getEbMS3ErrorCode() {
        return ebMS3ErrorCode;
    }

    public EbMS3ExceptionBuilder ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode ebMS3ErrorCode) {
        this.ebMS3ErrorCode = ebMS3ErrorCode;
        return this;
    }

    public EbMS3ExceptionBuilder message(String message) {
        this.message = message;
        return this;
    }

    public EbMS3ExceptionBuilder origin(String origin) {
        this.origin = origin;
        return this;
    }

    public EbMS3ExceptionBuilder errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public EbMS3ExceptionBuilder severity(String severity) {
        this.severity = severity;
        return this;
    }

    public EbMS3ExceptionBuilder shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }

    public EbMS3ExceptionBuilder category(String category) {
        this.category = category;
        return this;
    }

    public String getRefToMessageId() {
        return refToMessageId;
    }

    public EbMS3ExceptionBuilder refToMessageId(String refToMessageId) {
        this.refToMessageId = refToMessageId;
        return this;
    }

    public MSHRole getMshRole() {
        return mshRole;
    }

    public EbMS3ExceptionBuilder mshRole(MSHRole mshRole) {
        this.mshRole = mshRole;
        return this;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public EbMS3ExceptionBuilder recoverable(boolean recoverable) {
        this.recoverable = recoverable;
        return this;
    }

    public String getSignalMessageId() {
        return signalMessageId;
    }

    public EbMS3ExceptionBuilder signalMessageId(String signalMessageId) {
        this.signalMessageId = signalMessageId;
        return this;
    }

    public String getMessage() {
        return message;
    }
}
