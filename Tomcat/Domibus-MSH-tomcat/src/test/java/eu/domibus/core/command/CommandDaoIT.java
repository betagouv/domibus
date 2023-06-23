package eu.domibus.core.command;

import eu.domibus.AbstractIT;
import eu.domibus.common.JPAConstants;
import eu.domibus.core.clustering.CommandDao;
import eu.domibus.core.clustering.CommandEntity;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Cosmin Baciu
 * @since 4.0.1
 */
public class CommandDaoIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(CommandDaoIT.class);

    @Autowired
    private CommandDao commandDao;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @BeforeEach
    public void setup() {
        LOG.putMDC(DomibusLogger.MDC_USER, "test_user");
    }

    @Test
    @Transactional
    public void createCommand() {
        CommandEntity entity = new CommandEntity();
        entity.setCreationTime(new Date());
        entity.setServerName("ms1");
        entity.setCommandName("command1");

        commandDao.create(entity);

        List<CommandEntity> ms1 = commandDao.findCommandsByServerName("ms1");
        assertEquals(1, ms1.size());
        CommandEntity commandEntity = ms1.get(0);
        assertNotNull(commandEntity.getCreationTime());
        assertNotNull(commandEntity.getModificationTime());
        assertNotNull(commandEntity.getCreatedBy());
        assertNotNull(commandEntity.getModifiedBy());

        assertEquals(commandEntity.getCreationTime(), commandEntity.getModificationTime());
    }

    @Test
    @Transactional
    public void deleteCommandAndProperties() {
        CommandEntity entity = new CommandEntity();
        entity.setCreationTime(new Date());
        entity.setServerName("ms1");
        entity.setCommandName("command1");

        HashMap<String, String> commandProperties = new HashMap<>();
        commandProperties.put("key1", "value1");
        commandProperties.put("key2", "value2");
        entity.setCommandProperties(commandProperties);
        commandDao.create(entity);
        em.flush();

        // Check the TB_COMMAND_PROPERTY rows were properly generated
        assertEquals(2, em.createNativeQuery("SELECT * FROM TB_COMMAND_PROPERTY").getResultList().size());
        List<CommandEntity> ms1 = commandDao.findCommandsByServerName("ms1");

        //Delete of TB_COMMAND should delete TB_COMMAND_PROPERTY related
        commandDao.delete(ms1.get(0));

        em.flush();
        assertEquals(0, em.createNativeQuery("SELECT * FROM TB_COMMAND_PROPERTY").getResultList().size());
        assertEquals(0, commandDao.findCommandsByServerName("ms1").size());
    }
}
