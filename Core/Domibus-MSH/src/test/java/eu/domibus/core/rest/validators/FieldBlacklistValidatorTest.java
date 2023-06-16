package eu.domibus.core.rest.validators;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.validators.CustomWhiteListed;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;
import java.lang.annotation.Annotation;

public class FieldBlacklistValidatorTest {

    @Tested
    FieldBlacklistValidator fieldBlacklistValidator;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void shouldValidateWhenBlacklistIsDefined() {
        new Expectations(fieldBlacklistValidator) {{
            domibusPropertyProvider.getProperty(FieldBlacklistValidator.BLACKLIST_PROPERTY);
            result = "%'\\/";
        }};

        fieldBlacklistValidator.initialize(null);

        String validValue = "abc.";
        String invalidValue = "abc%";
        String emptyValue = "";

        boolean actualValid = fieldBlacklistValidator.isValid(validValue);
        boolean actualInvalid = fieldBlacklistValidator.isValid(invalidValue);
        boolean emptyIsValid = fieldBlacklistValidator.isValid(emptyValue);

        Assertions.assertEquals(true, actualValid);
        Assertions.assertEquals(false, actualInvalid);
        Assertions.assertEquals(true, emptyIsValid);
    }

    @Test
    public void shouldValidateWhenBlacklistIsEmpty() {
        new Expectations(fieldBlacklistValidator) {{
            domibusPropertyProvider.getProperty(FieldBlacklistValidator.BLACKLIST_PROPERTY);
            result = "";
        }};

        fieldBlacklistValidator.initialize(null);

        String invalidValue = "abc%";
        boolean result = fieldBlacklistValidator.isValid(invalidValue, (CustomWhiteListed) null);

        Assertions.assertEquals(true, result);
    }

    @Test
    public void shouldThrowWhenInvalid() {
        new Expectations(fieldBlacklistValidator) {{
            domibusPropertyProvider.getProperty(FieldBlacklistValidator.BLACKLIST_PROPERTY);
            result = "%'\\/";
        }};

        fieldBlacklistValidator.init();

        String validValue = "abc.";
        String invalidValue = "abc%";

        try {
            fieldBlacklistValidator.validate(validValue);
        } catch (IllegalArgumentException ex) {
            Assertions.fail("Should not throw for valid values");
        }
        try {
            fieldBlacklistValidator.validate(invalidValue);
            Assertions.fail("Should throw for invalid values");
        } catch (ValidationException ex) {
        }
    }

    @Test
    public void isWhiteListValid() {
        new Expectations(fieldBlacklistValidator) {{
            domibusPropertyProvider.getProperty(FieldBlacklistValidator.WHITELIST_PROPERTY);
            result = "^[\\w\\-\\.: @]*$";
        }};

        CustomWhiteListed customChars = new CustomWhiteListed() {
            @Override
            public String permitted() {
                return "%";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CustomWhiteListed.class;
            }
        };

        fieldBlacklistValidator.initialize(null);

        String validValue = "abc.";
        String invalidValue = "abc%";
        String emptyValue = "";

        boolean actualValid = fieldBlacklistValidator.isWhiteListValid(validValue, null);
        boolean actualInvalid = fieldBlacklistValidator.isWhiteListValid(invalidValue, customChars);
        boolean emptyIsValid = fieldBlacklistValidator.isWhiteListValid(emptyValue, null);

        Assertions.assertEquals(true, actualValid);
        Assertions.assertEquals(true, actualInvalid);
        Assertions.assertEquals(true, emptyIsValid);
    }
}
