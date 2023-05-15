package eu.domibus.api.multitenancy.lock;

import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.Callable;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Configuration
public class DbSynchronizedRunnableFactory {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(DbSynchronizedRunnableFactory.class);

    @Autowired
    DBSynchronizationHelper dbSynchronizationHelper;

    /**
     * Instantiates a SynchronizedRunnable
     * IMPORTANT: Only use with tasks that are short in duration since the locking requires an active database transaction
     * @see DBSynchronizedRunnable
     */
    @Bean(autowireCandidate = false)
    @Scope("prototype")
    public DBSynchronizedRunnable synchronizedRunnable(Runnable runnable, String lockKey) {
        return new DBSynchronizedRunnable(runnable, lockKey, dbSynchronizationHelper);
    }

    @Bean(autowireCandidate = false)
    @Scope("prototype")
    public <T> DBSynchronizedRunnable<T> synchronizedCallable(Callable<T> callable, String lockKey) {
        return new DBSynchronizedRunnable<>(callable, lockKey, dbSynchronizationHelper);
    }
}
