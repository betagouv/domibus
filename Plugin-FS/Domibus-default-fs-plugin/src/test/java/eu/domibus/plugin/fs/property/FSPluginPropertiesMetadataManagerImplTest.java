package eu.domibus.plugin.fs.property;

import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.SEND_WORKER_INTERVAL;
import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.SENT_PURGE_WORKER_CRONEXPRESSION;

@ExtendWith(JMockitExtension.class)
public class FSPluginPropertiesMetadataManagerImplTest {

    @Injectable
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    @Tested
    FSPluginPropertiesMetadataManagerImpl propertyMetadataManager;

    @Test
    public void getKnownProperties_nonExisting() {
        Map<String, DomibusPropertyMetadataDTO> props = propertyMetadataManager.getKnownProperties();
        DomibusPropertyMetadataDTO actual = props.get("non_existing");

        Assertions.assertEquals(null, actual);
    }

    @Test
    public void getKnownProperties() {
        Map<String, DomibusPropertyMetadataDTO> props = propertyMetadataManager.getKnownProperties();
        DomibusPropertyMetadataDTO actual = props.get(SEND_WORKER_INTERVAL);

        Assertions.assertEquals(SEND_WORKER_INTERVAL, actual.getName());
        Assertions.assertEquals(true, actual.isDomain());
        Assertions.assertEquals(true, actual.isWithFallback());
    }

    @Test
    public void hasKnownProperty_nonExisting() {
        boolean actual = propertyMetadataManager.hasKnownProperty("non_existing");

        Assertions.assertEquals(false, actual);
    }

    @Test
    public void hasKnownProperty() {
        boolean actual = propertyMetadataManager.hasKnownProperty(SENT_PURGE_WORKER_CRONEXPRESSION);

        Assertions.assertEquals(true, actual);
    }

}
