package eu.domibus.core.user.ui;


import eu.domibus.api.security.AuthRole;
import eu.domibus.core.dao.BasicDao;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Thomas Dussart, Ion Perpegel
 * @since 3.3
 * <p>
 * Dao to handle admin console users.
 */
@Repository
public class UserDaoImpl extends BasicDao<User> implements UserDao {

    public UserDaoImpl() {
        super(User.class);
    }

    @Override
    public List<User> listUsers() {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findAll", User.class);
        return namedQuery.getResultList();
    }

    @Override
    public List<User> getSuspendedUsers(final Date currentTimeMinusSuspensionInterval) {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findSuspendedUsers", User.class);
        namedQuery.setParameter("SUSPENSION_INTERVAL", currentTimeMinusSuspensionInterval);
        return namedQuery.getResultList().stream().collect(Collectors.toList());
    }

    @Override
    public User loadUserByUsername(String userName) {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findByUserName", User.class);
        namedQuery.setParameter(User.USER_NAME, userName);
        try {
            return namedQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User loadActiveUserByUsername(String userName) {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findActiveByUserName", User.class);
        namedQuery.setParameter(User.USER_NAME, userName);
        try {
            return namedQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void update(final List<User> users) {
        for (final User u : users) {
            super.update(u);
        }
    }

    @Override
    public void delete(final Collection<User> users) {
        users.forEach(user -> delete(user));
    }

    @Override
    public void delete(final User user) {
            user.setDeleted(true);
            super.update(user);
    }

    @Override
    public User findByUserName(String userName) {
        return loadUserByUsername(userName);
    }

    @Override
    public List<User> findWithPasswordChangedBetween(LocalDate start, LocalDate end, boolean withDefaultPassword) {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findWithPasswordChangedBetween", User.class);
        namedQuery.setParameter("START_DATE", start.atStartOfDay());
        namedQuery.setParameter("END_DATE", end.atStartOfDay());
        namedQuery.setParameter("DEFAULT_PASSWORD", withDefaultPassword);
        return namedQuery.getResultList().stream().collect(Collectors.toList());
    }

    @Override
    public void update(User user, boolean flush) {
        this.update(user);
        if (flush) {
            this.flush();
        }
    }

    @Override
    public List<User> findByRole(AuthRole role) {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findByRoleName", User.class);
        namedQuery.setParameter("ROLE_NAME", role.name());
        return namedQuery.getResultList().stream().collect(Collectors.toList());
    }

    @Override
    public boolean existsWithId(String userID) {
        TypedQuery<User> namedQuery = em.createNamedQuery("User.findAllByUserName", User.class);
        namedQuery.setParameter("USER_NAME", userID);
        return namedQuery.getResultList().size() > 0;
    }

}
