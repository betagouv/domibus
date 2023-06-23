package eu.domibus.web.rest.validators;

import eu.domibus.core.rest.validators.ObjectPropertiesMapBlacklistValidator;
import eu.domibus.core.rest.validators.QueryParamLengthValidator;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(JMockitExtension.class)
public class RestQueryParamsValidationInterceptorTest {
    @Tested
    RestQueryParamsValidationInterceptor restQueryParamsValidationInterceptor;

    @Injectable
    ObjectPropertiesMapBlacklistValidator blacklistValidator;
    @Injectable
    QueryParamLengthValidator queryParamLengthValidator;

    @Test
    public void handleQueryParamsTestValid() {
        String[] arr1 = new String[]{"", "valid value", "also invalid value"};
        String[] arr2 = new String[]{"", "valid.value-2", "also invalid value-2"};

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put("param1", arr1);
        queryParams.put("param2", arr2);

        boolean actualValid = restQueryParamsValidationInterceptor.handleQueryParams(queryParams, null);

        Assertions.assertTrue(actualValid);
    }

    @Test
    void handleQueryParamsTestInValid() {
        String[] arr1 = new String[]{"", "valid value", "also valid value"};
        String[] arr2 = new String[]{"", "invalid.value;2", "also invalid value%2"};

        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put("param1", arr1);
        queryParams.put("param2", arr2);

        new Expectations() {{
            blacklistValidator.validate((ObjectPropertiesMapBlacklistValidator.Parameter) any);
            result = new ValidationException("");
        }};

        Assertions.assertThrows(ValidationException. class,() -> restQueryParamsValidationInterceptor.handleQueryParams(queryParams, null));
    }

}
