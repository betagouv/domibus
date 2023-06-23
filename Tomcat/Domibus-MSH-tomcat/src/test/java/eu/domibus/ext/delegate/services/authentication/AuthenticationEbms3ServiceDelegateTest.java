package eu.domibus.ext.delegate.services.authentication;

import eu.domibus.api.security.AuthUtils;
import eu.domibus.ext.exceptions.AuthenticationExtException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.http.HttpServletRequest;

/**
 * @author migueti, Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class AuthenticationEbms3ServiceDelegateTest {

    @Tested
    AuthenticationServiceDelegate messageAcknowledgeServiceDelegate;

    @Injectable
    eu.domibus.api.security.AuthenticationService authenticationService;

    @Injectable
    protected AuthUtils authUtils;

    @Test
    public void testAuthenticate(@Injectable final HttpServletRequest httpRequest) throws Exception {
        messageAcknowledgeServiceDelegate.authenticate(httpRequest);

        new Verifications() {{
            authenticationService.authenticate(httpRequest);
        }};
    }


    @Test
    void testAuthenticateWhenAuthenticationExceptionIsRaised(@Injectable final HttpServletRequest httpRequest) throws Exception {
        new Expectations(messageAcknowledgeServiceDelegate) {{
            authenticationService.authenticate(httpRequest);
            result = new eu.domibus.api.security.AuthenticationException("not authenticated");
        }};

        Assertions.assertThrows(AuthenticationExtException. class,() -> messageAcknowledgeServiceDelegate.authenticate(httpRequest));

        new Verifications() {{
            authenticationService.authenticate(httpRequest);
        }};
    }

}
