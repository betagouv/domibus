package eu.domibus.security;

import eu.domibus.AbstractIT;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.user.UserManagementException;
import eu.domibus.common.JPAConstants;
import eu.domibus.core.multitenancy.dao.UserDomainDao;
import eu.domibus.core.user.ui.User;
import eu.domibus.core.user.ui.UserDao;
import eu.domibus.core.user.ui.UserRole;
import eu.domibus.core.user.ui.UserRoleDao;
import eu.domibus.core.user.ui.security.ConsoleUserSecurityPolicyManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
@Transactional
public class ConsoleUserSecurityPolicyManagerTestIT extends AbstractIT {

    @Autowired
    ConsoleUserSecurityPolicyManager userSecurityPolicyManager;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    protected UserDao userDao;

    @Autowired
    UserDomainDao userDomainDao;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    private DomainTaskExecutor domainTaskExecutor;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager entityManager;

    @Test
    @Transactional
    @Rollback
    public void testPasswordReusePolicy_shouldPass() {
        User user = initTestUser("testUser1");
        userSecurityPolicyManager.changePassword(user, "Password-1111111");
        userSecurityPolicyManager.changePassword(user, "Password-2222222");
        userSecurityPolicyManager.changePassword(user, "Password-3333333");
        userSecurityPolicyManager.changePassword(user, "Password-4444444");
        userSecurityPolicyManager.changePassword(user, "Password-5555555");
        userSecurityPolicyManager.changePassword(user, "Password-6666666");
        userSecurityPolicyManager.changePassword(user, "Password-1111111");
    }

    @Test
    @Transactional
    @Rollback
    public void testPasswordReusePolicy_shouldFail() {
        User user = initTestUser("testUser2");
        Assertions.assertThrows(DomibusCoreException.class, () -> {
                    userSecurityPolicyManager.changePassword(user, "Password-1111111");
                    userSecurityPolicyManager.changePassword(user, "Password-2222222");
                    userSecurityPolicyManager.changePassword(user, "Password-3333333");
                    userSecurityPolicyManager.changePassword(user, "Password-4444444");
                    userSecurityPolicyManager.changePassword(user, "Password-5555555");
                    userSecurityPolicyManager.changePassword(user, "Password-1111111");
                }
        );

    }

    @Test
    @Transactional
    @Rollback
    public void testPasswordComplexity_blankPasswordShouldFail() {
        User user = initTestUser("testUser3");
        Assertions.assertThrows(DomibusCoreException. class,() -> userSecurityPolicyManager.changePassword(user, ""));
    }

    @Test
    @Transactional
    @Rollback
    public void testPasswordComplexity_shortPasswordShouldFail() {
        User user = initTestUser("testUser4");
        Assertions.assertThrows(DomibusCoreException. class,() -> userSecurityPolicyManager.changePassword(user, "Aa-1"));
    }

    @Test
    @Transactional
    @Rollback
    public void test_validateUniqueUser() {
        User user = initTestUser("testUser_Unique");
        Assertions.assertThrows(UserManagementException. class,() -> userSecurityPolicyManager.validateUniqueUser(user));
    }

    private User initTestUser(String userName) {
        UserRole userRole = userRoleDao.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new UserRole("ROLE_USER");
            entityManager.persist(userRole);
        }
        User user = new User();
        user.setUserName(userName);
        user.setPassword("Password-0");
        user.addRole(userRole);
        user.setEmail("test@mailinator.com");
        user.setActive(true);
        userDao.create(user);

        if (domibusConfigurationService.isMultiTenantAware()) {
            String domainCode = domainContextProvider.getCurrentDomainSafely().getCode();
            domainTaskExecutor.submit(() -> userDomainDao.updateOrCreateUserDomain(userName, domainCode));
        }

        return user;
    }
}
