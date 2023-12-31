package eu.domibus.core.jpa;

import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_DATASOURCE_REPLICA_URL;

/**
 * @author Ion Perpegel
 * @since 5.2
 * <p>
 * Base data source configuration that routes to either read-only or read-write datasources, depending on the readOnly property of a Transaction
 */
public abstract class BaseDatasourceConfiguration {

    @Bean(DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE)
    public DataSource domibusDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        return getActualDatasource(domibusPropertyProvider);
    }

    @Bean
    public DataSource readWriteDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getReadWriteDataSource(domibusPropertyProvider);
    }

    @Bean
    public DataSource readOnlyDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getReadOnlyDataSource(domibusPropertyProvider);
    }

    protected DataSource getActualDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        DataSource readWriteDataSource = getReadWriteDataSource(domibusPropertyProvider);
        dataSourceMap.put(DataSourceType.READ_WRITE, readWriteDataSource);

        final String readOnlyDataSourceUrl = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_REPLICA_URL);
        if (StringUtils.isNotBlank(readOnlyDataSourceUrl)) {
            DataSource readOnlyDataSource = getReadOnlyDataSource(domibusPropertyProvider);
            dataSourceMap.put(DataSourceType.READ_ONLY, readOnlyDataSource);
        } else {
            // fallback to one datasource
            dataSourceMap.put(DataSourceType.READ_ONLY, readWriteDataSource);
        }

        routingDataSource.setTargetDataSources(dataSourceMap);
        return routingDataSource;
    }

    protected abstract DataSource getReadWriteDataSource(DomibusPropertyProvider domibusPropertyProvider);

    protected abstract DataSource getReadOnlyDataSource(DomibusPropertyProvider domibusPropertyProvider);

}
