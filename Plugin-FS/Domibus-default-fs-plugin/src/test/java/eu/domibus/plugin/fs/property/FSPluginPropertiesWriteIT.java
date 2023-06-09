package eu.domibus.plugin.fs.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.core.property.DefaultDomibusConfigurationService;
import eu.domibus.core.property.PropertyChangeManager;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.test.AbstractIT;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static eu.domibus.core.property.PropertyChangeManager.PROPERTY_VALUE_DELIMITER;
import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.LOCATION;

/**
 * @author Ion Perpegel
 * @since 5.1
 */
public class FSPluginPropertiesWriteIT extends AbstractIT {

    @Autowired
    FSPluginProperties fsPluginProperties;

    @Autowired
    DefaultDomibusConfigurationService domibusConfigurationService;

    @Autowired
    PropertyChangeManager propertyChangeManager;

    @Autowired
    DomainContextExtService domainContextExtService;

    @Configuration
    @PropertySource(value = "file:${domibus.config.location}/plugins/config/fs-plugin.properties")
    static class ContextConfiguration {
    }

    @Test
    public void testKnownPropertyValue_singleTenancy() throws IOException {
        final String domainDefault = "default";
        final String propertyName1 = FSPluginPropertiesMetadataManagerImpl.PAYLOAD_ID;
        final String propertyName2 = FSPluginPropertiesMetadataManagerImpl.SENT_ACTION;
        final String oldPropertyValue1 = "cid:message";
        final String oldPropertyValue2 = "delete";
        final String newPropertyValue1 = "new-property-value1";
        final String newPropertyValue2 = "new-property-value2";

        File propertyFile = getPropertyFile();
        DomainDTO domain = domainContextExtService.getCurrentDomainSafely();

        // test get value
        String value1 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName1);
        String value2 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName2);

        Assert.assertEquals(oldPropertyValue1, value1);
        Assert.assertEquals(oldPropertyValue2, value2);

        // test set value
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName1, newPropertyValue1, false);
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName2, newPropertyValue2, true);

        value1 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName1);
        value2 = fsPluginProperties.getKnownPropertyValue(domainDefault, propertyName2);

        String persistedPropertyName1 = findPropertyInFile(domain.getCode() + "." + propertyName1, propertyFile);
        String persistedPropertyName2 = findPropertyInFile(domain.getCode() + "." + propertyName2, propertyFile);

        Assert.assertEquals(persistedPropertyName1, value1);
        Assert.assertEquals(persistedPropertyName2, value2);
        Assert.assertEquals(newPropertyValue1, value1);
        Assert.assertEquals(newPropertyValue2, value2);

        // reset context
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName1, oldPropertyValue1, false);
        fsPluginProperties.setKnownPropertyValue(domainDefault, propertyName2, oldPropertyValue2, true);
    }

    private File getPropertyFile() {
        DomainDTO domain = domainContextExtService.getCurrentDomainSafely();
        String configurationFileName = fsPluginProperties.getConfigurationFileName(domain).get();
        String fullName = domibusConfigurationService.getConfigLocation() + File.separator + configurationFileName;
        return new File(fullName);
    }

    private String findPropertyInFile(String propertyName, File propertyFile) throws IOException {
        List<String> lines = Files.readAllLines(propertyFile.toPath());
        int lineNr = propertyChangeManager.findLineWithProperty(propertyName, lines);
        if (lineNr >= 0) {
            String persistedProperty = lines.get(lineNr);
            return StringUtils.substringAfter(persistedProperty, PROPERTY_VALUE_DELIMITER);
        }
        return null;
    }
}
