package eu.domibus.core.message.dictionary;


import eu.domibus.api.model.ActionEntity;
import eu.domibus.core.dao.SingleValueDictionaryDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;


/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class ActionDao extends SingleValueDictionaryDao<ActionEntity> {

    public ActionDao() {
        super(ActionEntity.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ActionEntity findOrCreateAction(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        ActionEntity mpc = findByValue(value);
        if (mpc != null) {
            return mpc;
        }
        ActionEntity newMpc = new ActionEntity();
        newMpc.setValue(value);
        create(newMpc);
        return newMpc;
    }

    public ActionEntity findByValue(final Object value) {
        final TypedQuery<ActionEntity> query = this.em.createNamedQuery("Action.findByValue", ActionEntity.class);
        query.setParameter("VALUE", value);
        return DataAccessUtils.singleResult(query.getResultList());
    }
}
