package eu.domibus.plugin.ws.property;

import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.DomibusPropertyExtService;
import eu.domibus.plugin.ws.property.listeners.MtomEnabledChangeListener;
import eu.domibus.plugin.ws.property.listeners.SchemaValidationEnabledChangeListener;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginPropertyManagerTest {

    @Tested
    @Injectable
    WSPluginPropertyManager wsPluginPropertyManager;

    @Injectable
    SchemaValidationEnabledChangeListener schemaValidationEnabledChangeListener;

    @Injectable
    MtomEnabledChangeListener mtomEnabledChangeListener;

    @Injectable
    DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    DomainExtService domainExtService;

    @Injectable
    DomibusConfigurationExtService domibusConfigurationExtService;

    @Test
    public void getKnownProperties() {
        Map<String, DomibusPropertyMetadataDTO> properties = wsPluginPropertyManager.getKnownProperties();
        Assertions.assertTrue(properties.containsKey(WSPluginPropertyManager.SCHEMA_VALIDATION_ENABLED_PROPERTY));
        Assertions.assertTrue(properties.containsKey(WSPluginPropertyManager.MTOM_ENABLED_PROPERTY));
        Assertions.assertTrue(properties.containsKey(WSPluginPropertyManager.PROP_LIST_PENDING_MESSAGES_MAXCOUNT));
        Assertions.assertFalse(properties.containsKey("unknown.property"));
    }
}
