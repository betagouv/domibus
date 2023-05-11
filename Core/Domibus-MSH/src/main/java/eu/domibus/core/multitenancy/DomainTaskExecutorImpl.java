package eu.domibus.core.multitenancy;

import eu.domibus.api.multitenancy.*;
import eu.domibus.api.multitenancy.lock.SynchronizedRunnable;
import eu.domibus.api.multitenancy.lock.SynchronizedRunnableFactory;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.core.crypto.spi.CryptoSpiException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

import static eu.domibus.common.TaskExecutorConstants.DOMIBUS_LONG_RUNNING_TASK_EXECUTOR_BEAN_NAME;
import static eu.domibus.common.TaskExecutorConstants.DOMIBUS_TASK_EXECUTOR_BEAN_NAME;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Service
public class DomainTaskExecutorImpl implements DomainTaskExecutor {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomainTaskExecutorImpl.class);
    
    public static final long DEFAULT_WAIT_TIMEOUT_IN_SECONDS = 60L;
    
    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Qualifier(DOMIBUS_TASK_EXECUTOR_BEAN_NAME)
    @Autowired
    protected SchedulingTaskExecutor schedulingTaskExecutor;

    @Qualifier(DOMIBUS_LONG_RUNNING_TASK_EXECUTOR_BEAN_NAME)
    @Autowired
    protected SchedulingTaskExecutor schedulingLongTaskExecutor;

    @Autowired
    SynchronizedRunnableFactory synchronizedRunnableFactory;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;
    
    @Override
    public <T extends Object> T submit(Callable<T> task) {
        DomainCallable domainCallable = new DomainCallable(domainContextProvider, task);
        final Future<T> utrFuture = schedulingTaskExecutor.submit(domainCallable);
        try {
            return utrFuture.get(DEFAULT_WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new DomainTaskException("Could not execute task", e);
        }
    }

    @Override
    public <T extends Object> T submit(Callable<T> task, Domain domain) {
        DomainCallable domainCallable = new DomainCallable(domainContextProvider, task, domain);
        final Future<T> utrFuture = schedulingTaskExecutor.submit(domainCallable);
        try {
            return utrFuture.get(DEFAULT_WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Restore interrupted state
            Thread.currentThread().interrupt();
            throw new DomainTaskException("Could not execute task", e);
        }
    }

    @Override
    public void submit(Runnable task) {
        LOG.trace("Submitting task");
        final ClearDomainRunnable clearDomainRunnable = new ClearDomainRunnable(domainContextProvider, task);
        submitRunnable(schedulingTaskExecutor, clearDomainRunnable, true, DEFAULT_WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public Future<?> submit(Runnable task, boolean waitForTask) {
        LOG.trace("Submitting task, waitForTask [{}]", waitForTask);
        final ClearDomainRunnable clearDomainRunnable = new ClearDomainRunnable(domainContextProvider, task);
        return submitRunnable(schedulingTaskExecutor, clearDomainRunnable, waitForTask, DEFAULT_WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void submit(Runnable task, Runnable errorHandler, String lockKey) {
        submit(task, errorHandler, lockKey, true, DEFAULT_WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void submit(Runnable task, Runnable errorHandler, String lockKey, boolean waitForTask, Long timeout, TimeUnit timeUnit) {
        LOG.trace("Submitting task with lock file [{}], timeout [{}] expressed in unit [{}]", lockKey, timeout, timeUnit);

        Runnable synchronizedRunnable = synchronizedRunnableFactory.synchronizedRunnable(task, lockKey);

        Runnable setMDCContextTaskRunnable = new SetMDCContextTaskRunnable(synchronizedRunnable, errorHandler);
        final Runnable clearDomainRunnable = new ClearDomainRunnable(domainContextProvider, setMDCContextTaskRunnable);

        submitRunnable(schedulingTaskExecutor, clearDomainRunnable, errorHandler, waitForTask, timeout, timeUnit);
    }

    @Override
    public <T> T submit(Callable<T> task, Runnable errorHandler, String lockKey, Long timeout, TimeUnit timeUnit) {
        LOG.trace("Submitting task with lock file [{}], timeout [{}] expressed in unit [{}]", lockKey, timeout, timeUnit);

        Callable<T> synchronizedRunnable = synchronizedRunnableFactory.synchronizedCallable(task, lockKey);

        Callable<T> setMDCContextTaskRunnable = new SetMDCContextTaskRunnable<T>(synchronizedRunnable, errorHandler);
        final Callable<T> clearDomainRunnable = new ClearDomainRunnable<T>(domainContextProvider, setMDCContextTaskRunnable);

        return submitCallable(schedulingTaskExecutor, clearDomainRunnable, errorHandler, timeout, timeUnit);
    }

    @Override
    public void submit(Runnable task, Domain domain) {
        submit(schedulingTaskExecutor, task, domain, true, DEFAULT_WAIT_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void submit(Runnable task, Domain domain, boolean waitForTask, Long timeout, TimeUnit timeUnit) {
        submit(schedulingTaskExecutor, task, domain, waitForTask, timeout, timeUnit);
    }

    @Override
    public void submitLongRunningTask(Runnable task, Domain domain) {
        submitLongRunningTask(task, null, domain);
    }

    @Override
    public void submitLongRunningTask(Runnable task, Runnable errorHandler, Domain domain) {
        submit(schedulingLongTaskExecutor, new SetMDCContextTaskRunnable(task, errorHandler), domain, false, null, null);
    }

    @Override
    public <R> R executeWithLock(Callable<R> task, String dbLockKey, Object javaLockKey) {
        if (domibusConfigurationService.isClusterDeployment()) {
            LOG.debug("Handling execution using db lock.");
            try {
                R res = submit(task, null, dbLockKey, 3L, TimeUnit.MINUTES);
                LOG.debug("Finished handling execution using db lock.");
                return res;
            } catch (DomainTaskException ex) {
                throw new CryptoSpiException(ex.getCause());
            }
        } else {
            LOG.debug("Handling execution with java lock.");
            synchronized (javaLockKey) {
                try {
                    R res = task.call();
                    LOG.debug("Finished handling execution with java lock.");
                    return res;
                } catch (Exception e) {
                    throw new CryptoSpiException(e);
                }
            }
        }
    }
    
    protected Future<?> submit(SchedulingTaskExecutor taskExecutor, Runnable task, Domain domain, boolean waitForTask, Long timeout, TimeUnit timeUnit) {
        LOG.trace("Submitting task for domain [{}]", domain);

        final DomainRunnable domainRunnable = new DomainRunnable(domainContextProvider, domain, task);
        Future<?> utrFuture = submitRunnable(taskExecutor, domainRunnable, waitForTask, timeout, timeUnit);

        LOG.trace("Completed task for domain [{}]", domain);

        return utrFuture;
    }

    protected Future<?> submitRunnable(SchedulingTaskExecutor taskExecutor, Runnable task, boolean waitForTask, Long timeout, TimeUnit timeUnit) {
        return submitRunnable(taskExecutor, task, null, waitForTask, timeout, timeUnit);
    }

    protected Future<?> submitRunnable(SchedulingTaskExecutor taskExecutor, Runnable task, Runnable errorHandler, boolean waitForTask, Long timeout, TimeUnit timeUnit) {
        final Future<?> utrFuture = taskExecutor.submit(task);

        if (waitForTask) {
            LOG.debug("Waiting for task to complete");
            try {
                utrFuture.get(timeout, timeUnit);
                LOG.debug("Task completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handleRunnableError(e, errorHandler);
            } catch (ExecutionException | TimeoutException e) {
                handleRunnableError(e, errorHandler);
            }
        }
        return utrFuture;
    }

    protected <T> T submitCallable(SchedulingTaskExecutor taskExecutor, Callable<T> task, Runnable errorHandler, Long timeout, TimeUnit timeUnit) {
        final Future<T> utrFuture = taskExecutor.submit(task);

        LOG.debug("Waiting for task to complete");
        T res = null;
        try {
            res = utrFuture.get(timeout, timeUnit);
            LOG.debug("Task completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleRunnableError(e, errorHandler);
        } catch (TimeoutException e) {
            handleRunnableError(e, errorHandler);
        } catch (ExecutionException e) {
            handleRunnableError(e.getCause(), errorHandler);
        }
        return res;
    }
    
    protected void handleRunnableError(Throwable exception, Runnable errorHandler) {
        if (errorHandler != null) {
            LOG.debug("Running the error handler", exception);
            errorHandler.run();
            return;
        }

        throw new DomainTaskException("Could not execute task", exception);
    }
}
