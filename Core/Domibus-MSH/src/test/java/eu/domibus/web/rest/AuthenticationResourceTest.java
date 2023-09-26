package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskException;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.multitenancy.dao.UserDomainDao;
import eu.domibus.core.user.UserPersistenceService;
import eu.domibus.core.user.UserService;
import eu.domibus.core.user.ui.User;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.DomainRO;
import eu.domibus.web.rest.ro.LoginRO;
import eu.domibus.web.rest.ro.UserRO;
import eu.domibus.web.security.AuthenticationService;
import eu.domibus.web.security.DomibusUserDetailsImpl;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class AuthenticationResourceTest {

    @Tested
    AuthenticationResource authenticationResource;

    @Injectable
    AuthenticationService authenticationService;

    @Injectable
    UserDomainDao userDomainDao;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    UserDomainService userDomainService;

    @Injectable
    DomainService domainService;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomibusCoreMapper coreMapper;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Injectable
    protected UserPersistenceService userPersistenceService;

    @Injectable
    private UserService allUserManagementService;

    @Injectable
    private UserService userManagementService;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    CompositeSessionAuthenticationStrategy compositeSessionAuthenticationStrategy;


    @Test
    public void testWarningWhenDefaultPasswordUsed() {
        User user = new User() {{
            setUserName("user");
            setPassword("user");
        }};
        user.setDefaultPassword(true);
        LoginRO loginRO = new LoginRO();
        loginRO.setUsername("user");
        loginRO.setPassword("user");
        final DomibusUserDetailsImpl domibusUserDetails = new DomibusUserDetailsImpl(user);
        new Expectations() {{
            userDomainService.getDomainForUser(loginRO.getUsername());
            result = DomainService.DEFAULT_DOMAIN.getCode();

            authenticationService.authenticate("user", "user", DomainService.DEFAULT_DOMAIN.getCode());
            result = domibusUserDetails;


        }};

        authenticationResource.authenticate(loginRO, new MockHttpServletResponse(), null);

        new FullVerifications() {{
            domainContextProvider.setCurrentDomainWithValidation(DomainService.DEFAULT_DOMAIN.getCode());
            compositeSessionAuthenticationStrategy.onAuthentication((Authentication) any, (HttpServletRequest) any, (HttpServletResponse) any);
        }};
    }

    @Test
    public void testGetCurrentDomain() {
        // Given
        final DomainRO domainRO = new DomainRO();
        domainRO.setCode(DomainService.DEFAULT_DOMAIN.getCode());
        domainRO.setName(DomainService.DEFAULT_DOMAIN.getName());

        new Expectations(authenticationResource) {{
            domainContextProvider.getCurrentDomainSafely();
            result = DomainService.DEFAULT_DOMAIN;

            coreMapper.domainToDomainRO(DomainService.DEFAULT_DOMAIN);
            result = domainRO;
        }};

        // When
        final DomainRO result = authenticationResource.getCurrentDomain();

        // Then
        Assertions.assertEquals(domainRO, result);
    }

    @Test
    void testExceptionInSetCurrentDomain() {
        // Given
        new Expectations(authenticationResource) {{
            authenticationService.changeDomain("");
            result = new DomainTaskException("");
        }};
        // When
        Assertions.assertThrows(DomainTaskException.class, () -> authenticationResource.setCurrentDomain(""));
        // Then
        // expect DomainException
    }

    @Test
    public void testLogout_PrincipalExists(final @Injectable HttpServletRequest request,
                                           final @Injectable HttpServletResponse response,
                                           final @Injectable SecurityContext securityContext,
                                           final @Injectable Authentication authentication,
                                           final @Mocked CookieClearingLogoutHandler cookieClearingLogoutHandler,
                                           final @Mocked SecurityContextLogoutHandler securityContextLogoutHandler) {
        new Expectations() {{
            new MockUp<SecurityContextHolder>() {
                @Mock
                SecurityContext getContext() {
                    return securityContext;
                }
            };

            securityContext.getAuthentication();
            result = authentication;

            CookieClearingLogoutHandler cookieClearingLogoutHandler1 = new CookieClearingLogoutHandler("JSESSIONID", "XSRF-TOKEN");
            SecurityContextLogoutHandler securityContextLogoutHandler1 = new SecurityContextLogoutHandler();
            cookieClearingLogoutHandler1.logout(request, response, null);
            securityContextLogoutHandler1.logout(request, response, authentication);

            request.getContextPath();
            result = "contextPath";

            authentication.getName();
            result = "authName";
        }};

        //tested method
        authenticationResource.logout(request, response);

        new FullVerifications() {{
            response.addCookie((Cookie) any);
        }};
    }

    @Test
    public void testGetUser(final @Mocked DomibusUserDetailsImpl domibusUserDetails) {
        new Expectations() {{
            authenticationService.getLoggedUser();
            result = domibusUserDetails;
        }};

        //tested method
        final UserRO userNameActual = authenticationResource.getUser();
        Assertions.assertNotNull(userNameActual);
    }

    @Test
    public void testHandleAccountStatusException() {
        AccountStatusException ex = new AccountExpiredException("TEST");
        new Expectations(authenticationResource) {{
            errorHandlerService.createResponse(ex, HttpStatus.FORBIDDEN);
            result = any;
        }};
        //tested method
        authenticationResource.handleAccountStatusException(ex);
        new FullVerifications() {{
            errorHandlerService.createResponse(ex, HttpStatus.FORBIDDEN);
            times = 1;
        }};
    }

    @Test
    public void testHandleAuthenticationException() {
        AuthenticationException ex = new AccountExpiredException("TEST");

        new Expectations(authenticationResource) {{
            errorHandlerService.createResponse(ex, HttpStatus.FORBIDDEN);
            result = any;
        }};
        //tested method
        authenticationResource.handleAuthenticationException(ex);
        new FullVerifications() {{
            errorHandlerService.createResponse(ex, HttpStatus.FORBIDDEN);
            times = 1;
        }};
    }
}
