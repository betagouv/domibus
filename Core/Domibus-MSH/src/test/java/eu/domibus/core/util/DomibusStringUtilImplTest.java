package eu.domibus.core.util;

import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.api.util.DomibusStringUtil.VALID_STRING_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Soumya Chandran
 * @since 5.1
 */
@ExtendWith(JMockitExtension.class)
public class DomibusStringUtilImplTest {

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Tested
    private DomibusStringUtilImpl domibusStringUtil;

    @Test
    public void sanitizeFileName() {
        final String fileName = "/test-String&123@97.txt";
        String result = domibusStringUtil.sanitizeFileName(fileName);
        assertEquals(result, "_test-String_123@97.txt");

    }

    @Test
    public void isTrimmedStringLengthLongerThanDefaultMaxLength() {
        String messageId = StringUtils.repeat("X", 256);

        Assertions.assertTrue(domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(messageId));
    }

    @Test
    public void isTrimmedStringLengthLongerThanDefaultMaxLength_255() {
        String messageId = StringUtils.repeat("X", 255);

        Assertions.assertFalse(domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(messageId));
    }

    @Test
    public void isStringLengthLongerThan1024Chars_Valid() {
        String messageId = StringUtils.repeat("X", 1024);

        Assertions.assertFalse(domibusStringUtil.isStringLengthLongerThan1024Chars(messageId));
    }

    @Test
    public void isStringLengthLongerThan1024Chars() {
        String messageId = StringUtils.repeat("X", 1025);

        Assertions.assertTrue(domibusStringUtil.isStringLengthLongerThan1024Chars(messageId));
    }

    @Test
    public void unCamelCase() {
        String camelCaseString = "messageId";

        String unCamelCaseString = domibusStringUtil.unCamelCase(camelCaseString);
        Assertions.assertNotEquals(unCamelCaseString, camelCaseString);
        Assertions.assertEquals(unCamelCaseString, "Message Id");
    }

    @Test
    public void isValidString_nok() {
        new Expectations() {{
            domibusPropertyProvider.getProperty(VALID_STRING_REGEX);
            result = "^[A-Za-z][A-Za-z0-9-_@.]*[A-Za-z0-9]$";
        }};

        String inValidString = "blue+gw";
        boolean result = domibusStringUtil.isValidString(inValidString);
        Assertions.assertFalse(result);
    }

    @Test
    public void isValidString_ok() {
        String validString = "blue_gw";
        new Expectations() {{
            domibusPropertyProvider.getProperty(VALID_STRING_REGEX);
            result = "^[A-Za-z][A-Za-z0-9-_@.]*[A-Za-z0-9]$";
        }};

        boolean result = domibusStringUtil.isValidString(validString);
        Assertions.assertTrue(result);
    }

    @Test
    public void isValidMessageId() {
        String inValidMessageId = "%26%40%b418de6b-007b-11ee-a142-dc8b286a4853@domibus.eu";
        boolean result = domibusStringUtil.isValidMessageId(inValidMessageId);
        Assertions.assertFalse(result);
    }
}

