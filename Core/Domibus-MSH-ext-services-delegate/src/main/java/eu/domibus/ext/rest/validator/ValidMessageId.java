package eu.domibus.ext.rest.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * @author Soumya Chandran
 * @since 5.2
 */
@Documented
@Constraint(validatedBy = MessageIdValidator.class)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMessageId {
    String message() default "Invalid message id.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
