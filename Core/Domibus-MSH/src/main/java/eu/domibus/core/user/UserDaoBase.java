package eu.domibus.core.user;

import eu.domibus.api.security.AuthRole;
import eu.domibus.api.user.UserEntityBase;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
public interface UserDaoBase<U extends UserEntityBase> {
    U findByUserName(String userName);

    List<U> findWithPasswordChangedBetween(LocalDate start, LocalDate end, boolean withDefaultPassword);

    List<U> getSuspendedUsers(Date currentTimeMinusSuspensionInterval);

    void update(U user, boolean flush);

    void update(List<U> users);

    List<U> findByRole(AuthRole roleName);

    /**
     * Checks if there is a user with the specified name
     * @param userId the name of the user to check
     * @return true if exists already, false otherwise
     */
    boolean existsWithId(String userId);
}
