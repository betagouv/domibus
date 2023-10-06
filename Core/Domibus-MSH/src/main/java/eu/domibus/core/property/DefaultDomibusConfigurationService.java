package eu.domibus.core.property;

import eu.domibus.api.cache.CacheConstants;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.*;
import eu.domibus.common.DomibusCacheConstants;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.File;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_DIALECT;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Component
public class DefaultDomibusConfigurationService implements DomibusConfigurationService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DefaultDomibusConfigurationService.class);

    private static final String DATABASE_DIALECT = DOMIBUS_ENTITY_MANAGER_FACTORY_JPA_PROPERTY_HIBERNATE_DIALECT;

    private DataBaseEngine dataBaseEngine;

    // this is intentionally not domibusPropertyProvider: to cut the cyclic dependency
    private final PropertyRetrieveManager propertyRetrieveManager;

    // this is intentionally not domibusPropertyProvider: to cut the cyclic dependency
    private final PrimitivePropertyTypesManager primitivePropertyTypesManager;

    public DefaultDomibusConfigurationService(PropertyRetrieveManager propertyRetrieveManager,
                                              PrimitivePropertyTypesManager primitivePropertyTypesManager) {
        this.propertyRetrieveManager = propertyRetrieveManager;
        this.primitivePropertyTypesManager = primitivePropertyTypesManager;
    }

    @Override
    public String getConfigLocation() {
        return propertyRetrieveManager.getInternalProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_CONFIG_LOCATION);
    }

    @Cacheable(cacheManager = DomibusCacheConstants.CACHE_MANAGER, value = "multitenantCache")
    @Override
    public boolean isMultiTenantAware() {
        String internalProperty = propertyRetrieveManager.getInternalProperty(DomainService.GENERAL_SCHEMA_PROPERTY);
        return StringUtils.isNotBlank(internalProperty);
    }

    @Override
    public boolean isSingleTenantAware() {
        return !isMultiTenantAware();
    }

    @Override
    public boolean isClusterDeployment() {
        return getBooleanProperty(CLUSTER_DEPLOYMENT);
    }

    @Override
    public DataBaseEngine getDataBaseEngine() {
        if (dataBaseEngine == null) {
            final String property = propertyRetrieveManager.getInternalProperty(DATABASE_DIALECT);
            if (property == null) {
                throw new IllegalStateException("Database dialect not configured, please set property: domibus.entityManagerFactory.jpaProperty.hibernate.dialect");
            }
            dataBaseEngine = DataBaseEngine.getDatabaseEngine(property);
            LOG.debug("Database engine:[{}]", dataBaseEngine);
        }
        return dataBaseEngine;
    }

    @Override
    public boolean isExtAuthProviderEnabled() {
        return getBooleanProperty(EXTERNAL_AUTH_PROVIDER);
    }

    @Override
    public boolean isPayloadEncryptionActive(Domain domain) {
        return getBooleanProperty(domain, PAYLOAD_ENCRYPTION_PROPERTY);
    }

    @Override
    public boolean isPasswordEncryptionActive() {
        return getBooleanProperty(PASSWORD_ENCRYPTION_ACTIVE_PROPERTY);
    }

    @Override
    public boolean isPasswordEncryptionActive(Domain domain) {
        return getBooleanProperty(domain, PASSWORD_ENCRYPTION_ACTIVE_PROPERTY);
    }

    @Override
    public String getConfigurationFileName() {
        return DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;
    }

    @Override
    public String getConfigurationFileName(Domain domain) {
        String propertyFileName;
        if (domain == null) {
            throw new DomibusPropertyException("Cannot call this method with null domain.");
        }
        if (isSingleTenantAware()) {
            propertyFileName = getConfigurationFileName();
        } else {
            propertyFileName = getDomainConfigurationFileName(domain);
        }
        LOG.debug("Using property file [{}]", propertyFileName);

        return propertyFileName;
    }

    @Override
    public String getSuperConfigurationFileName() {
        return DomainService.DOMAINS_HOME + File.separator + "super-" + DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;
    }

    protected String getDomainConfigurationFileName(Domain domain) {
        return DomainService.DOMAINS_HOME + File.separator + domain.getCode() +
                File.separator + domain.getCode() + '-' + DomibusPropertyProvider.DOMIBUS_PROPERTY_FILE;
    }

    protected Boolean getBooleanProperty(Domain domain, String propertyName) {
        String domainValue = propertyRetrieveManager.getInternalProperty(domain, propertyName);
        return primitivePropertyTypesManager.getBooleanInternal(propertyName, domainValue);
    }

    protected Boolean getBooleanProperty(String propertyName) {
        String value = propertyRetrieveManager.getInternalProperty(propertyName);
        return primitivePropertyTypesManager.getBooleanInternal(propertyName, value);
    }
}
