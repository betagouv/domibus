package eu.domibus.property;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO.Type;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO.Usage;
import eu.domibus.ext.services.DomibusPropertyExtServiceDelegateAbstract;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Property manager for integration test; handles locally some properties
 *
 * @author Ion Perpegel
 * @since 5.0
 */
@Component
public class ExternalTestModulePropertyManager extends DomibusPropertyExtServiceDelegateAbstract
        implements DomibusPropertyManagerExt {

    public static final String EXTERNAL_NOT_EXISTENT = "externalModule.notExistent";
    public static final String EXTERNAL_MODULE_EXISTENT_NOT_HANDLED = "externalModule.existent.notHandled";
    public static final String EXTERNAL_MODULE_EXISTENT_LOCALLY_HANDLED = "externalModule.existent.handled.locally";
    public static final String EXTERNAL_MODULE_EXISTENT_GLOBALLY_HANDLED = "externalModule.existent.handled.globally";

    private Map<String, DomibusPropertyMetadataDTO> knownProperties;

    private Map<String, String> knownPropertyValues = new HashMap<>();

    public ExternalTestModulePropertyManager() {
        List<DomibusPropertyMetadataDTO> allProperties = Arrays.asList(
                new DomibusPropertyMetadataDTO(EXTERNAL_MODULE_EXISTENT_NOT_HANDLED, Type.STRING, "ExternalModule", Usage.DOMAIN),
                new DomibusPropertyMetadataDTO(EXTERNAL_MODULE_EXISTENT_LOCALLY_HANDLED, Type.STRING, "ExternalModule", Usage.DOMAIN),
                new DomibusPropertyMetadataDTO(EXTERNAL_MODULE_EXISTENT_GLOBALLY_HANDLED, Type.STRING, "ExternalModule", Usage.DOMAIN)
        );

        knownProperties = allProperties.stream()
                .peek(prop -> prop.setStoredGlobally(false))
                .collect(Collectors.toMap(x -> x.getName(), x -> x));

        knownProperties.get(EXTERNAL_MODULE_EXISTENT_GLOBALLY_HANDLED).setStoredGlobally(true);

        knownPropertyValues.put(EXTERNAL_MODULE_EXISTENT_LOCALLY_HANDLED, EXTERNAL_MODULE_EXISTENT_LOCALLY_HANDLED + ".value");
    }

    @Override
    public Map<String, DomibusPropertyMetadataDTO> getKnownProperties() {
        return knownProperties;
    }

    @Override
    protected String onGetLocalPropertyValue(String domainCode, String propertyName) {
        if (knownPropertyValues.containsKey(propertyName)) {
            return knownPropertyValues.get(propertyName);
        }
        return super.onGetLocalPropertyValue(domainCode, propertyName);
    }

    @Override
    protected void onSetLocalPropertyValue(String domainCode, String propertyName, String propertyValue, boolean broadcast) {
        if (knownPropertyValues.containsKey(propertyName)) {
            knownPropertyValues.put(propertyName, propertyValue);
            return;
        }
        super.onSetLocalPropertyValue(domainCode, propertyName, propertyValue, broadcast);
    }

    @Override
    protected String getPropertiesFileName() {
        return null;
    }

    @Override
    public String getConfigurationFileName() {
        return "external-module.properties";
    }

    @Override
    public Optional<String> getConfigurationFileName(DomainDTO domain) {
        return Optional.of("default-external-module.properties");
    }

    @Override
    public void removeProperties(DomainDTO domain) {

    }
}
