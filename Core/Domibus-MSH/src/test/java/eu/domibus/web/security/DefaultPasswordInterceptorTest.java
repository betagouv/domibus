package eu.domibus.web.security;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.HttpURLConnection;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JMockitExtension.class)
public class DefaultPasswordInterceptorTest {

    @Tested
    private DefaultPasswordInterceptor defaultPasswordInterceptor;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    private DomibusUserDetailsImpl domibusUserDetails;

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Test
    public void preHandle_DefaultPassword() throws Exception {
        // GIVEN
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD);
            result = "true";

            request.getRequestURI();
            result = "domibus/rest/authentication";

            authUtils.getUserDetails();
            result = domibusUserDetails;

            domibusUserDetails.isDefaultPasswordUsed();
            result = true;
        }};

        // WHEN
        boolean proceed = defaultPasswordInterceptor.preHandle(request, response, null);

        // THEN
        assertFalse(proceed);
        new FullVerifications() {{
            response.setStatus(HttpURLConnection.HTTP_FORBIDDEN);
        }};
    }

    @Test
    public void preHandle_ChangedPassword() throws Exception {
        // GIVEN
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD);
            result = "true";

            request.getRequestURI();
            result = "domibus/rest/authentication";

            authUtils.getUserDetails();
            result = domibusUserDetails;

            domibusUserDetails.isDefaultPasswordUsed();
            result = false;
        }};

        // WHEN
        boolean proceed = defaultPasswordInterceptor.preHandle(request, response, null);

        // THEN
        assertTrue(proceed);
        new FullVerifications() { /* no unexpected interactions */ };
    }

    @Test
    public void preHandle_NoUserDetails() throws Exception {
        // GIVEN
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD);
            result = "true";

            request.getRequestURI();
            result = "domibus/rest/authentication";

            authUtils.getUserDetails();
            result = null;
        }};

        // WHEN
        boolean proceed = defaultPasswordInterceptor.preHandle(request, response, null);

        // THEN
        assertTrue(proceed);
        new FullVerifications() { /* no unexpected interactions */ };
    }

    @Test
    public void preHandle_CheckIsDisabled() throws Exception {
        // GIVEN
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PASSWORD_POLICY_CHECK_DEFAULT_PASSWORD);
            result = "false";
        }};

        // WHEN
        boolean proceed = defaultPasswordInterceptor.preHandle(request, response, null);

        // THEN
        assertTrue(proceed);
        new FullVerifications() { /* no unexpected interactions */ };
    }
}
