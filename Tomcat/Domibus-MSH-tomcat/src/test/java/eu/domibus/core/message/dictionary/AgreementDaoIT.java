package eu.domibus.core.message.dictionary;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.AgreementRefEntity;
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
public class AgreementDaoIT extends AbstractIT {

    @Autowired
    private AgreementDao agreementDao;

    @Test
    public void testFindByValueAndType() {
        final String value = "value1";
        final String type = "string";
        final AgreementRefEntity entity = agreementDao.findOrCreateAgreement(value, type);


        final AgreementRefEntity foundAgreement = agreementDao.findOrCreateAgreement(value, type);
        assertNotNull(foundAgreement);

        Assertions.assertEquals(entity.getEntityId(), foundAgreement.getEntityId());
        Assertions.assertEquals(value, foundAgreement.getValue());
        Assertions.assertEquals(type, foundAgreement.getType());

        final AgreementRefEntity foundAgreement1 = agreementDao.findOrCreateAgreement(value, type);

        Assertions.assertEquals(foundAgreement.getEntityId(), foundAgreement1.getEntityId());
    }

    @Test
    public void testFindByValue() {
        final String value = "agreement1";
        final String type = null;
        final AgreementRefEntity entity = agreementDao.findOrCreateAgreement(value, type);

        final AgreementRefEntity foundAgreement = agreementDao.findOrCreateAgreement(value, type);
        assertNotNull(foundAgreement);

        Assertions.assertEquals(entity.getEntityId(), foundAgreement.getEntityId());
        Assertions.assertEquals(value, foundAgreement.getValue());
        Assertions.assertEquals(type, foundAgreement.getType());

        final AgreementRefEntity foundAgreement1 = agreementDao.findOrCreateAgreement(value, type);

        Assertions.assertEquals(foundAgreement.getEntityId(), foundAgreement1.getEntityId());
    }

    @Test
    public void testFindOrCreate() {
        final String value = "value1";

        final AgreementRefEntity foundAgreement1 = agreementDao.findOrCreateAgreement(value, "  ");
        assertNotNull(foundAgreement1);

        final AgreementRefEntity foundAgreement2 = agreementDao.findOrCreateAgreement(value, "");
        assertNotNull(foundAgreement2);

        final AgreementRefEntity foundAgreement3 = agreementDao.findOrCreateAgreement(value, null);
        assertNotNull(foundAgreement3);

        final AgreementRefEntity foundAgreement4 = agreementDao.findOrCreateAgreement(value, "type1");
        assertNotNull(foundAgreement4);

        Assertions.assertEquals(foundAgreement1.getEntityId(), foundAgreement2.getEntityId());
        Assertions.assertEquals(foundAgreement1.getEntityId(), foundAgreement3.getEntityId());
        Assertions.assertNotEquals(foundAgreement1.getEntityId(), foundAgreement4.getEntityId());
    }

}
