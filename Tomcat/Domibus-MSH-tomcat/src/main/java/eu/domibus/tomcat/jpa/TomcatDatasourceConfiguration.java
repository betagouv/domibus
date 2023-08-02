package eu.domibus.tomcat.jpa;

import com.zaxxer.hikari.HikariDataSource;
import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.jpa.DataSourceType;
import eu.domibus.core.jpa.TransactionRoutingDataSource;
import eu.domibus.tomcat.environment.NoH2DatabaseCondition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_SECOND;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Conditional(NoH2DatabaseCondition.class)
@Configuration
public class TomcatDatasourceConfiguration {

    @Bean
    public BeanPostProcessor dialectProcessor() {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HibernateJpaVendorAdapter) {
                    ((HibernateJpaVendorAdapter) bean).getJpaDialect().setPrepareConnection(false);
                }
                return bean;
            }
        };
    }

    @Bean(DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE)
    public DataSource domibusDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        // this code can be moved in common core module??
        TransactionRoutingDataSource routingDataSource =
                new TransactionRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(
                DataSourceType.READ_WRITE,
                readWriteDataSource(domibusPropertyProvider)
        );
        dataSourceMap.put(
                DataSourceType.READ_ONLY,
                readOnlyDataSource(domibusPropertyProvider)
        );

        routingDataSource.setTargetDataSources(dataSourceMap);
        return routingDataSource;
    }

    @Bean
    public DataSource readWriteDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        final String dataSourceURL = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_URL);
        return getHikariDataSource(domibusPropertyProvider, dataSourceURL);
    }

    @Bean
    public DataSource readOnlyDataSource(DomibusPropertyProvider domibusPropertyProvider) {
        final String dataSourceURL = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_REPLICA_URL);
        return getHikariDataSource(domibusPropertyProvider, dataSourceURL);
    }

    @Bean(name = DataSourceConstants.DOMIBUS_JDBC_QUARTZ_DATA_SOURCE, destroyMethod = "close")
    public DataSource quartzDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        final String dataSourceURL = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_URL);
        return getHikariDataSource(domibusPropertyProvider, dataSourceURL);
    }

    private HikariDataSource getHikariDataSource(DomibusPropertyProvider domibusPropertyProvider, String dataSourceURL) {
        HikariDataSource dataSource = new HikariDataSource();
        final String driverClassName = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_DRIVER_CLASS_NAME);
        dataSource.setDriverClassName(driverClassName);

        dataSource.setJdbcUrl(dataSourceURL);

        final String user = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_USER);
        dataSource.setUsername(user);

        final String password = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_PASSWORD); //NOSONAR
        dataSource.setPassword(password);

        final Integer maxLifetimeInSecs = domibusPropertyProvider.getIntegerProperty(DOMIBUS_DATASOURCE_MAX_LIFETIME);
        dataSource.setMaxLifetime(maxLifetimeInSecs * MILLIS_PER_SECOND);

        final Integer maxPoolSize = domibusPropertyProvider.getIntegerProperty(DOMIBUS_DATASOURCE_MAX_POOL_SIZE);
        dataSource.setMaximumPoolSize(maxPoolSize);

        final Integer connectionTimeout = domibusPropertyProvider.getIntegerProperty(DOMIBUS_DATASOURCE_CONNECTION_TIMEOUT);
        dataSource.setConnectionTimeout(connectionTimeout * MILLIS_PER_SECOND);

        final Integer idleTimeout = domibusPropertyProvider.getIntegerProperty(DOMIBUS_DATASOURCE_IDLE_TIMEOUT);
        dataSource.setIdleTimeout(idleTimeout * MILLIS_PER_SECOND);

        final Integer minimumIdle = domibusPropertyProvider.getIntegerProperty(DOMIBUS_DATASOURCE_MINIMUM_IDLE);
        dataSource.setMinimumIdle(minimumIdle);

        final String poolName = domibusPropertyProvider.getProperty(DOMIBUS_DATASOURCE_POOL_NAME);
        if (!StringUtils.isBlank(poolName)) {
            dataSource.setPoolName(poolName);
        }

        return dataSource;
    }
}
