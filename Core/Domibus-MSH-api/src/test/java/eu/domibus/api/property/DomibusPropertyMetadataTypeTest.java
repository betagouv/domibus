package eu.domibus.api.property;

import eu.domibus.api.property.validators.DomibusPropertyValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DomibusPropertyMetadataTypeTest {

    @Test
    public void testURIValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.URI.getValidator();

        Assertions.assertTrue(validator.isValid("localhost"));
        Assertions.assertTrue(validator.isValid("urn:oasis:names:tc:ebcore:partyid-type:unregistered"));
        Assertions.assertTrue(validator.isValid("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder"));
        Assertions.assertTrue(validator.isValid("/linux/path/"));
        Assertions.assertTrue(validator.isValid("c:/windows/path with spaces/~!@#$%^&()_+-=[]{};',.`/this_is_a_valid_windows_path"));
        Assertions.assertTrue(validator.isValid("file:///c:/windows/path/"));
        Assertions.assertTrue(validator.isValid("https://some-other-url:1234/url.aspx?param1=val&param2=val2+aaa"));

        Assertions.assertFalse(validator.isValid("this is invalid \r\b"));
        Assertions.assertFalse(validator.isValid("this is invalid <"));
    }

    @Test
    public void testConcurrencyValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.CONCURRENCY.getValidator();

        Assertions.assertTrue(validator.isValid("50"));
        Assertions.assertTrue(validator.isValid("1-10"));

        Assertions.assertFalse(validator.isValid("-1"));
        Assertions.assertFalse(validator.isValid("?"));
    }

    @Test
    public void testClassValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.CLASS.getValidator();

        Assertions.assertTrue(validator.isValid("eu.domibus.core.property.encryption.plugin.PasswordEncryptionExtServiceImplTest"));
        Assertions.assertTrue(validator.isValid("com.sun.xml.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl"));

        Assertions.assertFalse(validator.isValid("?"));
    }

    @Test
    public void testJndiValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.JNDI.getValidator();

        Assertions.assertTrue(validator.isValid("domibus.backend.jms.replyQueue"));
        Assertions.assertTrue(validator.isValid("jms/domibus.backend.jms.replyQueue"));

        Assertions.assertFalse(validator.isValid("aaa>aaa"));
    }

    @Test
    public void testEmailValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.EMAIL.getValidator();

        Assertions.assertTrue(validator.isValid("abc_ABC@domibus-host.com"));
        Assertions.assertTrue(validator.isValid("abc_ABC@domibus-host.com ; second.address@example.org "));
        Assertions.assertFalse(validator.isValid("invalid@@email.com"));
    }

    @Test
    public void testCommaSeparatedListValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.COMMA_SEPARATED_LIST.getValidator();

        Assertions.assertTrue(validator.isValid("domibus-blue, domibus-123"));
        Assertions.assertFalse(validator.isValid("aaa;"));
    }

    @Test
    public void testHyphenedNameValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.HYPHENED_NAME.getValidator();

        Assertions.assertTrue(validator.isValid("bdxr-transport-ebms3-as4-v1p0"));
        Assertions.assertFalse(validator.isValid("abc'abc"));
    }

    @Test
    public void testPositiveDecimalValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.POSITIVE_DECIMAL.getValidator();

        Assertions.assertTrue(validator.isValid("12.55"));
        Assertions.assertFalse(validator.isValid("12.555"));
        Assertions.assertFalse(validator.isValid("-12.55"));
    }

    @Test
    public void testPositiveIntegerValidator() {
        DomibusPropertyValidator validator = DomibusPropertyMetadata.Type.POSITIVE_INTEGER.getValidator();

        Assertions.assertTrue(validator.isValid("0"));
        Assertions.assertTrue(validator.isValid("12555"));
        Assertions.assertFalse(validator.isValid("12.555"));
        Assertions.assertFalse(validator.isValid("-214"));
    }
}
