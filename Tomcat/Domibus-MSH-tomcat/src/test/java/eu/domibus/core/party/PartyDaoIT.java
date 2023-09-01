package eu.domibus.core.party;

import eu.domibus.AbstractIT;
import eu.domibus.common.model.configuration.Identifier;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.core.pmode.ConfigurationDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceContext;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Transactional
public class PartyDaoIT extends AbstractIT {

    @PersistenceContext
    private javax.persistence.EntityManager em;

    @Autowired
    private PartyDao partyDao;

    @Autowired
    ConfigurationDAO configurationDAO;

    @Transactional
    @BeforeEach
    public void initParty() throws SQLException {
        Party party = new Party();
        party.setName("P1");
        Identifier id = new Identifier();
        id.setPartyId("P1 party id");
        party.getIdentifiers().add(id);

        partyDao.create(party);

        Process process = new Process();
        process.setName("PR1");
        process.addInitiator(party);

        party = new Party();
        party.setName("P2");
        id = new Identifier();
        id.setPartyId("P2 party id");
        party.getIdentifiers().add(id);

        process.addResponder(party);

        partyDao.create(party);

        party = new Party();
        party.setName("P3");
        id = new Identifier();
        id.setPartyId("P3 party id");
        party.getIdentifiers().add(id);

        partyDao.create(party);
    }

    @Transactional
    @Test
    public void listParties() throws SQLException {
        List<Party> parties = partyDao.getParties();
        assertNotNull(parties.get(0).getCreationTime());
        assertNotNull(parties.get(0).getModificationTime());
        assertNotNull(parties.get(0).getCreatedBy());
        assertNotNull(parties.get(0).getModifiedBy());
        assertNotNull(parties.get(1).getCreationTime());
        assertNotNull(parties.get(1).getModificationTime());
        assertNotNull(parties.get(1).getCreatedBy());
        assertNotNull(parties.get(1).getModifiedBy());
        assertNotNull(parties.get(2).getCreationTime());
        assertNotNull(parties.get(2).getModificationTime());
        assertNotNull(parties.get(2).getCreatedBy());
        assertNotNull(parties.get(2).getModifiedBy());
    }

//    @Transactional
    @Test
    public void testFindById() {
        // When
        Party findById = partyDao.findByPartyName("P1");

        // Then
        assertNotNull(findById);
        assertNotNull(findById.getCreationTime());
        assertNotNull(findById.getModificationTime());
        assertNotNull(findById.getCreatedBy());
        assertNotNull(findById.getModifiedBy());

        assertEquals(findById.getCreationTime(), findById.getModificationTime());
    }

}
