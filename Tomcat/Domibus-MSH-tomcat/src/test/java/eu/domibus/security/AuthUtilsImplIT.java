package eu.domibus.security;

import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.security.AuthUtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * This class tests the PreAuthorize annotation.
 * In order to trigger the @PreAuthorize, we use Spring to create the bean with
 * '@EnableGlobalMethodSecurity(prePostEnabled = true)'
 * <p>
 * '@WithMockUser' is then use to set up the security context with a mock user easily
 *
 * @author François Gautier
 * @since 4.2
 */
@ExtendWith(SpringExtension.class)
public class AuthUtilsImplIT {

    @Autowired
    private AuthUtils authUtils;

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    static class ContextConfiguration {
        @Bean
        public AuthUtils authUtils() {
            return new AuthUtilsImpl(null, null);
        }
    }

    @Test
    void hasAdminRole_noUser() {
        Assertions.assertThrows(AuthenticationCredentialsNotFoundException.class, () -> authUtils.checkHasAdminRoleOrUserRoleWithOriginalUser());
    }

    @Test
    @WithMockUser(username = "ecas", roles = {"ECAS"})
    void hasAdminRole_user() {
        Assertions.assertThrows(AccessDeniedException.class, () -> authUtils.checkHasAdminRoleOrUserRoleWithOriginalUser());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"AP_ADMIN"})
    public void hasAdminRole_apAdmin() {
        authUtils.checkHasAdminRoleOrUserRoleWithOriginalUser();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void hasAdminRole_admin() {
        authUtils.checkHasAdminRoleOrUserRoleWithOriginalUser();
    }

    @Test
    void hasUserRole_noUser() {
        Assertions.assertThrows(AuthenticationCredentialsNotFoundException.class, () -> authUtils.checkHasAdminRoleOrUserRoleWithOriginalUser());
    }

}
