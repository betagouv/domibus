package eu.domibus.core.message.dictionary;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.ServiceEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Transactional
public class ServiceDaoIT extends AbstractIT {

    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void testFindPropertyByNameValueAndType() {
        final String value = "value1";
        final String type = "string";

        final ServiceEntity property = serviceDao.findOrCreateService(value, type);

        final ServiceEntity foundProperty = serviceDao.findOrCreateService(value, type);
        assertNotNull(foundProperty);

        Assertions.assertEquals(property.getEntityId(), foundProperty.getEntityId());
        Assertions.assertEquals(value, foundProperty.getValue());
        Assertions.assertEquals(type, foundProperty.getType());

        final ServiceEntity foundProperty1 = serviceDao.findOrCreateService(value, type);

        Assertions.assertEquals(foundProperty.getEntityId(), foundProperty1.getEntityId());
    }

    @Test
    public void testFindPropertyByNameAndValue() {
        final String value = "value1";
        final String type = null;
        final ServiceEntity property = serviceDao.findOrCreateService(value, type);

        final ServiceEntity foundProperty = serviceDao.findOrCreateService(value, type);
        assertNotNull(foundProperty);

        Assertions.assertEquals(property.getEntityId(), foundProperty.getEntityId());
        Assertions.assertEquals(value, foundProperty.getValue());
        Assertions.assertEquals(type, foundProperty.getType());

        final ServiceEntity foundProperty1 = serviceDao.findOrCreateService(value, type);

        Assertions.assertEquals(foundProperty.getEntityId(), foundProperty1.getEntityId());
    }

    @Test
    public void testFindOrCreate() {
        final String value = "value1";

        final ServiceEntity foundEntity1 = serviceDao.findOrCreateService(value, "  ");
        assertNotNull(foundEntity1);

        final ServiceEntity foundEntity2 = serviceDao.findOrCreateService(value, "");
        assertNotNull(foundEntity2);

        final ServiceEntity foundEntity3 = serviceDao.findOrCreateService(value, null);
        assertNotNull(foundEntity3);

        final ServiceEntity foundEntity4 = serviceDao.findOrCreateService(value, "type1");
        assertNotNull(foundEntity4);

        Assertions.assertEquals(foundEntity1.getEntityId(), foundEntity2.getEntityId());
        Assertions.assertEquals(foundEntity1.getEntityId(), foundEntity3.getEntityId());
        Assertions.assertNotEquals(foundEntity1.getEntityId(), foundEntity4.getEntityId());
    }

}
