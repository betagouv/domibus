package eu.domibus.core.pulling;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 * @author Thomas Dussart
 * @since 4.2
 */

@ExtendWith(JMockitExtension.class)
public class PullRequestDaoTest {

    @Tested
    PullRequestDao pullRequestDao;

    @Injectable
    private EntityManager em;

    @Test
    public void testPersist(){
        PullRequest pullRequest = new PullRequest();
        pullRequestDao.savePullRequest(pullRequest);
        new Verifications(){{
           em.persist(pullRequest);
        }};
    }

    @Test
    public void testDeletePullRequest(@Mocked final Query query) {
        new Expectations(){{
            em.createNamedQuery("PullRequest.delete");
            result=query;
        }};
        String uuid = "uuid";
        pullRequestDao.deletePullRequest(uuid);
        new Verifications(){{
            query.setParameter("UUID",uuid);
            query.executeUpdate();
        }};

    }

    @Test
    public void countPendingPullRequest(@Mocked  final TypedQuery<Long> query){
        new Expectations(){{
            em.createNamedQuery("PullRequest.count", Long.class);
            result=query;
        }};
        pullRequestDao.countPendingPullRequest();
        new Verifications(){{
            query.getSingleResult();
        }};
    }

}
