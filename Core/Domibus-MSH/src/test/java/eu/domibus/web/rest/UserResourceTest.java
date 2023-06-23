package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.user.User;
import eu.domibus.api.user.UserRole;
import eu.domibus.api.user.UserState;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.user.UserService;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.UserFilterRequestRO;
import eu.domibus.web.rest.ro.UserResponseRO;
import eu.domibus.web.rest.ro.UserResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class UserResourceTest {

    @Tested
    UserResource userResource;

    @Injectable
    private UserService allUserManagementService;

    @Injectable
    private UserService userManagementService;

    @Injectable
    AuthCoreMapper authCoreMapper;

    @Injectable
    private CsvServiceImpl csvServiceImpl;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomainService domainService;

    private List<UserResponseRO> getUserResponseList() {
        final List<UserResponseRO> userResponseROList = new ArrayList<>();
        UserResponseRO userResponseRO = getUserResponseRO();
        userResponseROList.add(userResponseRO);
        return userResponseROList;
    }

    private UserResponseRO getUserResponseRO() {
        UserResponseRO userResponseRO = new UserResponseRO();
        userResponseRO.setUserName("username");
        userResponseRO.setEmail("email");
        userResponseRO.setActive(true);
        userResponseRO.setDomain("default");
        userResponseRO.setAuthorities(Arrays.asList("ROLE_USER"));
        userResponseRO.updateRolesField();
        userResponseRO.setStatus("PERSISTED");
        return userResponseRO;
    }

    @Test
    public void testUsers(@Injectable Domain domain) {
        // Given
        User user = new User();
        user.setUserName("userName");
        user.setEmail("email");
        user.setActive(true);
        user.setAuthorities(Arrays.asList("ROLE_USER"));
        user.setStatus(UserState.PERSISTED.name());

        final List<User> userList = new ArrayList<User>();
        userList.add(user);

        final List<UserResponseRO> userResponseROList = getUserResponseList();
        List<Domain> domainsList = new ArrayList<>();
        domainsList.add(domain);

        new Expectations() {{
            userManagementService.findUsers();
            result = userList;

            domain.getCode();
            result = "default";

            domainService.getDomains();
            result = domainsList;

            authCoreMapper.userListToUserResponseROList(userList);
            result = userResponseROList;
        }};

        // When
        List<UserResponseRO> userResponseROS = userResource.getUsers();
        userResource.updateUsers(userResponseROS);

        // Then
        Assertions.assertNotNull(userResponseROS);
        UserResponseRO userResponseRO = getUserResponseRO();
        Assertions.assertEquals(userResponseRO, userResponseROS.get(0));
    }

    @Test
    public void testRegularAdminRoles() {
        List<UserRole> userRoles = Arrays.asList(new UserRole(AuthRole.ROLE_ADMIN.name()));

        // Given
        new Expectations() {{
            authUtils.isSuperAdmin();
            result = false;
            userManagementService.findUserRoles();
            result = userRoles;
        }};

        // When
        List<String> roles = userResource.userRoles();

        // Then
        Assertions.assertNotNull(roles);
        Assertions.assertFalse(roles.contains(AuthRole.ROLE_AP_ADMIN.name()), "ROLE_AP_ADMIN must not be returned for regular admins");
    }

    @Test
    public void testSuperAdminRole() {
        List<UserRole> userRoles = Arrays.asList(new UserRole(AuthRole.ROLE_ADMIN.name()));

        // Given
        new Expectations() {{
            authUtils.isSuperAdmin();
            result = true;
            allUserManagementService.findUserRoles();
            result = userRoles;
        }};

        // When
        List<String> roles = userResource.userRoles();

        // Then
        Assertions.assertNotNull(roles);
        Assertions.assertTrue(roles.contains(AuthRole.ROLE_AP_ADMIN.name()), "ROLE_AP_ADMIN must be returned for super admins");
    }

    @Test
    public void testGetCsv(@Injectable UserFilterRequestRO request,
                           @Injectable UserResultRO entries,
                           @Injectable UserResponseRO userResponseRO,
                           @Injectable ResponseEntity<String> responseEntity) {
        List<UserResponseRO> usersResponseROList = new ArrayList<>();
        usersResponseROList.add(userResponseRO);

        new Expectations(userResource) {{
            userResource.retrieveAndPackageUsers(request);
            result = entries;
            csvServiceImpl.exportToCSV(entries.getEntries(), UserResponseRO.class, (Map<String, String>) any, (List<String>) any);
            result = anyString;
        }};

        final ResponseEntity<String> csv = userResource.getCsv(request);

        Assertions.assertEquals(HttpStatus.OK, csv.getStatusCode());
    }

    @Test
    public void retrieveAndPackageUsers(@Injectable UserFilterRequestRO request,
                                        @Injectable UserResultRO entries,
                                        @Injectable UserResponseRO userResponseRO,
                                        @Injectable User user) {
        List<User> users = new ArrayList<>();
        users.add(user);
        List<UserResponseRO> userResponseROS = new ArrayList<>();
        userResponseROS.add(userResponseRO);

        new Expectations(userResource) {{
            userManagementService.findUsersWithFilters(request.getAuthRole(), request.getUserName(), request.getDeleted(),
                    request.getPageStart(), request.getPageSize());
            result = users;
            userResource.prepareResponse(users);
            result = userResponseROS;
        }};

        Assertions.assertNotNull(userResource.retrieveAndPackageUsers(request));
    }
}
