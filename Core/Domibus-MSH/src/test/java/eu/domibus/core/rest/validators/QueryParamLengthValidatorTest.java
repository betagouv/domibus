package eu.domibus.core.rest.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;
import java.util.HashMap;

/**
 * @author François Gautier
 * @since 5.0
 */
public class QueryParamLengthValidatorTest {

    private QueryParamLengthValidator queryParamLengthValidator;
    private HashMap<String, String[]> queryParams;

    @BeforeEach
    public void setUp() throws Exception {
        queryParamLengthValidator = new QueryParamLengthValidator();
        queryParams = new HashMap<>();

    }

    @Test
    public void validateEmpty() {
        queryParams.put("empty", new String[]{});
        queryParamLengthValidator.validate(queryParams);
    }

    @Test
    public void validateBlank() {
        queryParams.put("empty", new String[]{""});
        queryParamLengthValidator.validate(queryParams);
    }

    @Test
    public void validateTooLong() {
        queryParams.put("empty", new String[]{"1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"});

        try {
            queryParamLengthValidator.validate(queryParams);
            Assertions.fail();
        } catch (ValidationException e) {
            //ok
        }
    }
}
