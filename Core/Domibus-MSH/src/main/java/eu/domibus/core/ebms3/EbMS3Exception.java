package eu.domibus.core.ebms3;

import eu.domibus.api.ebms3.model.Ebms3Description;
import eu.domibus.api.ebms3.model.Ebms3Error;
import eu.domibus.api.model.Description;
import eu.domibus.api.model.MSHRole;
import eu.domibus.common.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.dom.DOMDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.WebFault;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Christian Koch, Stefan Mueller
 * This is the implementation of a ebMS3 Error Message
 */
@WebFault(name = "ebMS3Error")
public class EbMS3Exception extends Exception {

    /**
     * Default locale for error messages
     */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    /**
     * Default ResourceBundle name for error messages
     */
    public static final String RESOURCE_BUNDLE_NAME = "messages.ebms3.codes.MessagesBundle";
    /**
     * Default ResourceBundle for error messages
     */
    public static final ResourceBundle DEFAULT_MESSAGES = ResourceBundle.getBundle(EbMS3Exception.RESOURCE_BUNDLE_NAME, EbMS3Exception.DEFAULT_LOCALE);
    /**
     * Default value for recoverable
     */
    public static final boolean DEFAULT_RECOVERABLE = true;

    private final ErrorCode.EbMS3ErrorCode ebMS3ErrorCode;
    /**
     * "This OPTIONAL attribute provides a short description of the error that can be reported in a log, in order to facilitate readability."
     * (OASIS ebXML Messaging Services Version 3.0: Part 1, Core Features, 1 October 2007)
     */
    private String errorDetail;
    private String refToMessageId;
    private MSHRole mshRole;
    private boolean recoverable = DEFAULT_RECOVERABLE;
    private String signalMessageId;

    protected String origin;
    protected String errorCode;
    protected String severity;
    protected String category;
    protected String shortDescription;

    protected EbMS3Exception(final ErrorCode.EbMS3ErrorCode ebMS3ErrorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.ebMS3ErrorCode = ebMS3ErrorCode;
    }

    public boolean isRecoverable() {
        return this.recoverable;
    }

    public void setRecoverable(final boolean recoverable) {
        this.recoverable = recoverable;
    }

    public Description getDescription() {
        return this.getDescription(EbMS3Exception.DEFAULT_MESSAGES);
    }

    public Description getDescription(final ResourceBundle bundle) {
        final Description description = new Description();
        description.setValue(bundle.getString(this.ebMS3ErrorCode.getCode().name()));
        description.setLang(bundle.getLocale().getLanguage());

        return description;
    }

    public Description getDescription(final Locale locale) {
        return this.getDescription(ResourceBundle.getBundle(EbMS3Exception.RESOURCE_BUNDLE_NAME, locale));
    }

    public String getErrorDetail() {
        return StringUtils.abbreviate(this.errorDetail, 255);
    }

    public void setErrorDetail(final String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public String getOrigin() {
        if (StringUtils.isNotBlank(origin)) {
            return origin;
        }
        if (this.ebMS3ErrorCode != null) {
            return this.ebMS3ErrorCode.getCode().getOrigin();
        }
        return null;
    }

    public ErrorCode.EbMS3ErrorCode getEbMS3ErrorCode() {
        return ebMS3ErrorCode;
    }

    public ErrorCode getErrorCodeObject() {
        if(ebMS3ErrorCode != null) {
            return ebMS3ErrorCode.getCode().getErrorCode();
        }
        return null;
    }

    public String getShortDescription() {
        if (StringUtils.isNotBlank(shortDescription)) {
            return shortDescription;
        }
        if (this.ebMS3ErrorCode != null) {
            return this.ebMS3ErrorCode.getShortDescription();
        }
        return null;
    }

    public String getSeverity() {
        if (StringUtils.isNotBlank(severity)) {
            return severity;
        }
        if (this.ebMS3ErrorCode != null) {
            return this.ebMS3ErrorCode.getSeverity();
        }
        return null;
    }

    public String getCategory() {
        if (StringUtils.isNotBlank(category)) {
            return category;
        }
        if (this.ebMS3ErrorCode != null) {
            return this.ebMS3ErrorCode.getCategory().name();
        }
        return null;
    }

    //this is a hack to avoid a classCastException in @see WebFaultOutInterceptor
    public Source getFaultInfo() {
        Document document = new DOMDocument("Empty_document");
        final Element firstElement = document.createElement("Empty_child");
        document.appendChild(firstElement);
        return new DOMSource(document);
    }

    public Ebms3Error getFaultInfoError() {

        final Ebms3Error ebMS3Error = new Ebms3Error();

        ebMS3Error.setOrigin(getOrigin());
        ebMS3Error.setErrorCode(getErrorCode());
        ebMS3Error.setSeverity(getSeverity());
        ebMS3Error.setErrorDetail((this.errorDetail != null ? getErrorDetail() : ""));
        ebMS3Error.setCategory(getCategory());
        ebMS3Error.setRefToMessageInError(this.refToMessageId);
        ebMS3Error.setShortDescription(this.getShortDescription());

        //we have the long description only for standard ebms3 error codes in MessagesBundle.properties
        if(ebMS3ErrorCode != null) {
            Ebms3Description ebms3Description = new Ebms3Description();
            ebms3Description.setValue(this.getDescription().getValue());
            ebms3Description.setLang(this.getDescription().getLang());
            ebMS3Error.setDescription(ebms3Description);
        }

        return ebMS3Error;
    }

    public String getRefToMessageId() {
        return this.refToMessageId;
    }

    public void setRefToMessageId(final String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    public MSHRole getMshRole() {
        return this.mshRole;
    }

    public void setMshRole(final MSHRole mshRole) {
        this.mshRole = mshRole;
    }

    public String getSignalMessageId() {
        return this.signalMessageId;
    }

    public void setSignalMessageId(final String signalMessageId) {
        this.signalMessageId = signalMessageId;
    }


    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getErrorCode() {
        if (StringUtils.isNotBlank(errorCode)) {
            return errorCode;
        }
        if (this.ebMS3ErrorCode != null) {
            return this.ebMS3ErrorCode.getCode().getErrorCode().getErrorCodeName();
        }
        return null;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
}

