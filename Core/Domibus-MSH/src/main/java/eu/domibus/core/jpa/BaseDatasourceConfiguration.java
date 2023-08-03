package eu.domibus.core.jpa;

import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

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
        dataSourceMap.put(
                DataSourceType.READ_ONLY,
                getReadOnlyDataSource(domibusPropertyProvider)
        );

        routingDataSource.setTargetDataSources(dataSourceMap);
        return routingDataSource;
    }

    protected abstract DataSource getReadWriteDataSource(DomibusPropertyProvider domibusPropertyProvider);

    protected abstract DataSource getReadOnlyDataSource(DomibusPropertyProvider domibusPropertyProvider);

}
