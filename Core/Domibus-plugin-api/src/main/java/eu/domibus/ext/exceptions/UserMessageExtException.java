package eu.domibus.ext.exceptions;

import eu.domibus.common.Ebms3ErrorExt;

/**
 * Raised in case an exception occurs when dealing with User Messages. Set the ebmsError to throw a custom ebms3 error.
 *
 * @author Tiago Miguel
 * @since 4.0
 */
public class UserMessageExtException extends DomibusServiceExtException {

    /**
     * The details of the ebms3 error
     */
    protected Ebms3ErrorExt ebmsError;

    /**
     * Constructs a new instance with a specific error code and message.
     *
     * @param errorCode a DomibusErrorCode
     * @param message the message detail
     */
    public UserMessageExtException(DomibusErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a new instance with a specific error, message and cause.
     *
     * @param errorCode a DomibusError
     * @param message the message detail
     * @param throwable the cause of the exception
     */
    public UserMessageExtException(DomibusErrorCode errorCode, String message, Throwable throwable) {
        super(errorCode, message, throwable);
    }

    /**
     * Constructs a new instance with a specific cause.
     *
     * @param cause the cause of the exception
     */
    public UserMessageExtException(Throwable cause) {
        super(DomibusErrorCode.DOM_001, cause.getMessage(), cause);
    }

    public Ebms3ErrorExt getEbmsError() {
        return ebmsError;
    }

    public void setEbmsError(Ebms3ErrorExt ebmsError) {
        this.ebmsError = ebmsError;
    }
}
