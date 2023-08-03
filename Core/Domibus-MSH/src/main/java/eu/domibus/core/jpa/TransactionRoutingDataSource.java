package eu.domibus.core.jpa;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ion Perpegel
 * @since 5.1.1
 *
 * Returns the discriminator value that will be used to choose either the read-write or the read-only JDBC DataSource.
 */
public class TransactionRoutingDataSource
        extends AbstractRoutingDataSource {

    @Nullable
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager
                .isCurrentTransactionReadOnly() ?
                DataSourceType.READ_ONLY :
                DataSourceType.READ_WRITE;
    }
}
