package eu.domibus.core.earchive;

import eu.domibus.core.dao.BasicDao;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author François Gautier
 * @since 5.0
 */
@Repository
public class EArchiveBatchUserMessageDao extends BasicDao<EArchiveBatchUserMessage> {

    public EArchiveBatchUserMessageDao() {
        super(EArchiveBatchUserMessage.class);
    }

    @Transactional
    public void create(EArchiveBatch entity, Long userMessageLogEntityId) {
        EArchiveBatchUserMessage eArchiveBatchUserMessage = new EArchiveBatchUserMessage();
        eArchiveBatchUserMessage.seteArchiveBatch(entity);
        eArchiveBatchUserMessage.setUserMessageEntityId(userMessageLogEntityId);
        super.create(eArchiveBatchUserMessage);
    }
}
