package eu.domibus.core.multitenancy.lock;

import eu.domibus.api.multitenancy.lock.DomibusSynchronizationException;
import eu.domibus.api.multitenancy.lock.SynchronizationService;
import eu.domibus.api.multitenancy.lock.SynchronizedRunnableFactory;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

/**
 * Executes a task with a lock (either a db lock for cluster deployment or a simple java lock otherwise)
 *
 * @author Ion Perpegel
 * @since 5.2
 */
@Service
public class SynchronizationServiceImpl implements SynchronizationService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SynchronizationServiceImpl.class);

    private final DomibusConfigurationService domibusConfigurationService;

    private final SynchronizedRunnableFactory synchronizedRunnableFactory;

    public SynchronizationServiceImpl(DomibusConfigurationService domibusConfigurationService,
                                      SynchronizedRunnableFactory synchronizedRunnableFactory) {
        this.domibusConfigurationService = domibusConfigurationService;
        this.synchronizedRunnableFactory = synchronizedRunnableFactory;
    }

    @Override
    public <T> Callable<T> getSynchronizedCallable(Callable<T> task, String dbLockKey, Object javaLockKey) {
        Callable<T> synchronizedRunnable;
        if (domibusConfigurationService.isClusterDeployment()) {
            synchronizedRunnable = synchronizedRunnableFactory.synchronizedCallable(task, dbLockKey);
        } else {
            synchronizedRunnable = javaSyncCallable(task, javaLockKey);
        }
        return synchronizedRunnable;
    }

    @Override
    public <T> T execute(Callable<T> task, String dbLockKey, Object javaLockKey, Runnable errorHandler) {
        Callable<T> synchronizedRunnable = getSynchronizedCallable(task, dbLockKey, javaLockKey);
        try {
            return synchronizedRunnable.call();
        } catch (Exception e) {
            throw new DomibusSynchronizationException(e);
        }
    }

    private <T> Callable<T> javaSyncCallable(Callable<T> task, Object javaLockKey) {
        return () -> {
            if (javaLockKey != null) {
                synchronized (javaLockKey) {
                    return executeTask(task);
                }
            } else {
                return executeTask(task);
            }
        };
    }

    private <T> T executeTask(Callable<T> task) {
        try {
            LOG.debug("Handling sync execution with java lock.");
            T res = task.call();
            LOG.debug("Finished handling sync execution with java lock.");
            return res;
        } catch (Exception e) {
            throw new DomibusSynchronizationException("Error executing a callable task with java lock.", e);
        }
    }
}
