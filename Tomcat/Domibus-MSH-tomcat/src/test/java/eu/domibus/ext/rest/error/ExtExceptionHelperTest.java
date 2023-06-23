package eu.domibus.ext.rest.error;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.ext.domain.ErrorDTO;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.DomibusServiceExtException;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 4.2
 * @author Catalin Enache
 */
@ExtendWith(JMockitExtension.class)
public class ExtExceptionHelperTest {

    @Tested
    ExtExceptionHelper extExceptionHelper;

    @Test
    public void test_handleExtException() {
                final DomibusCoreException domibusCoreException = new DomibusCoreException("core test exception");
        final DomibusServiceExtException extException = new DomibusServiceExtException(DomibusErrorCode.DOM_003, "ext core exception", domibusCoreException);

        new Expectations(extExceptionHelper) {{
        }};

        //tested method
        extExceptionHelper.handleExtException(extException);

        new FullVerifications(extExceptionHelper) {{
            HttpStatus httpStatusActual;
            extExceptionHelper.createResponseFromCoreException((Throwable) any, httpStatusActual = withCapture());
            Assertions.assertEquals(HttpStatus.BAD_REQUEST, httpStatusActual);
        }};
    }

    @Test
    public void test_createResponse() {

        //tested method
        Throwable throwable = new Throwable();
        ResponseEntity<ErrorDTO> result = extExceptionHelper.createResponse(throwable);
        Assertions.assertNotNull(result.getBody());
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());

        new FullVerifications(extExceptionHelper) {{
            boolean showErrorDetailsActual;
            extExceptionHelper.createResponse(throwable, HttpStatus.INTERNAL_SERVER_ERROR, anyBoolean);
        }};
    }

    @Test
    public void test_getPModeValidationMessage() {
        ValidationIssue validationIssue = new ValidationIssue("test 123");
        List<ValidationIssue> validationIssueList = Collections.singletonList(validationIssue);
        PModeValidationException e = new PModeValidationException(validationIssueList);

        //tested method
        String errorMessage = extExceptionHelper.getPModeValidationMessage(e);
        Assertions.assertTrue(errorMessage.contains("test 123"));
    }

    @Test
    public void test_createResponseFromPModeValidationException() {
        final String errorMessage = "[DOM_003]:PMode validation failed. Validation issues: Initiator party [blue_gw2] of process [tc1Process] not found in business process parties";
        PModeValidationException e = new PModeValidationException(new ArrayList<>());

        new Expectations(extExceptionHelper) {{
            extExceptionHelper.getPModeValidationMessage(e);
            result =  errorMessage;
        }};

        extExceptionHelper.createResponseFromPModeValidationException(e);

        new FullVerifications() {{
            String errorMessageActual;
            extExceptionHelper.createResponse(errorMessageActual = withCapture(), ExtExceptionHelper.HTTP_STATUS_INVALID_REQUEST);
            Assertions.assertEquals(errorMessage, errorMessageActual);
        }};
    }

    @Test
    public void identifyExtErrorCodeFromCoreErrorCode() {
        Assertions.assertEquals(DomibusErrorCode.DOM_009, extExceptionHelper.identifyExtErrorCodeFromCoreErrorCode(DomibusCoreErrorCode.DOM_009));
        Assertions.assertEquals(DomibusErrorCode.DOM_002, extExceptionHelper.identifyExtErrorCodeFromCoreErrorCode(DomibusCoreErrorCode.DOM_002));
    }
}
