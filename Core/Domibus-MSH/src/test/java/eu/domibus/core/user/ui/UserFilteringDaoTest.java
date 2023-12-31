package eu.domibus.core.user.ui;

import eu.domibus.core.dao.BasicDao;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class UserFilteringDaoTest {

    @Tested
    UserFilteringDao userFilteringDao;

    @Injectable
    Predicate predicate;

    @Injectable
    CriteriaBuilder criteriaBuilder;

    @Injectable
    Root<User> userEntity;

    @Injectable
    BasicDao basicDao;

    @Injectable
    EntityManager entityManager;

    @Test
    public void getPredicates() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(userFilteringDao.USER_ROLE, "ROLE_ADMIN");
        filters.put(userFilteringDao.USER_NAME, "admin");
        filters.put(userFilteringDao.DELETED_USER, true);

        List<Predicate> predicates = userFilteringDao.getPredicates(filters, criteriaBuilder, userEntity);

        Assertions.assertEquals(predicates.size(), 3);
    }
}
