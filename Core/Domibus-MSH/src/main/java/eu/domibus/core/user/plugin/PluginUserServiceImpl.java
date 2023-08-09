package eu.domibus.core.user.plugin;

import com.google.common.collect.Streams;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthType;
import eu.domibus.api.user.UserBase;
import eu.domibus.api.user.UserManagementException;
import eu.domibus.api.user.plugin.AuthenticationEntity;
import eu.domibus.api.user.plugin.PluginUserService;
import eu.domibus.core.alerts.service.PluginUserAlertsServiceImpl;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.user.plugin.security.PluginUserSecurityPolicyManager;
import eu.domibus.core.user.plugin.security.password.PluginUserPasswordHistoryDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.domibus.api.property.DomibusGeneralConstants.*;

/**
 * @author Ion Perpegel, Catalin Enache
 * @since 4.0
 */
@Service
public class PluginUserServiceImpl implements PluginUserService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginUserServiceImpl.class);

    @Autowired
    @Qualifier("securityAuthenticationDAO")
    private AuthenticationDAO authenticationDAO;

    @Autowired
    private BCryptPasswordEncoder bcryptEncoder;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private DomainContextProvider domainProvider;

    @Autowired
    private PluginUserSecurityPolicyManager userSecurityPolicyManager;

    @Autowired
    PluginUserAlertsServiceImpl userAlertsService;

    @Autowired
    PluginUserPasswordHistoryDao pluginUserPasswordHistoryDao;

    @Autowired
    private AuthCoreMapper authCoreMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AuthenticationEntity> findUsers(AuthType authType, AuthRole authRole, String originalUser, String userName, int page, int pageSize) {
        Map<String, Object> filters = createFilterMap(authType, authRole, originalUser, userName);
        List<AuthenticationEntity> users = authenticationDAO.findPaged(page * pageSize, pageSize, "entityId", true, filters);
        return users;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers(AuthType authType, AuthRole authRole, String originalUser, String userName) {
        Map<String, Object> filters = createFilterMap(authType, authRole, originalUser, userName);
        return authenticationDAO.countEntries(filters);
    }

    @Override
    @Transactional
    public void updateUsers(List<AuthenticationEntity> addedUsers, List<AuthenticationEntity> updatedUsers, List<AuthenticationEntity> removedUsers) {

        final Domain currentDomain = domainProvider.getCurrentDomain();

        checkUsers(addedUsers, updatedUsers);

        addedUsers.forEach(u -> insertNewUser(u, currentDomain));

        updatedUsers.forEach(u -> updateUser(u));

        removedUsers.forEach(u -> deleteUser(u));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerPasswordAlerts() {
        userAlertsService.triggerPasswordExpirationEvents();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void reactivateSuspendedUsers() {
        userSecurityPolicyManager.reactivateSuspendedUsers();
    }

    /**
     * get all users from general schema and validate new users against existing names
     *
     * @param addedUsers
     * @param updatedUsers
     */
    protected void checkUsers(List<AuthenticationEntity> addedUsers, List<AuthenticationEntity> updatedUsers) throws UserManagementException {
        // check duplicates with other plugin users
        for (AuthenticationEntity user : addedUsers) {
            if (!StringUtils.isEmpty(user.getUserName())) {
                if (addedUsers.stream().anyMatch(x -> x != user && user.getUserName().equalsIgnoreCase(x.getUserName()))) {
                    throw new UserManagementException("Cannot add user " + user.getUserName() + " more than once.");
                }
                validatePluginUserName(user.getUserName());
            }
            if (StringUtils.isNotBlank(user.getCertificateId())) {
                if (addedUsers.stream().anyMatch(x -> x != user && user.getCertificateId().equalsIgnoreCase(x.getCertificateId())))
                    throw new UserManagementException("Cannot add user with certificate " + user.getCertificateId() + " more than once.");
            }
        }

        // check for duplicates with other users or plugin users in single and multi-tenancy modes
        for (UserBase user : addedUsers) {
            userSecurityPolicyManager.validateUniqueUser(user);
        }

        Streams.concat(addedUsers.stream(), updatedUsers.stream())
                .forEach(authenticationEntity -> {
                            validateAuthRoles(authenticationEntity.getAuthRoles());
                            validateOriginalUser(authenticationEntity.getOriginalUser());
                        }
                );
        Streams.concat(addedUsers.stream(), updatedUsers.stream())
                .filter(user -> StringUtils.equals(user.getAuthRoles(), AuthRole.ROLE_USER.name()))
                .filter(user -> StringUtils.isEmpty(user.getOriginalUser()))
                .findFirst()
                .ifPresent(user -> {
                    throw new UserManagementException("Cannot add or update the user " + user.getUserName()
                            + " having the " + AuthRole.ROLE_USER.name() + " role without providing the original user value.");
                });

    }

    protected void validateOriginalUser(String originalUser) {
        if(StringUtils.isBlank(originalUser)){
            return;
        }
    }

    protected void validateAuthRoles(String authRoles) {
        if(StringUtils.isBlank(authRoles)){
            LOG.error("Valid AuthRoles not provided for PluginUser");
            throw new UserManagementException("Valid AuthRoles should be supplied for PluginUser.");
        }
        //authRoles is semicolon separated list
        List<String> lstAuthRoles = Arrays.asList(authRoles.split(";"));
        for (String authRole : lstAuthRoles) {
            try {
                AuthRole.valueOf(authRole);
            } catch (IllegalArgumentException e) {
                LOG.error("AuthRole supplied:[{}] is unknown.", authRole);
                throw new UserManagementException("AuthRole supplied " + authRole + " is not known.");
            }
        }
    }

    protected void validatePluginUserName(String userName) {
        String lclPluginUserName = StringUtils.trim(userName);
        int lclPluginUserNameLength = StringUtils.length(lclPluginUserName);
        if (lclPluginUserNameLength < PLUGIN_USERNAME_MIN_LENGTH || lclPluginUserNameLength > PLUGIN_USERNAME_MAX_LENGTH) {
            throw new UserManagementException("Plugin User Username should be between 4 and 255 characters long.");
        }
        if (!lclPluginUserName.matches(PLUGIN_USERNAME_PATTERN)) {
            throw new UserManagementException("Plugin User should be alphanumeric with allowed special characters .@_");
        }
    }

    protected Map<String, Object> createFilterMap(AuthType authType, AuthRole authRole, String originalUser, String userName) {
        HashMap<String, Object> filters = new HashMap<>();
        if (authType != null) {
            filters.put("authType", authType.name());
        }
        if (authRole != null) {
            filters.put("authRoles", authRole.name());
        }
        filters.put("originalUser", originalUser);
        filters.put("userName", userName);
        return filters;
    }

    protected void insertNewUser(AuthenticationEntity u, Domain domain) {
        if (u.getPassword() != null) {
            userSecurityPolicyManager.validateComplexity(u.getUserName(), u.getPassword());
            u.setPassword(bcryptEncoder.encode(u.getPassword()));
        }
        authenticationDAO.create(u);

        userDomainService.setDomainForUser(u.getUniqueIdentifier(), domain.getCode());
    }

    protected void updateUser(AuthenticationEntity modified) {
        AuthenticationEntity existing = authenticationDAO.read(modified.getEntityId());

        if (StringUtils.isBlank(existing.getCertificateId())) {
            // locking policy is only applicable to Basic auth plugin users
            userSecurityPolicyManager.applyLockingPolicyOnUpdate(modified, existing);
        }
        existing.setActive(modified.isActive());

        if (!StringUtils.isEmpty(modified.getPassword())) {
            changePassword(existing, modified.getPassword());
        }

        existing.setAuthRoles(modified.getAuthRoles());
        existing.setOriginalUser(modified.getOriginalUser());

        authenticationDAO.update(existing);
    }

    private void changePassword(AuthenticationEntity user, String newPassword) {
        userSecurityPolicyManager.changePassword(user, newPassword);
    }

    protected void deleteUser(AuthenticationEntity u) {
        AuthenticationEntity entity = authenticationDAO.read(u.getEntityId());
        delete(entity);

        userDomainService.deleteDomainForUser(u.getUniqueIdentifier());
    }

    private void delete(AuthenticationEntity user) {
        //delete password history
        pluginUserPasswordHistoryDao.removePasswords(user, 0);
        //delete actual user
        authenticationDAO.delete(user);
    }
}
