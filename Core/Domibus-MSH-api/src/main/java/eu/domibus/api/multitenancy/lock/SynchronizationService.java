package eu.domibus.api.multitenancy.lock;

import java.util.concurrent.Callable;

/**
 * @author Ion Perpegel
 * @since 5.2
 */
public interface SynchronizationService {

    public <T> T execute(final Callable<T> task, final String dbLockKey, final Object javaLockKey, final Runnable errorHandler);

}
