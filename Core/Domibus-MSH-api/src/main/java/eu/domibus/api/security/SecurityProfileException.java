package eu.domibus.api.security;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;

/**
 * @author Lucian Furca
 * @since 5.2
 * <p>
 * Class that encapsulates information about a Security Profile exception
 */
public class SecurityProfileException extends DomibusCoreException {

    public SecurityProfileException(String message) {
        super(DomibusCoreErrorCode.DOM_013, message);
    }

    public SecurityProfileException(String message, Throwable cause) {
        super(DomibusCoreErrorCode.DOM_013, message, cause);
    }
}