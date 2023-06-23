package eu.domibus.core.user.plugin;

import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.UserState;
import eu.domibus.api.user.plugin.AuthenticationEntity;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.user.plugin.security.PluginUserSecurityPolicyManager;
import eu.domibus.web.rest.ro.PluginUserRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for PluginUserMapper
 * @author Arun Raj
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class PluginUserMapperTest {

    @Tested
    private PluginUserMapper pluginUserMapper;

    @Injectable
    PluginUserSecurityPolicyManager userSecurityPolicyManager;

    @Injectable
    private AuthCoreMapper authCoreMapper;

    @Injectable
    private UserDomainService userDomainService;

    

    @Test
    public void convertAndPrepareUsers() {
        AuthenticationEntity user = new AuthenticationEntity();
        user.setUserName("user1");
        final List<AuthenticationEntity> userList = Arrays.asList(user);

        PluginUserRO userRO = new PluginUserRO();
        userRO.setUserName("user1");
        userRO.setExpirationDate(LocalDateTime.now(ZoneOffset.UTC).plusDays(30));

        LocalDateTime expDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(30);

        new Expectations(pluginUserMapper) {{
            pluginUserMapper.convertAndPrepareUser(user);
            result = userRO;
        }};

        List<PluginUserRO> result = pluginUserMapper.convertAndPrepareUsers(userList);

        Assertions.assertEquals(userList.size(), result.size());
        Assertions.assertEquals(userRO, result.get(0));
    }

    @Test
    public void convertAndPrepareUser() {
        AuthenticationEntity user = new AuthenticationEntity();
        user.setUserName("user1");

        PluginUserRO userRO = new PluginUserRO();
        userRO.setUserName("user1");
        userRO.setExpirationDate(LocalDateTime.now(ZoneOffset.UTC).plusDays(30));

        LocalDateTime expDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(30);

        new Expectations() {{
            authCoreMapper.authenticationEntityToPluginUserRO(user);
            result = userRO;
            userSecurityPolicyManager.getExpirationDate(user);
            result = expDate;
            userDomainService.getDomainForUser(user.getUniqueIdentifier());
            result="domain1";
        }};

        PluginUserRO result = pluginUserMapper.convertAndPrepareUser(user);

        Assertions.assertEquals(userRO, result);
        Assertions.assertEquals(UserState.PERSISTED.name(), result.getStatus());
        Assertions.assertEquals(AuthType.BASIC.name(), result.getAuthenticationType());
        Assertions.assertEquals(!user.isActive() && user.getSuspensionDate() != null, result.isSuspended());
        Assertions.assertEquals("domain1", result.getDomain());
        Assertions.assertEquals(expDate, result.getExpirationDate());
    }

}
