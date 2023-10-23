package eu.domibus.common;

/**
 * The error details from the Signal message eg
 * <pre>
 * <eb:SignalMessage>
 *      <eb:Error category="CONTENT" errorCode="EBMS:0003" origin="ebMS" refToMessageInError="25273934-0dc1-11ed-b6b8-06668988840f@domibus.eu" severity="failure" shortDescription="ValueInconsistent">
 *          <eb:ErrorDetail>This contains the error details</eb:ErrorDetail>
 *      </eb:Error>
 * </eb:SignalMessage>
 * </pre>
 */
public class Ebms3ErrorExt {

    protected String origin;
    protected String errorCode;
    protected String severity;
    protected String errorDetail;
    protected String category;
    protected String shortDescription;

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
