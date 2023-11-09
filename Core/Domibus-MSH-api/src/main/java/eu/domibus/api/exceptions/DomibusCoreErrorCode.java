package eu.domibus.api.exceptions;

/**
 * Enum for Domibus core errors.
 *
 * @author Federico Martini
 * @since 3.3
 */
public enum DomibusCoreErrorCode {

    /**
     * Generic error
     */
    DOM_001("001"),

    /**
     * Authentication or Authorization error
     */
    DOM_002("002"),
    /**
     * Invalid pmode configuration
     */
    DOM_003("003"),

    /**
     * Problem with Raw message when trying to handle non repudiation. (Pull)
     */
    DOM_004("004"),
    /**
     * Certificate related exception.
     */
    DOM_005("005"),
    /**
     * Proxy related exception.
     */
    DOM_006("006"),
    /**
     * Invalid message exception
     */
    DOM_007("007"),
    /**
     * Convert exception
     */
    DOM_008("008"),
    /**
     * Not found exception
     */
    DOM_009("009"),
    /**
     * Party not reachable exception
     */
    DOM_010("010"),
    /**
     * Duplicate found exception
     */
    DOM_011("011"),
    /**
     * Xml processing exception
     */
    DOM_012("012"),
    /**
     * Security profile exception
     */
    DOM_013("013"),
    ;

    private final String errorCode;

    DomibusCoreErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
