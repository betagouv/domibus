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
import eu.domibus.core.user.UserPersistenceService;
import eu.domibus.core.user.UserService;
import eu.domibus.core.user.ui.UserDao;
import eu.domibus.core.user.ui.UserFilteringDao;
import eu.domibus.core.user.ui.UserManagementServiceImpl;
import eu.domibus.core.user.ui.UserRoleDao;
import eu.domibus.core.user.ui.security.ConsoleUserSecurityPolicyManager;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
public class AllUsersManagementServiceImplTest {

    @Tested
    private AllUsersManagementServiceImpl allUsersManagementService;

    @Injectable
    @Qualifier(SuperUserManagementServiceImpl.BEAN_NAME)
    private UserService superUserManagementService;

    @Injectable
    @Qualifier(UserManagementServiceImpl.BEAN_NAME)
    private UserService userManagementService;

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
    public void findUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User() {{
            setUserName("user1");
        }});
        List<User> superUsers = new ArrayList<>();
        superUsers.add(new User() {{
            setUserName("super1");
        }});

        new Expectations() {{
            userManagementService.findUsers();
            result = users;
            superUserManagementService.findUsers();
            result = superUsers;
        }};

        List<User> all = allUsersManagementService.findUsers();

        assertEquals(all.size(), 2);
        assertEquals(all.get(0).getUserName(), "user1");
        assertEquals(all.get(1).getUserName(), "super1");
    }

    @Test
    public void findUsersWithFilters(@Injectable eu.domibus.api.user.User user,
                                     @Injectable eu.domibus.api.user.User user1) {
        List<User> users = new ArrayList<>();
        users.add(user);
        List<User> superUsers = new ArrayList<>();
        superUsers.add(user1);

        new Expectations() {{
            userManagementService.findUsersWithFilters(AuthRole.ROLE_ADMIN, "admin", "true", 1, 10);
            result = users;
            superUserManagementService.findUsersWithFilters(AuthRole.ROLE_ADMIN, "admin", "true", 1, 10);
            result = superUsers;
        }};

        List<User> all = allUsersManagementService.findUsersWithFilters(AuthRole.ROLE_ADMIN, "admin", "true", 1, 10);

        assertEquals(all.size(), 2);
    }

    @Test
    public void updateUsers() {
        eu.domibus.api.user.User user = new User();
        eu.domibus.api.user.User superUser = new User();
        List<User> users = new ArrayList<>();
        users.add(user);
        List<User> superUsers = new ArrayList<>();
        superUsers.add(superUser);
        List<User> allUsers = Arrays.asList(user, superUser);
        user.setAuthorities(Arrays.asList(AuthRole.ROLE_ADMIN.name()));
        superUser.setAuthorities(Arrays.asList(AuthRole.ROLE_AP_ADMIN.name()));

        allUsersManagementService.updateUsers(allUsers);

        new Verifications() {{
            List<User> regularUsers;
            userManagementService.updateUsers(regularUsers = withCapture());
            assertEquals(regularUsers.get(0), user);

            List<User> superUsers;
            superUserManagementService.updateUsers(superUsers = withCapture());
            assertEquals(superUsers.get(0), superUser);
        }};
    }

    @Test
    public void changePassword() {
        String username = "username";
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";

        new Expectations() {{
            userDomainService.getDomainForUser(username);
            result = "domain";
        }};

        allUsersManagementService.changePassword(username, currentPassword, newPassword);

        new Verifications() {{
            userManagementService.changePassword(username, currentPassword, newPassword);
            superUserManagementService.changePassword(username, currentPassword, newPassword);
            times = 0;
        }};
    }
}
