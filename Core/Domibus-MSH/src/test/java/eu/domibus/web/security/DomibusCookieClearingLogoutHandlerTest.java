package eu.domibus.web.security;

import eu.domibus.web.rest.AuthenticationResource;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static eu.domibus.core.spring.DomibusSessionConfiguration.SESSION_COOKIE_NAME;
import static eu.domibus.web.rest.AuthenticationResource.CSRF_COOKIE_NAME;

/**
 * @author Sebastian-Ion TINCU
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class DomibusCookieClearingLogoutHandlerTest {

    private DomibusCookieClearingLogoutHandler handler;

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Injectable
    private Authentication authentication;

    @Test
    @Disabled("EDELIVERY-6896")
    public void removesCookiesNotHavingTheirPathsEndingWithForwardSlashInAdditionToTheOnesEndingWithIt() {
        givenCookieClearingLogoutHandler(SESSION_COOKIE_NAME, CSRF_COOKIE_NAME);
        givenContextPath("");

        whenLoggingOut();

        thenCookiesHavingTheirPathsBothEndingAndNotEndingWithSlashAddedToResponseToBeRemoved();
    }

    @Test
    void throwsExceptionWhenCookiesToRemoveIsNull() {
        Assertions.assertThrows(IllegalArgumentException. class,() -> givenCookieClearingLogoutHandler((String[]) null));
    }


    private void givenContextPath(String contextPath) {
        new Expectations() {{
           request.getContextPath(); result = "domibus";
        }};
    }

    private void givenCookieClearingLogoutHandler(String... cookiesToClear) {
        handler = new DomibusCookieClearingLogoutHandler(cookiesToClear);
    }

    private void whenLoggingOut() {
        handler.logout(request, response, authentication);
    }

    private void thenCookiesHavingTheirPathsBothEndingAndNotEndingWithSlashAddedToResponseToBeRemoved() {
        new Verifications() {{
            List<Cookie> cookies = new ArrayList<>();
            response.addCookie(withCapture(cookies));

            Assertions.assertTrue(cookies.stream()
                                        .filter(cookie -> StringUtils.endsWith(cookie.getPath(), "/"))
                                        .allMatch(cookie -> StringUtils.equalsAny(cookie.getName(), SESSION_COOKIE_NAME, CSRF_COOKIE_NAME)
                                                && StringUtils.equals(cookie.getPath(), "domibus/")),
                    "Should have removed the cookies having their paths ending with a forwards slash");
            Assertions.assertTrue(cookies.stream()
                                        .filter(cookie -> !StringUtils.endsWith(cookie.getPath(), "/"))
                                        .allMatch(cookie -> StringUtils.equalsAny(cookie.getName(), SESSION_COOKIE_NAME, CSRF_COOKIE_NAME)
                                                && StringUtils.equals(cookie.getPath(), "domibus")),
                    "Should have also removed the cookies having their paths not ending with a forwards slash");
        }};
    }
}
