package eu.domibus.core.message;

import eu.domibus.api.model.UserMessage;
import eu.domibus.core.dao.BasicDao;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class UserMessageDao extends BasicDao<UserMessage> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageDao.class);

    private static final String GROUP_ID = "GROUP_ID";

    public UserMessageDao() {
        super(UserMessage.class);
    }


    @Transactional
    public UserMessage findByEntityId(Long entityId) {
        final UserMessage userMessage = super.read(entityId);

        initializeChildren(userMessage);

        return userMessage;
    }

    private void initializeChildren(UserMessage userMessage) {
        //initialize values from the second level cache
        userMessage.getMessageProperties().forEach(messageProperty -> messageProperty.getValue());
        userMessage.getMpcValue();
        userMessage.getServiceValue();
        userMessage.getActionValue();
        userMessage.getAgreementRef();
        userMessage.getPartyInfo().getFrom().getPartyId().getValue();
        userMessage.getPartyInfo().getFrom().getRole().getValue();
        userMessage.getPartyInfo().getTo().getPartyId().getValue();
        userMessage.getPartyInfo().getTo().getRole().getValue();
    }

    @Transactional
    public UserMessage findByMessageId(String messageId) {
        final TypedQuery<UserMessage> query = this.em.createNamedQuery("UserMessage.findByMessageId", UserMessage.class);
        query.setParameter("MESSAGE_ID", messageId);
        final UserMessage userMessage = DataAccessUtils.singleResult(query.getResultList());
        if(userMessage != null) {
            initializeChildren(userMessage);
        }
        return userMessage;
    }

    public UserMessage findByGroupEntityId(Long groupEntityId) {
        final TypedQuery<UserMessage> query = this.em.createNamedQuery("UserMessage.findByGroupEntityId", UserMessage.class);
        query.setParameter("ENTITY_ID", groupEntityId);
        return DataAccessUtils.singleResult(query.getResultList());
    }

    public List<UserMessage> findUserMessageByGroupId(final String groupId) {
        final TypedQuery<UserMessage> query = this.em.createNamedQuery("UserMessage.findUserMessageByGroupId", UserMessage.class);
        query.setParameter(GROUP_ID, groupId);
        return query.getResultList();
    }

    @Timer(clazz = UserMessageDao.class, value = "deleteMessages")
    @Counter(clazz = UserMessageDao.class, value = "deleteMessages")
    public int deleteMessages(List<String> messageIds) {
        LOG.debug("deleteMessages [{}]", messageIds.size());
        final Query deleteQuery = em.createNamedQuery("UserMessage.deleteMessages");
        deleteQuery.setParameter("MESSAGEIDS", messageIds);
        int result = deleteQuery.executeUpdate();
        LOG.debug("deleteMessages result [{}]", result);
        return result;
    }
}