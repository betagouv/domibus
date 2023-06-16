package eu.domibus.plugin.fs.property;

import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.ext.services.*;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class FSPluginPropertiesTest {

    @Injectable
    protected PasswordEncryptionExtService pluginPasswordEncryptionService;

    @Injectable
    protected DomainExtService domainExtService;

    @Injectable
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    DomainContextExtService domainContextExtService;

    @Tested
    @Injectable
    protected FSPluginPropertiesMetadataManagerImpl fsPluginPropertiesMetadataManager;

    @Injectable
    DomibusPropertyExtService domibusPropertyExtService;

    @Tested
    FSPluginProperties fsPluginProperties;


    @Test
    public void testHasKnownProperty() {
        final String propertyName = "fsplugin.messages.location";

        final Boolean isKnownProperty = fsPluginPropertiesMetadataManager.hasKnownProperty(propertyName);
        Assertions.assertEquals(true, isKnownProperty);

        final Boolean isKnownFSProperty = fsPluginProperties.hasKnownProperty(propertyName);
        Assertions.assertEquals(true, isKnownFSProperty);
    }


    @Test
    public void testUnknownProperty() {
        final String propertyName = "fsplugin.messages.location.unknown";


        final Boolean isKnownFSProperty = fsPluginProperties.hasKnownProperty(propertyName);
        Assertions.assertEquals(false, isKnownFSProperty);
 
        String value = fsPluginProperties.getKnownPropertyValue("default", propertyName);
        Assertions.assertTrue(StringUtils.isBlank(value));
    }

}
