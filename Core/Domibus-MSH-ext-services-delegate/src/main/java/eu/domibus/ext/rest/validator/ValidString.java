package eu.domibus.ext.rest.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * @author Soumya Chandran
 * @since 5.2
 */
@Documented
@Constraint(validatedBy = StringValidator.class)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidString {
    String message() default "Invalid string value.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
