package eu.domibus.core.message.acknowledge;

import eu.domibus.core.dao.BasicDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class MessageAcknowledgementPropertyDao extends BasicDao<MessageAcknowledgementProperty> {

    public MessageAcknowledgementPropertyDao() {
        super(MessageAcknowledgementProperty.class);
    }

}
