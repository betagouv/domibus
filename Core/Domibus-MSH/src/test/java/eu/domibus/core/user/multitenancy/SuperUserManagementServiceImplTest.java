package eu.domibus.core.user.multitenancy;

import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.user.User;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.service.ConsoleUserAlertsServiceImpl;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.multitenancy.dao.UserDomainDao;
import eu.domibus.core.multitenancy.dao.UserDomainEntity;
import eu.domibus.core.user.UserPersistenceService;
import eu.domibus.core.user.ui.UserDao;
import eu.domibus.core.user.ui.UserFilteringDao;
import eu.domibus.core.user.ui.UserManagementServiceImpl;
import eu.domibus.core.user.ui.UserRoleDao;
import eu.domibus.core.user.ui.security.ConsoleUserSecurityPolicyManager;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class SuperUserManagementServiceImplTest {

    @Tested
    @Mocked
    private SuperUserManagementServiceImpl superUserManagementService;

    @Injectable
    AlertConfigurationService alertConfigurationService;

    @Injectable
    UserPersistenceService userPersistenceService;

    @Injectable
    protected UserRoleDao userRoleDao;

    @Injectable
    protected UserDao userDao;

    @Injectable
    protected UserDomainService userDomainService;

    @Injectable
    protected DomainTaskExecutor domainTaskExecutor;

    @Injectable
    private UserManagementServiceImpl userManagementService;

    @Injectable
    ConsoleUserSecurityPolicyManager userPasswordManager;

    @Injectable
    ConsoleUserAlertsServiceImpl userAlertsService;

    @Injectable
    UserDomainDao userDomainDao;

    @Injectable
    protected AuthUtils authUtils;

    @Injectable
    UserFilteringDao userFilteringDao;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomainService domainService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    AuthCoreMapper authCoreMapper;

    @Test
    public void updateUsers() {
        User user = new User() {{
            setUserName("user1");
            setAuthorities(Arrays.asList(AuthRole.ROLE_USER.toString()));
        }};

        User sUser = new User() {{
            setUserName("super1");
            setAuthorities(Arrays.asList(AuthRole.ROLE_AP_ADMIN.toString()));
        }};

        List<User> all = Arrays.asList(user, sUser);

        superUserManagementService.updateUsers(all);

        new Verifications() {{
            SecurityContextHolder.getContext().getAuthentication();
            SecurityContextHolder.getContext().setAuthentication((Authentication) any);
            domainTaskExecutor.submit((Runnable) any);
        }};
    }

    @Test
    public void changePassword() {
        String username = "u1", currentPassword = "pass1", newPassword = "newPass1";

        superUserManagementService.changePassword(username, currentPassword, newPassword);

        new Verifications() {{
            domainTaskExecutor.submit((Runnable) any);
            times = 1;
        }};
    }

    @Test
    public void getPreferredDomainForUser(@Mocked eu.domibus.api.user.User user,
                                          @Mocked UserDomainEntity userDomainEntity1,
                                          @Mocked UserDomainEntity userDomainEntity2) {

        new Expectations() {{
            user.getUserName();
            result = "user2";
            userDomainEntity1.getUserName();
            result = "user1";
            userDomainEntity2.getUserName();
            result = "user2";
            userDomainEntity2.getPreferredDomain();
            result = "domain2";
            userDomainDao.listPreferredDomains();
            result = Arrays.asList(userDomainEntity1, userDomainEntity2);
        }};

        String res = superUserManagementService.getPreferredDomainForUser(user);

        assertEquals("domain2", res);
    }


}
