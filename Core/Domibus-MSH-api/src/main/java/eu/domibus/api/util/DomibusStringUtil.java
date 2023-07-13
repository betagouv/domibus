package eu.domibus.api.util;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN;

/**
 * @author Soumya Chandran
 * @since 5.1
 */
public interface DomibusStringUtil {

    String VALID_STRING_REGEX = "^[a-zA-Z0-9\\.@_-]*$";
    String MESSAGE_ID_PATTERN = DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN;
    String unCamelCase(String str);

    boolean isStringLengthLongerThanDefaultMaxLength(String testString);

    boolean isTrimmedStringLengthLongerThanDefaultMaxLength(String testString);

    boolean isStringLengthLongerThan1024Chars(String testString);

    /**
     * replacing all special characters except [a-zA-Z0-9] and [@.-] with _ from any string
     *
     * @param fileName string to be sanitized by removing special characters
     * @return sanitized fileName
     */
    String sanitizeFileName(String fileName);

    boolean isValidString(String name);

    boolean isValidMessageId(String messageId);

}
