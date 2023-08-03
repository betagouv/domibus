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
 * @since 5.1.1
 */
@Configuration
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
        TransactionRoutingDataSource routingDataSource =
                new TransactionRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(
                DataSourceType.READ_WRITE,
                getReadWriteDataSource(domibusPropertyProvider)
        );

        final String readOnlyDataSourceUrl = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_REPLICA_URL);
        if(StringUtils.isNotBlank(readOnlyDataSourceUrl)) {
            dataSourceMap.put(
                    DataSourceType.READ_ONLY,
                    getReadOnlyDataSource(domibusPropertyProvider)
            );
        } else {
            // fallback to one datasource
            dataSourceMap.put(
                    DataSourceType.READ_ONLY,
                    getReadWriteDataSource(domibusPropertyProvider)
            );
        }

        routingDataSource.setTargetDataSources(dataSourceMap);
        return routingDataSource;
    }

    protected abstract DataSource getReadWriteDataSource(DomibusPropertyProvider domibusPropertyProvider);

    protected abstract DataSource getReadOnlyDataSource(DomibusPropertyProvider domibusPropertyProvider);

}
