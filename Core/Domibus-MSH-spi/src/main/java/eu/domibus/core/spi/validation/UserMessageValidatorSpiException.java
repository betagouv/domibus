package eu.domibus.core.spi.validation;

/**
 * @author Cosmin Baciu
 * @since 5.0
 *
 * Exception raised in case the UserMessage validation does not pass
 */
public class UserMessageValidatorSpiException extends RuntimeException {

    /**
     * We use a string so that we can throw custom ebMS3 exceptions
     */
    protected Ebms3ErrorSpi ebms3ErrorCode;

    public UserMessageValidatorSpiException() {
    }

    public UserMessageValidatorSpiException(String message) {
        super(message);
    }

    public UserMessageValidatorSpiException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserMessageValidatorSpiException(Throwable cause) {
        super(cause);
    }

    public Ebms3ErrorSpi getEbms3ErrorCode() {
        return ebms3ErrorCode;
    }

    public void setEbms3ErrorCode(Ebms3ErrorSpi ebms3ErrorCode) {
        this.ebms3ErrorCode = ebms3ErrorCode;
    }
}
