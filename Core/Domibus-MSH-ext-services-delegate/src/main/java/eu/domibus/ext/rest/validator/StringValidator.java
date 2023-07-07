package eu.domibus.ext.rest.validator;

import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.api.util.DomibusStringUtil;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static eu.domibus.api.util.DomibusStringUtil.VALID_STRING_REGEX;

/**
 * Validator for String parameters
 *
 * @author Soumya Chandran
 * @since 5.2
 */
@Service
public class StringValidator implements
        ConstraintValidator<ValidString, String> {


    private DomibusStringUtil domibusStringUtil;

    public StringValidator() {

    }

    public StringValidator(DomibusStringUtil domibusStringUtil) {
        this.domibusStringUtil = domibusStringUtil;
    }

    @Override
    public void initialize(ValidString constraintAnnotation) {
        this.domibusStringUtil = SpringContextProvider.getApplicationContext().getBean(DomibusStringUtil.class);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(value)) {
            context.buildConstraintViolationWithTemplate("Parameter value is too long (over 255 characters).")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }
        if (!domibusStringUtil.isValidString(value)) {
            context.buildConstraintViolationWithTemplate("Forbidden characters found in the parameter. Doesn't match with pattern " + VALID_STRING_REGEX)
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }
}
