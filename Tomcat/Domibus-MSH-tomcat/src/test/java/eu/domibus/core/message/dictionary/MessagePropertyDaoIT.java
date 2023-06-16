package eu.domibus.core.message.dictionary;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MessageProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author François Gautier
 * @since 5.0
 */
@Transactional
public class MessagePropertyDaoIT extends AbstractIT {

    @Autowired
    private MessagePropertyDao propertyDao;

    @Test
    public void testFindPropertyByNameValueAndType() {
        final String name = "prop1";
        final String value = "value1";
        final String type = "string";
        final MessageProperty property = propertyDao.findOrCreateProperty(name, value, type);

        final MessageProperty foundProperty = propertyDao.findOrCreateProperty(name, value, type);
        assertNotNull(foundProperty);

        Assertions.assertEquals(property.getEntityId(), foundProperty.getEntityId());
        Assertions.assertEquals(name, foundProperty.getName());
        Assertions.assertEquals(value, foundProperty.getValue());
        Assertions.assertEquals(type, foundProperty.getType());

        final MessageProperty foundProperty1 = propertyDao.findOrCreateProperty(name, value, type);

        Assertions.assertEquals(foundProperty.getEntityId(), foundProperty1.getEntityId());
    }

    @Test
    public void testFindPropertyByNameAndValue() {
        final String name = "prop1";
        final String value = "value1";
        final String type = null;
        final MessageProperty property = propertyDao.findOrCreateProperty(name, value, type);

        final MessageProperty foundProperty = propertyDao.findOrCreateProperty(name, value, type);
        assertNotNull(foundProperty);

        Assertions.assertEquals(property.getEntityId(), foundProperty.getEntityId());
        Assertions.assertEquals(name, foundProperty.getName());
        Assertions.assertEquals(value, foundProperty.getValue());
        Assertions.assertEquals(type, foundProperty.getType());

        final MessageProperty foundProperty1 = propertyDao.findOrCreateProperty(name, value, type);

        Assertions.assertEquals(foundProperty.getEntityId(), foundProperty1.getEntityId());
    }


    @Test
    public void testFindOrCreate() {
        final String name = "name1";
        final String value = "value1";

        final MessageProperty foundEntity1 = propertyDao.findOrCreateProperty(name, value, "  ");
        assertNotNull(foundEntity1);

        final MessageProperty foundEntity2 = propertyDao.findOrCreateProperty(name, value, "");
        assertNotNull(foundEntity2);

        final MessageProperty foundEntity3 = propertyDao.findOrCreateProperty(name, value, null);
        assertNotNull(foundEntity3);

        final MessageProperty foundEntity4 = propertyDao.findOrCreateProperty(name, value, "type1");
        assertNotNull(foundEntity4);

        Assertions.assertEquals(foundEntity1.getEntityId(), foundEntity2.getEntityId());
        Assertions.assertEquals(foundEntity1.getEntityId(), foundEntity3.getEntityId());
        Assertions.assertNotEquals(foundEntity1.getEntityId(), foundEntity4.getEntityId());
    }

}
