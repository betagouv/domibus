package eu.domibus.api.multitenancy;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Wrapper for the Runnable class to be executed. Catches any exception and executes the error handler if defined.
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
public class SetMDCContextTaskRunnable<T> implements Runnable, Callable<T> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SetMDCContextTaskRunnable.class);

    protected Runnable runnable;

    protected Callable<T> callable;

    protected Runnable errorHandler;

    protected Map<String, String> copyOfContextMap;

    public SetMDCContextTaskRunnable(final Runnable runnable, Runnable errorHandler) {
        this(errorHandler);
        this.runnable = runnable;
    }

    public SetMDCContextTaskRunnable(final Callable<T> callable, Runnable errorHandler) {
        this(errorHandler);
        this.callable = callable;
    }

    private SetMDCContextTaskRunnable(Runnable errorHandler) {
        this.errorHandler = errorHandler;
        this.copyOfContextMap = LOG.getCopyOfContextMap();
    }

    @Override
    public void run() {
        try {
            executeTask(this::wrapRunnable);
        } catch (Exception e) {
            throw new DomainTaskException(e);
        }
    }

    private Boolean wrapRunnable() {
        runnable.run();
        return true;
    }

    @Override
    public T call() {
        return executeTask(() -> callable.call());
    }

    private <T> T executeTask(Callable<T> task) {
        T res = null;
        try {
            if (copyOfContextMap != null) {
                LOG.trace("Setting MDC context map");
                LOG.setContextMap(copyOfContextMap);
                LOG.trace("Finished setting MDC context map");
            }

            LOG.trace("Start executing task");
            res = task.call();
            LOG.trace("Finished executing task");
        } catch (Throwable e) {
            LOG.error("Error executing task", e);
            executeErrorHandler(e);
        }
        return res;
    }

    protected void executeErrorHandler(Throwable ex) {
        if (errorHandler == null) {
            LOG.trace("No error handler has been set");
            throw new DomainTaskException("Could not execute task", ex);
        }

        try {
            LOG.trace("Start executing error handler");
            errorHandler.run();
            LOG.trace("Finished executing error handler");
        } catch (Throwable e) {
            LOG.error("Error executing error handler", e);
        }
    }
}
