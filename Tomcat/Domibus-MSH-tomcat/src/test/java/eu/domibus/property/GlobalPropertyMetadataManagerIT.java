package eu.domibus.property;

import eu.domibus.AbstractIT;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.core.property.GlobalPropertyMetadataManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE_PER_MPC;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_UI_TITLE_NAME;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
public class GlobalPropertyMetadataManagerIT extends AbstractIT {

    @Autowired
    private GlobalPropertyMetadataManager globalPropertyMetadataManager;

    @Test
    public void hasKnownProperty_nonexistent() {
        String propertyName = "non-existent-property";

        boolean res = globalPropertyMetadataManager.hasKnownProperty(propertyName);
        Assertions.assertFalse(res);
    }

    @Test
    public void hasKnownProperty_existent() {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        boolean res = globalPropertyMetadataManager.hasKnownProperty(propertyName);
        Assertions.assertTrue(res);
    }

    @Test
    public void getPropertyMetadata_nonexistent_createOnTheFly() {
        String propertyName = "non-existent-property";

        DomibusPropertyMetadata res = globalPropertyMetadataManager.getPropertyMetadata(propertyName);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(propertyName, res.getName());
        Assertions.assertEquals(DomibusPropertyMetadata.Usage.ANY.getValue(), res.getUsage());
    }

    @Test
    public void getPropertyMetadata_existent() {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        DomibusPropertyMetadata res = globalPropertyMetadataManager.getPropertyMetadata(propertyName);
        Assertions.assertNotNull(res);
        Assertions.assertEquals(propertyName, res.getName());
    }

    @Test
    public void getProperty_createNested() {
        String composablePropertyName = "composable_property";
        String nestedPropertyName = composablePropertyName + ".prop1";
        DomibusPropertyMetadata propertyMetadata = DomibusPropertyMetadata.getGlobalProperty(composablePropertyName);
        propertyMetadata.setComposable(true);
        globalPropertyMetadataManager.getAllProperties().put(composablePropertyName, propertyMetadata);

        DomibusPropertyMetadata res = globalPropertyMetadataManager.getPropertyMetadata(nestedPropertyName);

        Assertions.assertNotNull(res);
        Assertions.assertEquals(nestedPropertyName, res.getName());
        Assertions.assertEquals(false, res.isComposable());
    }

    @Test
    public void getComposableProperty_createNested() {
        String composablePropertyName = "composable_property";
        String nestedPropertyName = composablePropertyName + ".prop1";
        DomibusPropertyMetadata propertyMetadata = DomibusPropertyMetadata.getGlobalProperty(composablePropertyName);
        propertyMetadata.setComposable(true);
        globalPropertyMetadataManager.getAllProperties().put(composablePropertyName, propertyMetadata);

        DomibusPropertyMetadata res = globalPropertyMetadataManager.getComposableProperty(nestedPropertyName);

        Assertions.assertNotNull(res);
        Assertions.assertEquals(composablePropertyName, res.getName());
        Assertions.assertEquals(true, res.isComposable());
    }

    @Test
    public void hasKnownProperty_composable() {
        String propertyName = DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE_PER_MPC + ".MPC_NAME";

        boolean res = globalPropertyMetadataManager.hasKnownProperty(propertyName);
        Assertions.assertTrue(res);
    }
}
