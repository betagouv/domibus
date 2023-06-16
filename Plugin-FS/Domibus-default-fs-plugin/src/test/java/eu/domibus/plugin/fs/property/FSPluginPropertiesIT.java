package eu.domibus.plugin.fs.property;

import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.test.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static eu.domibus.plugin.fs.worker.FSSendMessagesService.DEFAULT_DOMAIN;

/**
 * @author Catalin Enache
 * @since 5.0
 */
public class FSPluginPropertiesIT extends AbstractIT {

    private static final String NONEXISTENT_DOMAIN = "NONEXISTENT_DOMAIN";

    private static final String DEFAULT_LOCATION = "/tmp/fs_plugin_data/default";

    @Autowired
    FSPluginProperties fsPluginProperties;

    @Configuration
    @PropertySource(value = "file:${domibus.config.location}/dataset/fsplugin/fs-plugin.properties")
    // as this test file is added last to property sources and
    // contrary to implemented properties mechanism which allow to override values from the default file
    // fs-plugin-default.properties takes precedence here so we will test both files values
    static class ContextConfiguration {
    }


    @Test
    public void testGetLocation() {
        String location = fsPluginProperties.getLocation(DEFAULT_DOMAIN);
        Assertions.assertEquals(DEFAULT_LOCATION, location);
    }

    @Test
    void testGetLocation_NonExistentDomain() {
        Assertions.assertThrows(DomibusPropertyExtException.class, () -> fsPluginProperties.getLocation(NONEXISTENT_DOMAIN));
    }

    @Test
    public void testGetSentAction() {
        Assertions.assertEquals(FSPluginProperties.ACTION_DELETE, fsPluginProperties.getSentAction(DEFAULT_DOMAIN));
    }

    @Test
    public void testGetSentPurgeWorkerCronExpression() {
        Assertions.assertEquals("0 0/1 * * * ?", fsPluginProperties.getSentPurgeWorkerCronExpression(DEFAULT_DOMAIN));
    }

    @Test
    public void testGetSentPurgeExpired() {
        Assertions.assertEquals(Integer.valueOf(600), fsPluginProperties.getSentPurgeExpired(DEFAULT_DOMAIN));
    }

    @Test
    public void testGetFailedPurgeWorkerCronExpression() {
        Assertions.assertEquals("0 0/1 * * * ?", fsPluginProperties.getFailedPurgeWorkerCronExpression(DEFAULT_DOMAIN));
    }

    @Test
    public void testGetFailedPurgeExpired() {
        Assertions.assertEquals(Integer.valueOf(600), fsPluginProperties.getFailedPurgeExpired(DEFAULT_DOMAIN));
    }

    @Test
    public void testGetReceivedPurgeExpired() {
        Assertions.assertEquals(Integer.valueOf(600), fsPluginProperties.getReceivedPurgeExpired(DEFAULT_DOMAIN));
    }

    @Test
    void testGetPayloadId_NullDomain() {
        Assertions.assertThrows(DomibusPropertyExtException.class, () -> fsPluginProperties.getPayloadId(null));
    }

    @Test
    public void testGetPayloadId_ok() {
        Assertions.assertEquals("cid:message", fsPluginProperties.getPayloadId(DEFAULT_DOMAIN));
    }

    @Test
    public void testKnownPropertyValue_singleTenancy() {
        final String domainDefault = "default";
        final String propertyName1 = FSPluginPropertiesMetadataManagerImpl.FAILED_ACTION;
        final String propertyName2 = FSPluginPropertiesMetadataManagerImpl.SENT_ACTION;
        final String oldPropertyValue1 = "archive";
        final String oldPropertyValue2 = "delete";
        final String newPropertyValue1 = "new-property-value1";
        final String newPropertyValue2 = "new-property-value2";

        // test get value
        String value1 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName1);
        String value2 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName2);

        Assertions.assertEquals(oldPropertyValue1, value1);
        Assertions.assertEquals(oldPropertyValue2, value2);

        // test set value
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName1, newPropertyValue1, false);
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName2, newPropertyValue2, true);

        value1 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName1);
        value2 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName2);

        Assertions.assertEquals(newPropertyValue1, value1);
        Assertions.assertEquals(newPropertyValue2, value2);

        // reset context
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName1, oldPropertyValue1, false);
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName2, oldPropertyValue2, true);
    }


}
