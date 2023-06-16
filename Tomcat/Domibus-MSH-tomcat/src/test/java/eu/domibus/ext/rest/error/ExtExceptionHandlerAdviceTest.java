package eu.domibus.ext.rest.error;

import eu.domibus.ext.exceptions.DomibusServiceExtException;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Catalin Enache
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class ExtExceptionHandlerAdviceTest {

    @Tested
    ExtExceptionHandlerAdvice extExceptionHandlerAdvice;

    @Injectable
    ExtExceptionHelper extExceptionHelper;


    @Test
    public void test_handleException() {
        //tested method
        Exception e = new Exception();
        extExceptionHandlerAdvice.handleException(e);

        new FullVerifications() {{
            extExceptionHelper.createResponse(e);
        }};
    }

    @Test
    public void test_handleDomibusServiceExtException() {

        DomibusServiceExtException e = new DomibusServiceExtException(null, null);
        extExceptionHandlerAdvice.handleDomibusServiceExtException(e);

        new FullVerifications() {{
            extExceptionHelper.handleExtException(e);
        }};
    }

    @Test
    public void test_handleException_Generic() {

        Exception e = new Exception();
        extExceptionHandlerAdvice.handleException(e);

        new FullVerifications() {{
            extExceptionHelper.createResponse(e);
        }};
    }
}
