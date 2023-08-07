package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MSHRoleEntity;
import eu.domibus.core.dao.BasicDao;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

/**
 * @author Cosmin Baciu
 * @since 5.0
 *
 * @implNote This DAO class works with {@link MSHRoleEntity}, which is a static dictionary
 * based on the {@link MSHRole} enum: no new values are expected to be added at runtime;
 * therefore, {@link MshRoleDao} can be used directly, without subclassing {@link AbstractDictionaryService}.
 */
@Repository
public class MshRoleDao extends BasicDao<MSHRoleEntity> {

    public MshRoleDao() {
        super(MSHRoleEntity.class);
    }

    @Transactional
    public MSHRoleEntity findOrCreate(final MSHRole role) {
        if (role == null) {
            return null;
        }

        MSHRoleEntity mshRoleEntity = findByRole(role);
        if (mshRoleEntity != null) {
            return mshRoleEntity;
        }
        MSHRoleEntity entity = new MSHRoleEntity();
        entity.setRole(role);
        create(entity);
        return entity;
    }

    public MSHRoleEntity findByRole(final MSHRole role) {
        final TypedQuery<MSHRoleEntity> query = this.em.createNamedQuery("MSHRoleEntity.findByValue", MSHRoleEntity.class);
        query.setParameter("ROLE", role);
        return DataAccessUtils.singleResult(query.getResultList());
    }
}
