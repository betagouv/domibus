package eu.domibus.core.property;

import eu.domibus.api.property.DomibusPropertyMetadata;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PROXY_PASSWORD;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_UI_TITLE_NAME;

@ExtendWith(JMockitExtension.class)
public class CorePropertyMetadataManagerImplTest {

    @Tested
    CorePropertyMetadataManagerImpl corePropertyMetadataManager;

    @Injectable
    ApplicationContext applicationContext;

    @Test
    public void getKnownProperties_nonExisting() {
        Map<String, DomibusPropertyMetadata> props = corePropertyMetadataManager.getKnownProperties();
        DomibusPropertyMetadata actual = props.get("non_existing");

        Assertions.assertEquals(null, actual);
    }

    @Test
    public void getKnownProperties() {
        Map<String, DomibusPropertyMetadata> props = corePropertyMetadataManager.getKnownProperties();
        DomibusPropertyMetadata actual = props.get(DOMIBUS_UI_TITLE_NAME);

        Assertions.assertEquals(DOMIBUS_UI_TITLE_NAME, actual.getName());
        Assertions.assertEquals(actual.getUsage(), DomibusPropertyMetadata.Usage.DOMAIN.getValue());
        Assertions.assertTrue(actual.isWithFallback());
    }

    @Test
    public void hasKnownProperty_nonExisting() {
        boolean actual = corePropertyMetadataManager.hasKnownProperty("non_existing");

        Assertions.assertEquals(false, actual);
    }

    @Test
    public void hasKnownProperty() {
        boolean actual = corePropertyMetadataManager.hasKnownProperty(DOMIBUS_UI_TITLE_NAME);

        Assertions.assertTrue(actual);
    }

    @Test
    public void getKnownProperties_getGlobalProperty() {
        Map<String, DomibusPropertyMetadata> props = corePropertyMetadataManager.getKnownProperties();
        DomibusPropertyMetadata actual = props.get(DOMIBUS_PROXY_PASSWORD);

        Assertions.assertEquals(DOMIBUS_PROXY_PASSWORD, actual.getName());
        Assertions.assertEquals("PASSWORD", actual.getType());
        Assertions.assertTrue(actual.isEncrypted());
    }
}
