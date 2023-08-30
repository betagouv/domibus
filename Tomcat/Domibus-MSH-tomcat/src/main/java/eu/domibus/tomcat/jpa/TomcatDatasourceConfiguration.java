package eu.domibus.tomcat.jpa;

import com.zaxxer.hikari.HikariDataSource;
import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.jpa.BaseDatasourceConfiguration;
import eu.domibus.tomcat.environment.NoH2DatabaseCondition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_SECOND;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Conditional(NoH2DatabaseCondition.class)
@Configuration
public class TomcatDatasourceConfiguration extends BaseDatasourceConfiguration {

    @Override
    protected DataSource getReadWriteDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getDefaultDataSource(domibusPropertyProvider);
    }

    @Override
    protected DataSource getReadOnlyDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getReplicaDataSource(domibusPropertyProvider);
    }

    @Bean(name = DataSourceConstants.DOMIBUS_JDBC_QUARTZ_DATA_SOURCE, destroyMethod = "close")
    public DataSource quartzDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        return getDefaultDataSource(domibusPropertyProvider);
    }

    private HikariDataSource getDefaultDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getHikariDataSource(domibusPropertyProvider, DOMIBUS_DATASOURCE_DRIVER_CLASS_NAME, DOMIBUS_DATASOURCE_URL, DOMIBUS_DATASOURCE_USER, DOMIBUS_DATASOURCE_PASSWORD,
                DOMIBUS_DATASOURCE_MAX_LIFETIME, DOMIBUS_DATASOURCE_MAX_POOL_SIZE, DOMIBUS_DATASOURCE_CONNECTION_TIMEOUT, DOMIBUS_DATASOURCE_IDLE_TIMEOUT,
                DOMIBUS_DATASOURCE_MINIMUM_IDLE, DOMIBUS_DATASOURCE_POOL_NAME);
    }

    private HikariDataSource getReplicaDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        return getHikariDataSource(domibusPropertyProvider, DOMIBUS_DATASOURCE_REPLICA_DRIVER_CLASS_NAME, DOMIBUS_DATASOURCE_REPLICA_URL, DOMIBUS_DATASOURCE_REPLICA_USER,
                DOMIBUS_DATASOURCE_REPLICA_PASSWORD, DOMIBUS_DATASOURCE_REPLICA_MAX_LIFETIME, DOMIBUS_DATASOURCE_REPLICA_MAX_POOL_SIZE, DOMIBUS_DATASOURCE_REPLICA_CONNECTION_TIMEOUT,
                DOMIBUS_DATASOURCE_REPLICA_IDLE_TIMEOUT, DOMIBUS_DATASOURCE_REPLICA_MINIMUM_IDLE, DOMIBUS_DATASOURCE_REPLICA_POOL_NAME);
    }

    private HikariDataSource getHikariDataSource(DomibusPropertyProvider domibusPropertyProvider, String domibusDatasourceReplicaDriverClassName, String domibusDatasourceReplicaUrl, String domibusDatasourceReplicaUser, String domibusDatasourceReplicaPassword, String domibusDatasourceReplicaMaxLifetime, String domibusDatasourceReplicaMaxPoolSize, String domibusDatasourceReplicaConnectionTimeout, String domibusDatasourceReplicaIdleTimeout, String domibusDatasourceReplicaMinimumIdle, String domibusDatasourceReplicaPoolName) {
        HikariDataSource dataSource = new HikariDataSource();

        final String driverClassName = domibusPropertyProvider.getProperty(domibusDatasourceReplicaDriverClassName);
        dataSource.setDriverClassName(driverClassName);

        final String dataSourceURL = domibusPropertyProvider.getProperty(domibusDatasourceReplicaUrl);
        dataSource.setJdbcUrl(dataSourceURL);

        final String user = domibusPropertyProvider.getProperty(domibusDatasourceReplicaUser);
        dataSource.setUsername(user);

        final String password = domibusPropertyProvider.getProperty(domibusDatasourceReplicaPassword); //NOSONAR
        dataSource.setPassword(password);

        final Integer maxLifetimeInSecs = domibusPropertyProvider.getIntegerProperty(domibusDatasourceReplicaMaxLifetime);
        dataSource.setMaxLifetime(maxLifetimeInSecs * MILLIS_PER_SECOND);

        final Integer maxPoolSize = domibusPropertyProvider.getIntegerProperty(domibusDatasourceReplicaMaxPoolSize);
        dataSource.setMaximumPoolSize(maxPoolSize);

        final Integer connectionTimeout = domibusPropertyProvider.getIntegerProperty(domibusDatasourceReplicaConnectionTimeout);
        dataSource.setConnectionTimeout(connectionTimeout * MILLIS_PER_SECOND);

        final Integer idleTimeout = domibusPropertyProvider.getIntegerProperty(domibusDatasourceReplicaIdleTimeout);
        dataSource.setIdleTimeout(idleTimeout * MILLIS_PER_SECOND);

        final Integer minimumIdle = domibusPropertyProvider.getIntegerProperty(domibusDatasourceReplicaMinimumIdle);
        dataSource.setMinimumIdle(minimumIdle);

        final String poolName = domibusPropertyProvider.getProperty(domibusDatasourceReplicaPoolName);
        if (!StringUtils.isBlank(poolName)) {
            dataSource.setPoolName(poolName);
        }

        return dataSource;
    }
}
