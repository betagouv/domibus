package eu.domibus.api.multitenancy.lock;

import eu.domibus.api.multitenancy.DomainTaskException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;

/**
 * Wrapper for the Runnable class to be executed. Attempts to lock via db record and in case it succeeds it runs the wrapped Runnable
 *
 * @author Ion Perpegel
 * @since 5.0
 */
public class SynchronizedRunnable implements Runnable {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SynchronizedRunnable.class);

    private SynchronizationService synchronizationService;
    private String lockKey;
    private Runnable runnable;

    public SynchronizedRunnable(Runnable runnable, String lockKey, SynchronizationService synchronizationService) {
        this.runnable = runnable;
        this.lockKey = lockKey;
        this.synchronizationService = synchronizationService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void run() {
        LOG.trace("Trying to lock [{}]", lockKey);

        String threadName = Thread.currentThread().getName();
        Thread.currentThread().setName(lockKey);

        try {
            // if this blocks, it means that another process has a write lock on the db record
            synchronizationService.acquireLock(lockKey);
            LOG.trace("Acquired lock on key [{}]", lockKey);

            LOG.trace("Start executing task");
            runnable.run();
            LOG.trace("Finished executing task");
        } catch (NoResultException nre) {
            throw new DomainTaskException(String.format("Lock key [%s] not found!", lockKey), nre);
        } catch (LockTimeoutException lte) {
            LOG.warn("[{}] key lock could not be acquired. It is probably used by another process.", lockKey, lte);
        } catch (Exception ex) {
            LOG.error("Error while running synchronized task.", ex);
        }

        Thread.currentThread().setName(threadName);
    }
}
