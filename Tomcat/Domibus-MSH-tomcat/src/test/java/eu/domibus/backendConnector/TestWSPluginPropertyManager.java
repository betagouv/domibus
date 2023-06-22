package eu.domibus.backendConnector;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO.Type;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO.Usage;
import eu.domibus.ext.services.DomibusPropertyExtServiceDelegateAbstract;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static eu.domibus.backendConnector.TestWSPluginMock.TEST_WS_PLUGIN;

/**
 * Property manager for integration test; handles locally some properties
 *
 * @author Ion Perpegel
 * @since 5.2
 */
@Component
public class TestWSPluginPropertyManager extends DomibusPropertyExtServiceDelegateAbstract
        implements DomibusPropertyManagerExt {

    public static final String TEST_WSPLUGIN_DOMAIN_ENABLED = "testWSPlugin.domain.enabled";

    private Map<String, DomibusPropertyMetadataDTO> knownProperties;

    public TestWSPluginPropertyManager() {
        List<DomibusPropertyMetadataDTO> allProperties = Arrays.asList(
                new DomibusPropertyMetadataDTO(TEST_WSPLUGIN_DOMAIN_ENABLED, Type.BOOLEAN, TEST_WS_PLUGIN, Usage.DOMAIN)
        );

        knownProperties = allProperties.stream()
                .collect(Collectors.toMap(x -> x.getName(), x -> x));

    }

    @Override
    public Map<String, DomibusPropertyMetadataDTO> getKnownProperties() {
        return knownProperties;
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
