package eu.domibus.core.util;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DomibusStringUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static eu.domibus.logging.DomibusMessageCode.VALUE_DO_NOT_CONFORM_TO_MESSAGEID_PATTERN;
import static eu.domibus.logging.DomibusMessageCode.VALUE_LONGER_THAN_DEFAULT_STRING_LENGTH;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.tika.utils.StringUtils.isBlank;

/**
 * @author Soumya Chandran
 * @since 5.1
 */
@Component
public class DomibusStringUtilImpl implements DomibusStringUtil {

    public static final String ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH = " is too long (over 255 characters).";
    public static final String ERROR_MSG_STRING_LONGER_THAN_STRING_LENGTH_1024 = " is too long (over 1024 characters).";
    public static final int DEFAULT_MAX_STRING_LENGTH = 255;
    public static final int MAX_STRING_LENGTH_1024 = 1024;
    public static final String STRING_SANITIZE_REGEX = "[^\\w@.-]";
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusStringUtilImpl.class);

    protected final  DomibusPropertyProvider domibusPropertyProvider;

    public DomibusStringUtilImpl(DomibusPropertyProvider domibusPropertyProvider) {
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    @Override
    public String unCamelCase(String str) {
        String result = str.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    @Override
    public boolean isStringLengthLongerThanDefaultMaxLength(String testString) {
        return ((testString != null) && (StringUtils.length(testString) > DEFAULT_MAX_STRING_LENGTH));
    }

    @Override
    public boolean isTrimmedStringLengthLongerThanDefaultMaxLength(String testString) {
        return isStringLengthLongerThanDefaultMaxLength(trim(testString));
    }

    @Override
    public boolean isStringLengthLongerThan1024Chars(String testString) {
        return (StringUtils.length(testString) > MAX_STRING_LENGTH_1024);
    }

    @Override
    public String sanitizeFileName(String fileName) {
        return fileName.replaceAll(STRING_SANITIZE_REGEX, "_");
    }


    @Override
    public boolean isValidString(String name) {
        String validStringPattern = domibusPropertyProvider.getProperty(VALID_STRING_REGEX);
        LOG.debug("validStringPattern read from file is [{}]", validStringPattern);
        String trimmedName = StringUtils.trim(name);
        if (!trimmedName.matches(validStringPattern)) {
            return false;
        }
        return true;
    }

    public boolean isValidMessageId(final String messageId) {
        if (isBlank(messageId)) {
            LOG.debug("Message id is empty: validation skipped");
            return false;
        }

        if (isTrimmedStringLengthLongerThanDefaultMaxLength(messageId)) {
            LOG.businessError(VALUE_LONGER_THAN_DEFAULT_STRING_LENGTH, "MessageId", messageId);
            return false;
        }

       return validateMessageIdPattern(messageId, "eb:Messaging/eb:UserMessage/eb:MessageInfo/eb:MessageId");
    }


    protected boolean validateMessageIdPattern(String messageId, String elementType) {
        String messageIdPattern = domibusPropertyProvider.getProperty(MESSAGE_ID_PATTERN);
        LOG.debug("MessageIdPattern read from file is [{}]", messageIdPattern);

        if (isBlank(messageIdPattern)) {
            LOG.debug("No messageIdPattern found.");
            return false;
        }

        if (!messageId.matches(messageIdPattern)) {
            LOG.businessError(VALUE_DO_NOT_CONFORM_TO_MESSAGEID_PATTERN, elementType, messageIdPattern, messageId);
            return false;
        }
        return true;
    }
}
