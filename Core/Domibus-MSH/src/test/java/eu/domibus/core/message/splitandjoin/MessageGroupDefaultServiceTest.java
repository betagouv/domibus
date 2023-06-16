package eu.domibus.core.message.splitandjoin;

import eu.domibus.api.model.splitandjoin.MessageGroupEntity;
import eu.domibus.core.message.UserMessageDao;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.junit.jupiter.api.Test;

/**
 * @author Cosmin Baciu
 * @since
 */
public class MessageGroupDefaultServiceTest {

    @Injectable
    protected MessageGroupDao messageGroupDao;

    @Injectable
    protected UserMessageDao userMessageDao;

    @Tested
    MessageGroupDefaultService messageGroupDefaultService;


    @Test
    public void setSourceMessageId(@Injectable final MessageGroupEntity messageGroupEntity) {
        String sourceMessageId = "123";
        String groupId = sourceMessageId;

        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;
        }};

        messageGroupDefaultService.setSourceMessageId(sourceMessageId, groupId);

        new Verifications() {{
//            messageGroupEntity.setSourceMessageId(sourceMessageId);
            messageGroupDao.update(messageGroupEntity);
        }};


    }
}
