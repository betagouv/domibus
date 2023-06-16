package eu.domibus.plugin.fs.property;

import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.core.property.DefaultDomibusConfigurationService;
import eu.domibus.core.property.PropertyProviderHelper;
import eu.domibus.core.property.PropertyRetrieveManager;
import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.test.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.LOCATION;

/**
 * @author Catalin Enache
 * @since 5.0
 */
public class FSPluginPropertiesMultitenantIT extends AbstractIT {

    private static final String DOMAIN1 = "default";
    private static final String DOMAIN2 = "red";
    private static final String NONEXISTENT_DOMAIN = "NONEXISTENT_DOMAIN";

    private static final String DOMAIN1_LOCATION = "/tmp/fs_plugin_data/default";

    @Autowired
    FSPluginPropertiesMetadataManagerImpl fsPluginPropertiesMetadataManager;

    @Autowired
    FSPluginProperties fsPluginProperties;
    @Autowired
    PropertyProviderHelper propertyProviderHelper;

    @Autowired
    DomainExtService domainExtService;
    @Autowired
    DomainService domainService;
    @Autowired
    PropertyRetrieveManager propertyRetrieveManager;
    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;
    @Autowired
    DomibusLocalCacheService domibusLocalCacheService;
    @Autowired
    DefaultDomibusConfigurationService defaultDomibusConfigurationService;

    @Configuration
    @PropertySource(value = "file:${domibus.config.location}/dataset/fsplugin/fs-plugin.properties")
    // as this test file is added last to property sources and
    // contrary to implemented properties mechanism which allow to override values from the default file
    // fs-plugin-default.properties takes precedence here so we will test both files values
    static class ContextConfiguration {
    }

    @Test
    public void testGetLocation_Domain1() {
        String location = fsPluginProperties.getLocation(DOMAIN1);
        Assertions.assertEquals(DOMAIN1_LOCATION, location);
    }

    @Test
    public void testGetLocation_NonExistentDomain() {
        try {
            fsPluginProperties.getLocation(NONEXISTENT_DOMAIN);
            Assertions.fail("Exception expected");
        } catch (DomibusPropertyExtException e) {
            Assertions.assertTrue(e.getMessage().contains("Could not find domain with code"));
        }
    }

    @Test
    public void testGetUser() {
        String user = fsPluginProperties.getUser(DOMAIN1);
        Assertions.assertEquals("user1", user);
    }

    @Test
    public void testGetPassword() {
        Assertions.assertEquals("pass1", fsPluginProperties.getPassword(DOMAIN1));
    }

    @Test
    public void testGetUser_NotSecured() {
        Assertions.assertEquals("", fsPluginProperties.getUser(DOMAIN2));
    }

    @Test
    public void testGetPayloadId_Domain() {
        Assertions.assertEquals("cid:message", fsPluginProperties.getPayloadId(DOMAIN1));
    }

    @Test
    public void testGetPassword_NotSecured() {
        Assertions.assertEquals("", fsPluginProperties.getPassword(DOMAIN2));
    }

    @Test
    public void testKnownPropertyValue_multiTenancy() {
        final String oldPropertyValue1 = "/tmp/fs_plugin_data/default";
        final String oldPropertyValue2 = "/tmp/fs_plugin_data/red";
        final String newPropertyValue1 = "new-property-value1";
        final String newPropertyValue2 = "new-property-value2";

        // test get value
        String value1 = fsPluginProperties.getKnownPropertyValue(DOMAIN1, LOCATION);
        String value2 = fsPluginProperties.getKnownPropertyValue(DOMAIN2, LOCATION);

        Assertions.assertEquals(oldPropertyValue1, value1);
        Assertions.assertEquals(oldPropertyValue2, value2);

        // test set value
        fsPluginProperties.setKnownPropertyValue(DOMAIN1, LOCATION, newPropertyValue1, true);
        fsPluginProperties.setKnownPropertyValue(DOMAIN2, LOCATION, newPropertyValue2, true);

        value1 = fsPluginProperties.getKnownPropertyValue(DOMAIN1, LOCATION);
        value2 = fsPluginProperties.getKnownPropertyValue(DOMAIN2, LOCATION);

        Assertions.assertEquals(newPropertyValue1, value1);
        Assertions.assertEquals(newPropertyValue2, value2);

        // reset context
        fsPluginProperties.setKnownPropertyValue(DOMAIN1, LOCATION, oldPropertyValue1, true);
        fsPluginProperties.setKnownPropertyValue(DOMAIN2, LOCATION, oldPropertyValue2, true);
    }

}
