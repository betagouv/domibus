
package eu.domibus.core.party;

import eu.domibus.core.dao.BasicDao;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.Collection;
import java.util.List;

/**
 * @author Christian Koch, Stefan Mueller
 */
@Repository
public class PartyDao extends BasicDao<Party> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyDao.class);

    public PartyDao() {
        super(Party.class);
    }

    public Collection<Identifier> findPartyIdentifiersByEndpoint(final String endpoint) {
        final Query query = this.em.createNamedQuery("Party.findPartyIdentifiersByEndpoint");
        query.setParameter("ENDPOINT", endpoint);

        return query.getResultList();
    }

    public List<Party> getParties(){
        return em.createNamedQuery("Party.findAll",Party.class).getResultList();
    }

    public Party findByPartyName(final String id) {
        final Query query = this.em.createNamedQuery("Party.findByName");
        query.setParameter("NAME", id);
        return (Party) query.getSingleResult();
    }

    public void deleteAll() {
        Query query = em.createNamedQuery("Party.deleteAll");
        query.executeUpdate();
    }
}
