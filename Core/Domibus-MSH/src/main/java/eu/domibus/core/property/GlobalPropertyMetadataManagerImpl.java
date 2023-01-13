package eu.domibus.core.property;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.common.DomibusCacheConstants;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

import static eu.domibus.api.property.DomibusPropertyMetadata.NAME_SEPARATOR;

/**
 * Helper class involved in managing the property metadata of core or external property managers ( metadata integrator)
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class GlobalPropertyMetadataManagerImpl implements GlobalPropertyMetadataManager {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(GlobalPropertyMetadataManagerImpl.class);

    protected Map<String, DomibusPropertyMetadata> allPropertyMetadataMap;

    protected Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap;

    private volatile boolean internalPropertiesLoaded = false;
    private volatile boolean externalPropertiesLoaded = false;
    private final Object propertyMetadataMapLock = new Object();

    private final List<DomibusPropertyMetadataManagerSPI> propertyMetadataManagers;

    private final List<DomibusPropertyManagerExt> extPropertyManagers;

    private final DomibusPropertyMetadataMapper domibusPropertyMetadataMapper;

    private final NestedPropertiesManager nestedPropertiesManager;

    public GlobalPropertyMetadataManagerImpl(@Autowired(required = false) List<DomibusPropertyMetadataManagerSPI> propertyMetadataManagers,
                                             // needs to be lazy because we do have a conceptual cyclic dependency(and we do not control external modules):
                                             // PropertyProvider->GlobalPropertyMetadataManager->DomibusPropertyManagerExtX
                                             // ->DomibusPropertyExtServiceDelegateAbstract-DomibusPropertyServiceDelegate->DomibusPropertyProvider
                                             @Autowired(required = false) @Lazy List<DomibusPropertyManagerExt> extPropertyManagers,
                                             NestedPropertiesManager nestedPropertiesManager,
                                             DomibusPropertyMetadataMapper domibusPropertyMetadataMapper) {
        this.propertyMetadataManagers = propertyMetadataManagers;
        this.extPropertyManagers = extPropertyManagers;
        this.domibusPropertyMetadataMapper = domibusPropertyMetadataMapper;
        this.nestedPropertiesManager = nestedPropertiesManager;
    }

    @Override
    public Map<String, DomibusPropertyMetadata> getAllProperties() {
        loadExternalPropertiesIfNeeded();
        return allPropertyMetadataMap;
    }

    @Override
    @Cacheable(cacheManager = DomibusCacheConstants.CACHE_MANAGER, value = DomibusLocalCacheService.DOMIBUS_PROPERTY_METADATA_CACHE, key = "#propertyName")
    public DomibusPropertyMetadata getPropertyMetadata(String propertyName) {
        loadPropertiesIfNotFound(propertyName);

        DomibusPropertyMetadata prop = allPropertyMetadataMap.get(propertyName);
        if (prop != null) {
            LOG.trace("Found property [{}], returning its metadata.", propertyName);
            return prop;
        }

        synchronized (propertyMetadataMapLock) {
            LOG.trace("Acquired lock to search new property: [{}]", propertyName);

            // try to see if it is a compose-able property, i.e. propertyName+suffix
            DomibusPropertyMetadata propMeta = getComposablePropertyMetadata(allPropertyMetadataMap, propertyName);
            if (propMeta != null) {
                LOG.trace("Found compose-able property [{}], returning its metadata.", propertyName);
                DomibusPropertyMetadata newPropMeta = clonePropertyMetadata(propertyName, propMeta);
                newPropMeta.setComposable(false);
                return newPropMeta;
            }

            // if still not found, initialize metadata on-the-fly
            LOG.warn("Cannot find property metadata for property [{}]. Creating on-the-fly metadata for it.", propertyName);
            DomibusPropertyMetadata newProp = DomibusPropertyMetadata.getOnTheFlyProperty(propertyName);
            return clonePropertyMetadata(propertyName, newProp);
        }
    }

    @Override
    public DomibusPropertyManagerExt getManagerForProperty(String propertyName) throws DomibusPropertyException {
        loadPropertiesIfNotFound(propertyName);
        if (hasProperty(internalPropertyMetadataMap, propertyName)) {
            LOG.trace("Property [{}] is internal, returning null as the manager (no external manager involved).", propertyName);
            return null;
        }

        Optional<DomibusPropertyManagerExt> found = extPropertyManagers.stream()
                .filter(manager -> manager.hasKnownProperty(propertyName)).findFirst();

        if (found.isPresent()) {
            LOG.trace("Property [{}] is external, returning its manager [{}].", propertyName, found.get());
            return found.get();
        }

        throw new DomibusPropertyException("Property " + propertyName + " could not be found anywhere.");
    }

    @Override
    public boolean hasKnownProperty(String propertyName) {
        return hasProperty(getAllProperties(), propertyName);
    }

    @Override
    public DomibusPropertyMetadata getComposableProperty(String propertyName) {
        return getComposablePropertyMetadata(getAllProperties(), propertyName);
    }

    protected boolean hasProperty(Map<String, DomibusPropertyMetadata> map, String propertyName) {
        return map.containsKey(propertyName)
                || hasComposableProperty(map, propertyName);
    }

    /**
     * Checks if the given property name corresponds to a composable property with a declared value in property bag
     *
     * @param map          the map with proerty metadata
     * @param propertyName the name of the property
     * @return true if it is
     */
    protected boolean hasComposableProperty(Map<String, DomibusPropertyMetadata> map, String propertyName) {
        synchronized (propertyMetadataMapLock) {
            LOG.trace("Acquired lock to search new property: [{}]", propertyName);
            DomibusPropertyMetadata propMeta = getComposablePropertyMetadata(map, propertyName);
            if (propMeta == null) {
                LOG.trace("Could not find composable property metadata for [{}].", propertyName);
                return false;
            }
            List<String> props = nestedPropertiesManager.getNestedProperties(propMeta);
            if (CollectionUtils.isEmpty(props)) {
                LOG.trace("Could not find any nested properties for [{}].", propMeta.getName());
                return false;
            }
            return props.stream().anyMatch(prop -> propertyName.equals(propMeta.getName() + NAME_SEPARATOR + prop));
        }
    }

    protected DomibusPropertyMetadata getComposablePropertyMetadata(Map<String, DomibusPropertyMetadata> map, String propertyName) {
        return map.values().stream()
                .filter(Objects::nonNull)
                .filter(propertyMetadata -> propertyMetadata.isComposable())
                .filter(propertyMetadata -> propertyName.startsWith(propertyMetadata.getName() + NAME_SEPARATOR))
                .findAny()
                .orElse(null);
    }

    /**
     * Initializes the metadata map with internal and/or external properties,
     * until the given property is found.
     *
     * @param propertyName the name of the property to search for
     * @implNote Initially, during the bean creation stage, only a few domibus-core properties are needed;
     * later on, the properties from all managers will be added to the map.
     */
    protected void loadPropertiesIfNotFound(String propertyName) {
        // We add domibus-core and specific server properties first to avoid infinite loop of bean creation (due to DB properties)
        if (internalPropertyMetadataMap == null) {
            synchronized (propertyMetadataMapLock) {
                if (!internalPropertiesLoaded) { // double-check locking
                    LOG.trace("Initializing core properties");

                    loadInternalProperties();

                    LOG.trace("Finished loading property metadata for internal property managers.");
                    internalPropertiesLoaded = true;
                }
            }
        }
        if (hasProperty(allPropertyMetadataMap, propertyName)) {
            LOG.trace("Found property metadata [{}] in core properties. Returning.", propertyName);
            return;
        }

        loadExternalPropertiesIfNeeded();
    }

    /**
     * Loads external properties (i.e. plugin properties and extension properties)
     * the first time one of them is needed
     */
    protected void loadExternalPropertiesIfNeeded() {
        if (!externalPropertiesLoaded) {
            synchronized (propertyMetadataMapLock) {
                if (!externalPropertiesLoaded) { // double-check locking
                    LOG.trace("Initializing external properties");

                    loadExternalProperties();

                    externalPropertiesLoaded = true;
                    LOG.trace("Finished loading property metadata for external property managers.");
                }
            }
        }
    }

    protected void loadInternalProperties() {
        allPropertyMetadataMap = new HashMap<>();
        internalPropertyMetadataMap = new HashMap<>();

        propertyMetadataManagers.forEach(propertyManager -> loadProperties(propertyManager, propertyManager.toString()));
    }

    protected void loadExternalProperties() {
        extPropertyManagers.forEach(this::loadExternalProperties);
    }

    protected void loadExternalProperties(DomibusPropertyManagerExt propertyManager) {
        LOG.trace("Loading property metadata for [{}] external property manager.", propertyManager);
        for (Map.Entry<String, DomibusPropertyMetadataDTO> entry : propertyManager.getKnownProperties().entrySet()) {
            DomibusPropertyMetadataDTO extProp = entry.getValue();
            DomibusPropertyMetadata domibusProp = domibusPropertyMetadataMapper.propertyMetadataDTOTopropertyMetadata(extProp);
            allPropertyMetadataMap.put(entry.getKey(), domibusProp);
        }
    }

    protected void loadProperties(DomibusPropertyMetadataManagerSPI propertyManager, String managerName) {
        LOG.trace("Loading property metadata for [{}] property manager.", managerName);
        for (Map.Entry<String, DomibusPropertyMetadata> entry : propertyManager.getKnownProperties().entrySet()) {
            DomibusPropertyMetadata prop = entry.getValue();
            allPropertyMetadataMap.put(entry.getKey(), prop);
            internalPropertyMetadataMap.put(entry.getKey(), prop);
        }
    }

    protected DomibusPropertyMetadata clonePropertyMetadata(String propertyName, DomibusPropertyMetadata propMeta) {
        // make a clone and then add it to the map
        DomibusPropertyMetadata newPropMeta = domibusPropertyMetadataMapper.clonePropertyMetadata(propMeta);
        // metadata name may be just the prefix, not be the concrete propertyName,
        // so we set the whole property name here to be correctly used down the stream. Not beautiful
        newPropMeta.setName(propertyName);

        allPropertyMetadataMap.put(propertyName, newPropMeta);
        internalPropertyMetadataMap.put(propertyName, newPropMeta);

        return newPropMeta;
    }

}
