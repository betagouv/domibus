package eu.domibus.ext.web.interceptor;

import eu.domibus.ext.exceptions.AuthenticationExtException;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.services.AuthenticationExtService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author migueti, Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class AuthenticationInterceptorTest {

    @Tested
    AuthenticationInterceptor authenticationInterceptor;

    @Injectable
    AuthenticationExtService authenticationExtService;

    @Test
    public void testPreHandle(@Injectable final HttpServletRequest httpRequest,
                              @Injectable final HttpServletResponse httpServletResponse) throws Exception {

        new Expectations() {{
            authenticationExtService.enforceAuthentication(httpRequest);
        }};

        assertTrue(authenticationInterceptor.preHandle(httpRequest, httpServletResponse, new Object()));
    }


    @Test
    public void testPreHandleWhenExceptionIsRaised(@Injectable final HttpServletRequest httpRequest,
                              @Injectable final HttpServletResponse httpServletResponse) throws Exception {

        new Expectations() {{
            authenticationExtService.enforceAuthentication(httpRequest);
            result = new AuthenticationExtException(DomibusErrorCode.DOM_002, "authentication error");
        }};

        assertFalse(authenticationInterceptor.preHandle(httpRequest, httpServletResponse, new Object()));
    }

}
