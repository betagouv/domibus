package eu.domibus.ext.rest.validator;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.api.util.DomibusStringUtil;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static eu.domibus.api.util.DomibusStringUtil.MESSAGE_ID_PATTERN;

/**
 * Validator for Message Id
 *
 * @author Soumya Chandran
 * @since 5.2
 */
@Service
public class MessageIdValidator implements
        ConstraintValidator<ValidMessageId, String> {

    private DomibusStringUtil domibusStringUtil;

    private DomibusPropertyProvider domibusPropertyProvider;

    public MessageIdValidator() {

    }

    public MessageIdValidator(DomibusStringUtil domibusStringUtil, DomibusPropertyProvider domibusPropertyProvider) {
        this.domibusStringUtil = domibusStringUtil;
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    @Override
    public void initialize(ValidMessageId constraintAnnotation) {
        this.domibusStringUtil = SpringContextProvider.getApplicationContext().getBean(DomibusStringUtil.class);
        this.domibusPropertyProvider = SpringContextProvider.getApplicationContext().getBean(DomibusPropertyProvider.class);
    }

    @Override
    public boolean isValid(String messageId, ConstraintValidatorContext context) {
        if (domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(messageId)) {
            context.buildConstraintViolationWithTemplate("Message Id is too long (over 255 characters).")
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }
        if (!domibusStringUtil.isValidMessageId(messageId)) {
            context.buildConstraintViolationWithTemplate("Forbidden characters found in messageId. It is not conform to the required MessageIdPattern " + domibusPropertyProvider.getProperty(MESSAGE_ID_PATTERN))
                    .addConstraintViolation()
                    .disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }
}
