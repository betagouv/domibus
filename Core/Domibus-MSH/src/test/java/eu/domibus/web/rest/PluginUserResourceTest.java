package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.UserState;
import eu.domibus.api.user.plugin.AuthenticationEntity;
import eu.domibus.api.user.plugin.PluginUserService;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.user.plugin.PluginUserMapper;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.PluginUserFilterRequestRO;
import eu.domibus.web.rest.ro.PluginUserRO;
import eu.domibus.web.rest.ro.PluginUserResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginUserResourceTest {

    @Tested
    PluginUserResource userResource;

    @Injectable
    PluginUserMapper pluginUserMapper;

    @Injectable
    private PluginUserService pluginUserService;

    @Injectable
    AuthCoreMapper authCoreMapper;

    @Injectable
    private CsvServiceImpl csvServiceImpl;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Injectable
    UserDomainService userDomainService;

    @Test
    public void findUsersTest() {
        AuthType authType = AuthType.BASIC;
        AuthRole authRole = AuthRole.ROLE_ADMIN;
        String originalUser = "originalUser1";
        String userName = "userName1";
        int pageStart = 1, pageSize = 10;

        AuthenticationEntity userAuthenticationEntity = new AuthenticationEntity();
        userAuthenticationEntity.setUserName(userName);
        List<AuthenticationEntity> userAuthenticationEntities = Arrays.asList(userAuthenticationEntity);

        PluginUserRO pluginUserRO = new PluginUserRO();
        pluginUserRO.setUserName(userName);
        List<PluginUserRO> pluginUserROList = Arrays.asList(pluginUserRO);

        new Expectations(userResource) {{
            pluginUserService.countUsers(authType, authRole, originalUser, userName);
            result = 1;

            pluginUserService.findUsers(authType, authRole, originalUser, userName, pageStart, pageSize);
            result = userAuthenticationEntities;

            pluginUserMapper.convertAndPrepareUsers(userAuthenticationEntities);
            result = pluginUserROList;
        }};

        PluginUserFilterRequestRO req = new PluginUserFilterRequestRO() {{
            setAuthType(authType);
            setAuthRole(authRole);
            setOriginalUser(originalUser);
            setUserName(userName);
            setPageStart(pageStart);
            setPageSize(pageSize);
        }};
        PluginUserResultRO result = userResource.findUsers(req);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(pluginUserRO, result.getEntries().get(0));
    }

    @Test
    public void updateUsersTest() {
        PluginUserRO userRO = new PluginUserRO();
        userRO.setUserName("user1");
        userRO.setStatus(UserState.NEW.name());
        List<PluginUserRO> userROs = Arrays.asList(userRO);

        AuthenticationEntity user = new AuthenticationEntity();
        user.setUserName("user1");
        final List<AuthenticationEntity> userList = Arrays.asList(user);

        new Expectations() {{
            authCoreMapper.pluginUserROListToAuthenticationEntityList(userROs);
            result = userList;
        }};

        userResource.updateUsers(userROs);

        new Verifications() {{
            List<AuthenticationEntity> addedUsers, updatedUsers, removedUsers;
            pluginUserService.updateUsers(addedUsers = withCapture(), updatedUsers = withCapture(), removedUsers = withCapture());
            times = 1;
            assertEquals(1, addedUsers.size());
            assertEquals(0, updatedUsers.size());
            assertEquals(0, removedUsers.size());
            assertEquals("user1", addedUsers.get(0).getUserName());
        }};
    }

    @Test
    public void getExcludedColumns() {
        List<String> excludedCert = userResource.getExcludedColumns(AuthType.CERTIFICATE);
        assertEquals(excludedCert.size(), 9);
        Set<String> set1 = new HashSet<>(Arrays.asList("userName", "expirationDate", "active", "suspended"));
        boolean containsAll = set1.isEmpty() || excludedCert.stream().map(Object::toString)
                .anyMatch(s -> set1.remove(s) && set1.isEmpty());
        assertTrue(containsAll);

        List<String> excludedBasic = userResource.getExcludedColumns(AuthType.BASIC);
        assertEquals(excludedBasic.size(), 6);
        Set<String> set2 = new HashSet<>(Arrays.asList("certificateId", "authenticationType", "entityId", "status", "password", "domain"));
        containsAll = set2.isEmpty() || excludedBasic.stream().map(Object::toString)
                .anyMatch(s -> set2.remove(s) && set2.isEmpty());
        assertTrue(containsAll);
    }

    @Test
    public void getCustomColumnNames() {
        Map<String, String> customCert = userResource.getCustomColumnNames(AuthType.CERTIFICATE);
        assertEquals(customCert.size(), 1);
        assertTrue(customCert.get("authRoles".toUpperCase()).equals("Role"));
        Map<String, String> customBasic = userResource.getCustomColumnNames(AuthType.BASIC);
        assertEquals(customBasic.size(), 2);
        assertTrue(customBasic.get("UserName".toUpperCase()).equals("User Name"));
    }
}
