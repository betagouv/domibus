package eu.domibus.api.multitenancy;

import java.util.concurrent.Callable;

/**
 * Wrapper for the Runnable class to be executed. Clear first the domain set on the thread before execution.
 *
 * @author Cosmin Baciu
 * @since 4.0.1
 */
public class ClearDomainRunnable<T> implements Runnable, Callable<T> {

    protected DomainContextProvider domainContextProvider;

    protected Runnable runnable;

    protected Callable<T> callable;

    public ClearDomainRunnable(final DomainContextProvider domainContextProvider, final Runnable runnable) {
        this.domainContextProvider = domainContextProvider;
        this.runnable = runnable;
    }

    public ClearDomainRunnable(final DomainContextProvider domainContextProvider, final Callable<T> callable) {
        this.domainContextProvider = domainContextProvider;
        this.callable = callable;
    }

    @Override
    public void run() {
        domainContextProvider.clearCurrentDomain();
        runnable.run();
    }

    @Override
    public T call() throws Exception {
        domainContextProvider.clearCurrentDomain();
        return callable.call();
    }
}
