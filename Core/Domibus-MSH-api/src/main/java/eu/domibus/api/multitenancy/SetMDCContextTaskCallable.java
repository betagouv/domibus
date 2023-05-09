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
public class SetMDCContextTaskCallable<T> implements Callable<T> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SetMDCContextTaskCallable.class);

    protected Callable<T> runnable;
    protected Runnable errorHandler;
    protected Map<String, String> copyOfContextMap;

    public SetMDCContextTaskCallable(final Callable<T> runnable, Runnable errorHandler) {
        this.runnable = runnable;
        this.errorHandler = errorHandler;
        this.copyOfContextMap = LOG.getCopyOfContextMap();
    }

    @Override
    public T call() {
        T res = null;
        try {
            if (copyOfContextMap != null) {
                LOG.trace("Setting MDC context map");
                LOG.setContextMap(copyOfContextMap);
                LOG.trace("Finished setting MDC context map");
            }

            LOG.trace("Start executing task");
            res = runnable.call();
            LOG.trace("Finished executing task");
        } catch (Throwable e) {
            LOG.error("Error executing task", e);
            executeErrorHandler();
        }
        return res;
    }


    protected void executeErrorHandler() {
        if (errorHandler == null) {
            LOG.trace("No error handler has been set");
            return;
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
