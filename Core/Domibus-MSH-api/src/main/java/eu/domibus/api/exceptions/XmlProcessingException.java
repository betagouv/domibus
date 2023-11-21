package eu.domibus.api.exceptions;

/**
 * @author Lucian Furca
 * @since 5.2
 * <p>
 * Class that encapsulates information about an XML parsing exception
 */
public class XmlProcessingException extends DomibusCoreException {

    public XmlProcessingException(String message) {
        super(DomibusCoreErrorCode.DOM_012, message);
    }

    public XmlProcessingException(String message, Throwable cause) {
        super(DomibusCoreErrorCode.DOM_012, message, cause);
    }
}