//package eu.domibus.api.multitenancy;
//
//import java.util.concurrent.Callable;
//
///**
// * Wrapper for the Runnable class to be executed. Clear first the domain set on the thread before execution.
// *
// * @author Cosmin Baciu
// * @since 4.0.1
// */
//public class ClearDomainCallable<T> implements Callable<T> {
//
//    protected DomainContextProvider domainContextProvider;
//    protected Callable<T> runnable;
//
//    public ClearDomainCallable(final DomainContextProvider domainContextProvider, final Callable<T> runnable) {
//        this.domainContextProvider = domainContextProvider;
//        this.runnable = runnable;
//    }
//
//    @Override
//    public T call() throws Exception {
//        domainContextProvider.clearCurrentDomain();
//        return runnable.call();
//    }
//}
