package eu.domibus.core.user.ui;

import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.user.AtLeastOneAdminException;
import eu.domibus.api.user.UserManagementException;
import eu.domibus.api.user.UserState;
import eu.domibus.core.alerts.service.ConsoleUserAlertsServiceImpl;
import eu.domibus.core.converter.AuthCoreMapper;
import eu.domibus.core.user.UserLoginErrorReason;
import eu.domibus.core.user.UserPersistenceService;
import eu.domibus.core.user.UserService;
import eu.domibus.core.user.ui.security.ConsoleUserSecurityPolicyManager;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PASSWORD_POLICY_DEFAULT_USER_AUTOGENERATE_PASSWORD;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PASSWORD_POLICY_DEFAULT_USER_CREATE;

/**
 * * Management of regular users, used in ST mode and when a domain admin user logs in in MT mode
 *
 * @author Thomas Dussart, Ion Perpegel
 * @since 3.3
 */
@Service(UserManagementServiceImpl.BEAN_NAME)
public class UserManagementServiceImpl implements UserService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserManagementServiceImpl.class);

    public static final String BEAN_NAME = "userManagementService";

    private static final String ALL_USERS = "all";
    private static final String USER_NAME = "userName";
    private static final String USER_ROLE = "userRole";
    private static final String DELETED_USER = "deleted";

    @Autowired
    protected UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    protected UserPersistenceService userPersistenceService;

    @Autowired
    protected UserDomainService userDomainService;

    @Autowired
    ConsoleUserSecurityPolicyManager userSecurityPolicyManager;

    @Autowired
    ConsoleUserAlertsServiceImpl userAlertsService;

    @Autowired
    protected AuthUtils authUtils;

    @Autowired
    private UserFilteringDao userFilteringDao;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected AuthCoreMapper authCoreMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<eu.domibus.api.user.User> findUsers() {
        return findUsers(this::getDomainForUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<eu.domibus.api.user.UserRole> findUserRoles() {
        LOG.debug("Retrieving user roles");
        List<UserRole> userRolesEntities = userRoleDao.listRoles();

        List<eu.domibus.api.user.UserRole> userRoles = new ArrayList<>();
        for (UserRole userRoleEntity : userRolesEntities) {
            eu.domibus.api.user.UserRole userRole = new eu.domibus.api.user.UserRole(userRoleEntity.getName());
            userRoles.add(userRole);
        }
        return userRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateUsers(List<eu.domibus.api.user.User> users) {
        try {
            userPersistenceService.updateUsers(users);
            if (!domibusConfigurationService.isMultiTenantAware()) {
                LOG.debug("Check at least one admin exists.");
                ensureAtLeastOneActiveAdmin();
            } else {
                LOG.debug("No check for multitenancy: a super admin always exists.");
            }
        } catch (AtLeastOneAdminException ex) {
            // clear user-domain mapping only for this error
            LOG.info("Remove domain association for new users.");
            users.stream()
                    .filter(eu.domibus.api.user.User::isNew)
                    .forEach(user -> userDomainService.deleteDomainForUser(user.getUserName()));
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized UserLoginErrorReason handleWrongAuthentication(final String userName) {
        // there is no security context when the user failed to login -> we're creating one
        return authUtils.runFunctionWithDomibusSecurityContext(() -> userSecurityPolicyManager.handleWrongAuthentication(userName), AuthRole.ROLE_ADMIN, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void reactivateSuspendedUsers() {
        userSecurityPolicyManager.reactivateSuspendedUsers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCorrectAuthentication(final String userName) {
        userSecurityPolicyManager.handleCorrectAuthentication(userName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateExpiredPassword(final String userName) {
        User user = getUserWithName(userName);
        boolean defaultPassword = user.hasDefaultPassword();
        LocalDateTime passwordChangeDate = user.getPasswordChangeDate();

        userSecurityPolicyManager.validatePasswordExpired(userName, defaultPassword, passwordChangeDate);
    }

    @Override
    public Integer getDaysTillExpiration(String userName) {
        User user = getUserWithName(userName);
        boolean isDefaultPassword = user.hasDefaultPassword();
        LocalDateTime passwordChangeDate = user.getPasswordChangeDate();

        return userSecurityPolicyManager.getDaysTillExpiration(userName, isDefaultPassword, passwordChangeDate);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void triggerPasswordAlerts() {
        userAlertsService.triggerPasswordExpirationEvents();
    }

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        userPersistenceService.changePassword(username, currentPassword, newPassword);
    }

    /**
     * Retrieves users from DB and sets some attributes for each user
     *
     * @param getDomainForUserFn the function to get the domain
     * @return the list of users
     */
    protected List<eu.domibus.api.user.User> findUsers(Function<eu.domibus.api.user.User, String> getDomainForUserFn) {
        LOG.debug("Retrieving console users");
        List<User> userEntities = userDao.listUsers();

        return prepareUsers(getDomainForUserFn, userEntities);
    }

    /**
     * Calls a function to get the domain for each user and also sets expiration date
     *
     * @param getDomainForUserFn the function to get the domain
     * @return the list of users
     */
    protected List<eu.domibus.api.user.User> prepareUsers(Function<eu.domibus.api.user.User, String> getDomainForUserFn, List<User> userEntities) {
        List<eu.domibus.api.user.User> users = new ArrayList<>();
        userEntities.forEach(userEntity -> {
            eu.domibus.api.user.User user = convertAndPrepareUser(getDomainForUserFn, userEntity);
            users.add(user);
        });
        return users;
    }

    protected eu.domibus.api.user.User convertAndPrepareUser(Function<eu.domibus.api.user.User, String> getDomainForUserFn, User userEntity) {
        eu.domibus.api.user.User user = authCoreMapper.userSecurityToUserApi(userEntity);

        String domainCode = getDomainForUserFn.apply(user);
        user.setDomain(domainCode);

        LocalDateTime expDate = userSecurityPolicyManager.getExpirationDate(userEntity);
        user.setExpirationDate(expDate);
        return user;
    }

    private String getDomainForUser(eu.domibus.api.user.User user) {
        return userDomainService.getDomainForUser(user.getUserName());
    }

    protected User getUserWithName(String userName) {
        User user = userDao.findByUserName(userName);
        if (user == null) {
            throw new UserManagementException("Could not find console user with the name " + userName);
        }
        return user;
    }

    protected void ensureAtLeastOneActiveAdmin() {
        if (!hasAtLeastOneActiveAdmin()) {
            throw new AtLeastOneAdminException();
        }
    }

    protected boolean hasAtLeastOneActiveAdmin() {
        AuthRole role = getAdminRole();
        List<User> users = userDao.findByRole(role);
        long count = users.stream().filter(u -> !u.isDeleted() && u.isActive()).count();
        return count > 0;
    }

    /**
     * Search users based on the following criteria's.
     *
     * @param authRole criteria to search the role of user (ROLE_ADMIN or ROLE_USER)
     * @param userName criteria to search by userName
     * @param page     pagination start
     * @param pageSize page size.
     */
    @Override
    @Transactional(readOnly = true)
    public List<eu.domibus.api.user.User> findUsersWithFilters(AuthRole authRole, String userName, String deleted, int page, int pageSize) {
        return findUsersWithFilters(authRole, userName, deleted, page, pageSize, this::getDomainForUser);
    }


    protected List<eu.domibus.api.user.User> findUsersWithFilters(AuthRole authRole, String userName, String deleted, int page, int pageSize, Function<eu.domibus.api.user.User, String> getDomainForUserFn) {

        LOG.debug("Retrieving console users");
        Map<String, Object> filters = createFilterMap(userName, deleted, authRole);
        List<User> users = userFilteringDao.findPaged(page * pageSize, pageSize, "entityId", true, filters);
        List<eu.domibus.api.user.User> finalUsers = prepareUsers(getDomainForUserFn, users);
        return finalUsers;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers(AuthRole authRole, String userName, String deleted) {
        Map<String, Object> filters = createFilterMap(userName, deleted, authRole);
        return userFilteringDao.countEntries(filters);
    }

    @Override
    public void createDefaultUserIfApplicable() {
        if (domibusConfigurationService.isExtAuthProviderEnabled()) {
            LOG.info("Default user creation is disabled when using EU Login; exiting.");
            return;
        }

        boolean enabled = domibusPropertyProvider.getBooleanProperty(DOMIBUS_PASSWORD_POLICY_DEFAULT_USER_CREATE);
        if (!enabled) {
            LOG.info("Default user creation [{}] is disabled; exiting.", DOMIBUS_PASSWORD_POLICY_DEFAULT_USER_CREATE);
            return;
        }

        // check already exists an active admin user
        if (hasAtLeastOneActiveAdmin()) {
            LOG.info("A user with role [{}] already exists; exiting.", getAdminRole());
            return;
        }

        String userName = getDefaultUserName();
        eu.domibus.api.user.User user = createDefaultUser(userName);

        userPersistenceService.updateUsers(Arrays.asList(user));
    }

    protected eu.domibus.api.user.User createDefaultUser(String userName) {
        eu.domibus.api.user.User user = new eu.domibus.api.user.User();

        user.setUserName(userName);
        user.setStatus(UserState.NEW.name());

        AuthRole userRole = getAdminRole();
        user.setAuthorities(Arrays.asList(userRole.name()));

        // need to set the hasDefaultPassword property
        user.setDefaultPassword(true);
        user.setActive(true);

        // generate password as guid
        String password = getPassword();
        user.setPassword(password);
        LOG.info("Default password for user [{}] is [{}].", userName, password);

        return user;
    }

    private String getPassword() {
        String result;
        boolean generate = domibusPropertyProvider.getBooleanProperty(DOMIBUS_PASSWORD_POLICY_DEFAULT_USER_AUTOGENERATE_PASSWORD);
        if (generate) {
            long start = System.currentTimeMillis();

            result = UUID.randomUUID().toString();

            long finish = System.currentTimeMillis();
            LOG.info("Password generation for default user took [{}]", finish - start);
        } else {
            result = "123456";
            LOG.info("Password for the default user will be hardcoded");
        }

        return result;
    }

    protected Map<String, Object> createFilterMap(String userName, String deleted, AuthRole authRole) {
        HashMap<String, Object> filters = new HashMap<>();
        addUserNameFilter(userName, filters);
        addDeletedUserFilter(deleted, filters);
        addUserRoleFilter(authRole, filters);
        LOG.debug("Added users filters: [{}]", filters);
        return filters;
    }

    protected void addUserRoleFilter(AuthRole authRole, HashMap<String, Object> filters) {
        if (authRole != null) {
            filters.put(USER_ROLE, authRole.name());
        }
    }

    protected void addUserNameFilter(String userName, HashMap<String, Object> filters) {
        if (userName != null) {
            filters.put(USER_NAME, userName);
        }
    }

    protected void addDeletedUserFilter(String deleted, HashMap<String, Object> filters) {
        if (StringUtils.equals(deleted, ALL_USERS)) {
            filters.put(DELETED_USER, null);
        } else {
            filters.put(DELETED_USER, Boolean.parseBoolean(deleted));
        }
    }

    protected AuthRole getAdminRole() {
        return AuthRole.ROLE_ADMIN;
    }

    protected String getDefaultUserName() {
        return "admin";
    }
}
