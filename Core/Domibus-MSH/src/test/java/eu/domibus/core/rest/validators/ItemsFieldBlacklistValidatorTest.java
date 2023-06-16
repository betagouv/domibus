package eu.domibus.core.rest.validators;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.web.rest.validators.ItemsWhiteListed;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ItemsFieldBlacklistValidatorTest {
    @Tested
    ItemsBlacklistValidator blacklistValidator;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void testIsValid() {
        new Expectations(blacklistValidator) {{
            domibusPropertyProvider.getProperty(FieldBlacklistValidator.BLACKLIST_PROPERTY);
            result = "%'\\/";
        }};

        blacklistValidator.init();

        String[] validValue = new String[]{"", "valid value", "also invalid value"};
        String[] invalidValue = new String[]{"", "valid value", "invalid value%"};
        String[] emptyValue = new String[]{};

        boolean actualValid = blacklistValidator.isValid(validValue);
        boolean actualInvalid = blacklistValidator.isValid(invalidValue);
        boolean emptyIsValid = blacklistValidator.isValid(emptyValue);

        Assertions.assertEquals(true, actualValid);
        Assertions.assertEquals(false, actualInvalid);
        Assertions.assertEquals(true, emptyIsValid);
    }

    @Test
    public void testGetErrorMessage() {
        String actual = blacklistValidator.getErrorMessage();
        Assertions.assertEquals(ItemsWhiteListed.MESSAGE, actual);
    }

}
