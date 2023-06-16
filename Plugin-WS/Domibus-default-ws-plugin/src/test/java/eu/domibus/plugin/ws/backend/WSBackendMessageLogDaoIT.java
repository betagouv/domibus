package eu.domibus.plugin.ws.backend;

import eu.domibus.plugin.ws.AbstractBackendWSIT;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static java.time.LocalDateTime.of;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author François Gautier
 * @since 5.0
 */
@Transactional
public class WSBackendMessageLogDaoIT extends AbstractBackendWSIT {

    @Autowired
    private WSBackendMessageLogDao wsBackendMessageLogDao;

    private WSBackendMessageLogEntity entityFailed2021;
    private WSBackendMessageLogEntity entityFailed2022;

    private WSBackendMessageLogEntity entityRetried1;

    private WSBackendMessageLogEntity entityRetried2;

    @BeforeEach
    public void setUp() {
        entityFailed2021 = create(WSBackendMessageStatus.SEND_FAILURE,
                of(2021, 12, 31, 1, 1),
                "SENDER1",
                "BACKEND1");
        entityFailed2022 = create(WSBackendMessageStatus.SEND_FAILURE,
                of(2022, 5, 10, 1, 1),
                "SENDER2",
                "BACKEND2");
        entityRetried1 = create(WSBackendMessageStatus.WAITING_FOR_RETRY);
        entityRetried2 = create(WSBackendMessageStatus.WAITING_FOR_RETRY);
        createEntityAndFlush(Arrays.asList(entityFailed2021,
                entityFailed2022,
                entityRetried1,
                entityRetried2));
    }

    @Test
    public void retry() {
        int count = wsBackendMessageLogDao.updateForRetry(
                Arrays.asList(entityFailed2021.getMessageId(),
                        entityFailed2022.getMessageId()));
        Assertions.assertEquals(2, count);
    }

    @Test
    public void findByMessageId_notFound() {
        WSBackendMessageLogEntity byMessageId = wsBackendMessageLogDao.findByMessageId("");
        Assertions.assertNull(byMessageId);
    }

    @Test
    public void findByMessageId_findOne() {
        WSBackendMessageLogEntity byMessageId = wsBackendMessageLogDao.findByMessageId(entityFailed2021.getMessageId());
        Assertions.assertNotNull(byMessageId);
    }

    @Test
    public void findRetryMessages() {
        List<WSBackendMessageLogEntity> messages = wsBackendMessageLogDao.findRetryMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(2, messages.size());
        assertThat(messages, CoreMatchers.hasItems(entityRetried1, entityRetried2));
    }

    @Test
    public void findAllFailedWithFilter() {
        List<WSBackendMessageLogEntity> allFailedWithFilter =
                wsBackendMessageLogDao.findAllFailedWithFilter(null, null, null, null, null, 5);
        assertThat(allFailedWithFilter.size(), Is.is(2));
        assertThat(allFailedWithFilter, CoreMatchers.hasItems(entityFailed2021, entityFailed2022));
    }

    @Test
    public void findAllFailedWithFilter_DateFrom() {
        List<WSBackendMessageLogEntity> allFailedWithFilter =
                wsBackendMessageLogDao.findAllFailedWithFilter(null, null, null, of(2022, 1, 1, 1, 1, 1), null, 5);
        assertThat(allFailedWithFilter.size(), Is.is(1));
        assertThat(allFailedWithFilter, CoreMatchers.hasItems(entityFailed2022));
    }

    @Test
    public void findAllFailedWithFilter_DateTo() {
        List<WSBackendMessageLogEntity> allFailedWithFilter =
                wsBackendMessageLogDao.findAllFailedWithFilter(null, null, null, null, of(2022, 1, 1, 1, 1, 1), 5);
        assertThat(allFailedWithFilter.size(), Is.is(1));
        assertThat(allFailedWithFilter, CoreMatchers.hasItems(entityFailed2021));
    }

    @Test
    public void findAllFailedWithFilter_Sender() {
        List<WSBackendMessageLogEntity> allFailedWithFilter =
                wsBackendMessageLogDao.findAllFailedWithFilter(null, "SENDER1", null, null, null, 5);
        assertThat(allFailedWithFilter.size(), Is.is(1));
        assertThat(allFailedWithFilter, CoreMatchers.hasItems(entityFailed2021));
    }

    @Test
    public void findAllFailedWithFilter_FinalRecipient() {
        List<WSBackendMessageLogEntity> allFailedWithFilter =
                wsBackendMessageLogDao.findAllFailedWithFilter(null, null, "BACKEND1", null, null, 5);
        assertThat(allFailedWithFilter.size(), Is.is(1));
        assertThat(allFailedWithFilter, CoreMatchers.hasItems(entityFailed2021));
    }

    @Test
    public void findRetriableAfterRepushed() {
        List<WSBackendMessageLogEntity> beforeRepush = wsBackendMessageLogDao.findRetryMessages();
        Assertions.assertEquals(2, beforeRepush.size()); // only 2 are ready for retry and none of them is in send_failure status
        wsBackendMessageLogDao.updateForRetry(
                Arrays.asList(entityFailed2021.getMessageId(),
                        entityFailed2022.getMessageId()));
        em.refresh(entityFailed2021);
        em.refresh(entityFailed2022);

        List<WSBackendMessageLogEntity> afterRepush = wsBackendMessageLogDao.findRetryMessages();
        // the 2 failed messages have been repushed so now they are added to the list waiting for retry
        Assertions.assertEquals(4, afterRepush.size());

        // check that the failed messages have been moved to backend status WAITING_FOR_RETRY, thus ready to be picked up for pushing again to C4
        Assertions.assertTrue(afterRepush.contains(entityFailed2021));
        Assertions.assertTrue(afterRepush.contains(entityFailed2022));

        Assertions.assertFalse(beforeRepush.contains(entityFailed2021));
        Assertions.assertFalse(beforeRepush.contains(entityFailed2022));
    }
}
