package eu.domibus.ext.services;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ion Perpegel
 * @since 4.2
 * <p>
 * Abstract class that implements DomibusPropertyManagerExt and delegates its methods to DomibusPropertyExtService
 * Used to derive external property managers that delegate to Domibus property manager. Ex: JmsPluginPropertyManager, DSS PropertyManager
 */
public abstract class DomibusPropertyExtServiceDelegateAbstract implements DomibusPropertyManagerExt {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusPropertyExtServiceDelegateAbstract.class);

    @Autowired
    protected DomibusPropertyExtService domibusPropertyExtService;

    @Autowired
    protected DomainExtService domainExtService;

    @Autowired
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    public abstract Map<String, DomibusPropertyMetadataDTO> getKnownProperties();

    @Override
    public String getKnownPropertyValue(String propertyName) {
        DomibusPropertyMetadataDTO propMeta = getPropertyMetadataSafely(propertyName);
        if (propMeta == null) {
            LOG.warn("Metadata not found for property [{}] so we fallback to core property provider.", propertyName);
            return domibusPropertyExtService.getProperty(propertyName);
        }
        if (propMeta.isStoredGlobally()) {
            return domibusPropertyExtService.getProperty(propertyName);
        }

        LOG.trace("Property [{}] is not stored globally so onGetLocalPropertyValue is called.", propertyName);
        return onGetLocalPropertyValue(propertyName);
    }

    /**
     * Method called for a locally stored property; should be overridden by derived classes for all locally stored properties
     *
     * @param propertyName the name of the property
     * @return the property value
     */
    protected String onGetLocalPropertyValue(String propertyName) {
        LOG.warn("Property [{}] is not stored globally and not handled locally so null was returned.", propertyName);
        return null;
    }

    @Override
    public Integer getKnownIntegerPropertyValue(String propertyName) {
        DomibusPropertyMetadataDTO propMeta = getPropertyMetadataSafely(propertyName);
        if (propMeta == null) {
            LOG.warn("Metadata not found for property [{}] so we fallback to core property provider.", propertyName);
            return domibusPropertyExtService.getIntegerProperty(propertyName);
        }
        if (propMeta.isStoredGlobally()) {
            return domibusPropertyExtService.getIntegerProperty(propertyName);
        }

        LOG.trace("Property [{}] is not stored globally so onGetLocalIntegerPropertyValue is called.", propertyName);
        return onGetLocalIntegerPropertyValue(propertyName, propMeta);
    }

    @Override
    public Boolean getKnownBooleanPropertyValue(String propertyName) {
        DomibusPropertyMetadataDTO propMeta = getPropertyMetadataSafely(propertyName);
        if (propMeta == null) {
            LOG.warn("Metadata not found for property [{}] so we fallback to core property provider.", propertyName);
            return domibusPropertyExtService.getBooleanProperty(propertyName);
        }
        if (propMeta.isStoredGlobally()) {
            return domibusPropertyExtService.getBooleanProperty(propertyName);
        }

        LOG.trace("Property [{}] is not stored globally so onGetLocalBooleanPropertyValue is called.", propertyName);
        return onGetLocalBooleanPropertyValue(propertyName, propMeta);
    }

    /**
     * Method called for a locally stored property; should be overridden by derived classes for all locally stored properties
     *
     * @param propertyName the name of the property
     * @param propMeta     the property metadata
     * @return the property value
     */
    protected Integer onGetLocalIntegerPropertyValue(String propertyName, DomibusPropertyMetadataDTO propMeta) {
        LOG.warn("Property [{}] is not stored globally and not handled locally so 0 was returned.", propertyName);
        return 0;
    }

    /**
     * Method called for a locally stored property; should be overridden by derived classes for all locally stored properties
     *
     * @param propertyName the name of the property
     * @param propMeta     the property metadata
     * @return the property value
     */
    protected Boolean onGetLocalBooleanPropertyValue(String propertyName, DomibusPropertyMetadataDTO propMeta) {
        LOG.warn("Property [{}] is not stored globally and not handled locally so 'null' was returned.", propertyName);
        return null;
    }

    @Override
    public String getKnownPropertyValue(String domainCode, String propertyName) {
        DomibusPropertyMetadataDTO propMeta = getPropertyMetadataSafely(propertyName);
        if (propMeta == null) {
            LOG.warn("Metadata not found for property [{}] so we fallback to core property provider on domain [{}].", propertyName, domainCode);
            final DomainDTO domain = domainExtService.getDomain(domainCode);
            return domibusPropertyExtService.getProperty(domain, propertyName);
        }
        if (propMeta.isStoredGlobally()) {
            final DomainDTO domain = domainExtService.getDomain(domainCode);
            if (domain == null) {
                throw new DomibusPropertyExtException("Could not find domain with code " + domainCode);
            }
            return domibusPropertyExtService.getProperty(domain, propertyName);
        }

        LOG.trace("Property [{}] is not stored globally so onGetLocalPropertyValue is called.", propertyName);
        return onGetLocalPropertyValue(domainCode, propertyName);
    }

    /**
     * Method called for a locally stored property; should be overridden by derived classes for all locally stored properties
     *
     * @param domainCode   the name of the domain
     * @param propertyName the name of the property
     * @return the property value
     */
    protected String onGetLocalPropertyValue(String domainCode, String propertyName) {
        LOG.warn("Property [{}] is not stored globally and not handled locally for domain [{}] so null was returned.", propertyName, domainCode);
        return null;
    }

    @Override
    public void setKnownPropertyValue(String domainCode, String propertyName, String propertyValue, boolean broadcast) {
        if (StringUtils.equals(propertyValue, getKnownPropertyValue(domainCode, propertyName))) {
            LOG.info("The property [{}] has already the value [{}] on domain [{}]; exiting.", propertyName, propertyValue, domainCode);
            return;
        }
        DomibusPropertyMetadataDTO propMeta = getPropertyMetadataIfExists(propertyName);
        if (propMeta.isStoredGlobally()) {
            final DomainDTO domain = domainExtService.getDomain(domainCode);
            domibusPropertyExtService.setProperty(domain, propertyName, propertyValue, broadcast);
        }

        LOG.debug("Property [{}] is not stored globally so onSetLocalPropertyValue is called.", propertyName);
        onSetLocalPropertyValue(domainCode, propertyName, propertyValue, broadcast);
    }

    /**
     * Method called for a locally stored property; should be overridden by derived classes for all locally stored properties
     *
     * @param domainCode    the code of the domain
     * @param propertyName  the name of the property
     * @param propertyValue the value of the property
     */
    protected void onSetLocalPropertyValue(String domainCode, String propertyName, String propertyValue, boolean broadcast) {
        LOG.warn("Property [{}] is not stored globally and not handled locally.", propertyName);
    }

    @Override
    public void setKnownPropertyValue(String propertyName, String propertyValue) {
        if (StringUtils.equals(propertyValue, getKnownPropertyValue(propertyName))) {
            LOG.info("The property [{}] has already the value [{}]; exiting.", propertyName, propertyValue);
            return;
        }
        DomibusPropertyMetadataDTO propMeta = getPropertyMetadataIfExists(propertyName);
        if (propMeta.isStoredGlobally()) {
            domibusPropertyExtService.setProperty(propertyName, propertyValue);
        } else {
            LOG.debug("Property [{}] is not stored globally so onSetLocalPropertyValue is called.", propertyName);
            onSetLocalPropertyValue(propertyName, propertyValue);
        }
    }

    protected abstract String getPropertiesFileName();

    protected String getModulePropertiesHome() {
        return PLUGINS_CONFIG_HOME;
    }

    @Override
    public String getConfigurationFileName() {
        return getModulePropertiesHome() + File.separator + getPropertiesFileName();
    }

    @Override
    public Optional<String> getConfigurationFileName(DomainDTO domain) {
        if (domain == null) {
            throw new DomibusPropertyExtException("Domain cannot be null. Call the method without the domain if this is the intention.");
        }

        if (domibusConfigurationExtService.isSingleTenantAware()) {
            throw new DomibusPropertyExtException("PLease call the method without the domain ST mode");
        }

        String propertyFileName = getDomainConfigurationFileName(domain);
        LOG.debug("Using property file [{}]", propertyFileName);
        return Optional.of(propertyFileName);
    }

    protected final String getDomainConfigurationFileName(DomainDTO domain) {
        return getModulePropertiesHome() + File.separator + DOMAINS_HOME + File.separator + domain.getCode() +
                File.separator + domain.getCode() + '-' + getPropertiesFileName();
    }

    @Override
    public void loadProperties(DomainDTO domain) {
        Optional<String> propFileName = getConfigurationFileName(domain);
        if (!propFileName.isPresent()) {
            LOG.info("No property file name provided for domain [{}]. Exiting.", domain);
            return;
        }
        domibusPropertyExtService.loadProperties(domain, propFileName.get());
    }

    @Override
    public void removeProperties(DomainDTO domain) {
        Optional<String> propFileName = getConfigurationFileName(domain);
        if (!propFileName.isPresent()) {
            LOG.info("No property file name provided for domain [{}]. Exiting.", domain);
            return;
        }
        domibusPropertyExtService.removeProperties(domain, propFileName.get());
    }

    /**
     * Method called for a locally stored property; should be overridden by derived classes for all locally stored properties
     *
     * @param propertyName  the name of the property
     * @param propertyValue the value of the property
     */
    protected void onSetLocalPropertyValue(String propertyName, String propertyValue) {
        LOG.warn("Property [{}] is not stored globally and not handled locally.", propertyName);
    }

    @Override
    public boolean hasKnownProperty(String name) {
        return getKnownProperties().containsKey(name);
    }

    protected DomibusPropertyMetadataDTO getPropertyMetadataIfExists(String propertyName) {
        checkPropertyExists(propertyName);
        return getKnownProperties().get(propertyName);
    }

    protected DomibusPropertyMetadataDTO getPropertyMetadataSafely(String propertyName) {
        if (!hasKnownProperty(propertyName)) {
            return null;
        }
        return getKnownProperties().get(propertyName);
    }

    protected void checkPropertyExists(String propertyName) {
        if (!hasKnownProperty(propertyName)) {
            throw new DomibusPropertyExtException("Unknown property: " + propertyName);
        }
    }

}
