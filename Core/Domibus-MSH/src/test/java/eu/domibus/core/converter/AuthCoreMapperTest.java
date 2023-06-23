package eu.domibus.core.converter;

import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.User;
import eu.domibus.api.user.plugin.AuthenticationEntity;
import eu.domibus.web.rest.ro.PluginUserRO;
import eu.domibus.web.rest.ro.UserResponseRO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author François Gautier
 * @since 5.0
 */
public class AuthCoreMapperTest extends AbstractMapperTest {

    @Autowired
    private AuthCoreMapper authCoreMapper;

    @Test
    public void convertUserUserResponseRO() {
        User toConvert = (User) objectService.createInstance(User.class);
        final UserResponseRO converted = authCoreMapper.userToUserResponseRO(toConvert);
        final User convertedBack = authCoreMapper.userResponseROToUser(converted);

        convertedBack.setDefaultPassword(toConvert.hasDefaultPassword());

        objectService.assertObjects(convertedBack, toConvert);
    }

    @Test
    public void convertUserUserUI() {
        eu.domibus.core.user.ui.User toConvert = (eu.domibus.core.user.ui.User) objectService.createInstance(eu.domibus.core.user.ui.User.class);
        final User converted = authCoreMapper.userSecurityToUserApi(toConvert);
        final eu.domibus.core.user.ui.User convertedBack = authCoreMapper.userApiToUserSecurity(converted);

        convertedBack.setAttemptCount(toConvert.getAttemptCount());
        convertedBack.setSuspensionDate(toConvert.getSuspensionDate());
        convertedBack.setPasswordChangeDate(toConvert.getPasswordChangeDate());
        convertedBack.setDefaultPassword(toConvert.hasDefaultPassword());
        convertedBack.setEntityId(toConvert.getEntityId());
        convertedBack.setPassword(toConvert.getPassword());
        toConvert.getRoles().forEach(userRole -> convertedBack.addRole(userRole));

        objectService.assertObjects(convertedBack, toConvert);
    }

    @Test
    public void convertAuthenticationEntityPluginUserRO_Certificate() {
        AuthenticationEntity toConvert = (AuthenticationEntity) objectService.createInstance(AuthenticationEntity.class);

        final PluginUserRO converted = authCoreMapper.authenticationEntityToPluginUserRO(toConvert);
        converted.setAuthenticationType(AuthType.CERTIFICATE.name());
        final AuthenticationEntity convertedBack = authCoreMapper.pluginUserROToAuthenticationEntity(converted);

        convertedBack.setAttemptCount(toConvert.getAttemptCount());
        convertedBack.setSuspensionDate(toConvert.getSuspensionDate());
        convertedBack.setPasswordChangeDate(toConvert.getPasswordChangeDate());
        convertedBack.setDefaultPassword(toConvert.hasDefaultPassword());
        convertedBack.setBackend(toConvert.getBackend());

        convertedBack.setUserName(toConvert.getUserName());

        objectService.assertObjects(convertedBack, toConvert);
    }

    @Test
    public void convertAuthenticationEntityPluginUserRO_Basic() {
        AuthenticationEntity toConvert = (AuthenticationEntity) objectService.createInstance(AuthenticationEntity.class);

        final PluginUserRO converted = authCoreMapper.authenticationEntityToPluginUserRO(toConvert);
        converted.setAuthenticationType(AuthType.BASIC.name());
        final AuthenticationEntity convertedBack = authCoreMapper.pluginUserROToAuthenticationEntity(converted);

        convertedBack.setAttemptCount(toConvert.getAttemptCount());
        convertedBack.setSuspensionDate(toConvert.getSuspensionDate());
        convertedBack.setPasswordChangeDate(toConvert.getPasswordChangeDate());
        convertedBack.setDefaultPassword(toConvert.hasDefaultPassword());
        convertedBack.setBackend(toConvert.getBackend());

        convertedBack.setCertificateId(toConvert.getCertificateId());

        objectService.assertObjects(convertedBack, toConvert);
    }

}
