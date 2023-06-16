package eu.domibus.core.earchive;

import eu.domibus.AbstractIT;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.common.JPAConstants;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author François Gautier
 * @since 5.0
 */
@Transactional
public class EArchiveBatchDaoIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveBatchDaoIT.class);

    @Autowired
    EArchiveBatchDao eArchiveBatchDao;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;
    private EArchiveBatchEntity firstContinuous;
    private EArchiveBatchEntity secondContinuous;
    private EArchiveBatchEntity firstManual;

    @BeforeEach
    @Transactional
    public void setup() {
        firstContinuous = new EArchiveBatchEntity();
        secondContinuous = new EArchiveBatchEntity();
        firstManual = new EArchiveBatchEntity();

        create(firstContinuous, 10L, EArchiveRequestType.CONTINUOUS);
        create(secondContinuous, 20L, EArchiveRequestType.CONTINUOUS);
        create(firstManual, 30L, EArchiveRequestType.MANUAL);
    }

    @Test
    @Transactional
    public void findEArchiveBatchByBatchId() {
        EArchiveBatchEntity first = eArchiveBatchDao.findEArchiveBatchByBatchEntityId(firstContinuous.getEntityId());
        EArchiveBatchEntity second = eArchiveBatchDao.findEArchiveBatchByBatchEntityId(secondContinuous.getEntityId());
        EArchiveBatchEntity third = eArchiveBatchDao.findEArchiveBatchByBatchEntityId(firstManual.getEntityId());

        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertNotNull(third);
    }

    private void create(EArchiveBatchEntity eArchiveBatch, Long lastPkUserMessage, EArchiveRequestType continuous) {
        eArchiveBatch.setLastPkUserMessage(lastPkUserMessage);
        eArchiveBatch.setRequestType(continuous);
        eArchiveBatchDao.create(eArchiveBatch);
    }

}
