package eu.domibus.core.message.splitandjoin;

import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.splitandjoin.MessageGroupEntity;
import eu.domibus.core.message.UserMessageFactory;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.plugin.handler.MessageSubmitterImpl;
import eu.domibus.messaging.MessagingProcessingException;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(JMockitExtension.class)
public class SplitAndJoinHelperTest {

    @Injectable
    protected UserMessageLogDao userMessageLogDao;

    @Injectable
    protected UserMessageFactory userMessageFactory;

    @Injectable
    MessageSubmitterImpl messageSubmitter;

    @Injectable
    protected MessageGroupDao messageGroupDao;

    @Injectable
    MshRoleDao mshRoleDao;

    @Tested
    SplitAndJoinHelper splitAndJoinHelper;

    @Test
    public void createMessagingForFragment(@Injectable UserMessage sourceMessage,
                                           @Injectable MessageGroupEntity messageGroupEntity,
                                           @Injectable UserMessage userMessageFragment) throws MessagingProcessingException {
        String backendName = "mybackend";

        final String fragment1 = "fragment1";

        new Expectations() {{
            userMessageFactory.createUserMessageFragment(sourceMessage, messageGroupEntity, 1L, fragment1);
            result = userMessageFragment;
        }};

        splitAndJoinHelper.createMessagingForFragment(sourceMessage, messageGroupEntity, backendName, fragment1, 1);

        new Verifications() {{
            userMessageFactory.createUserMessageFragment(sourceMessage, messageGroupEntity, 1L, fragment1);
        }};
    }

    @Test
    public void createMessageFragments(@Injectable UserMessage sourceMessage,
                                       @Injectable MessageGroupEntity messageGroupEntity
    ) throws MessagingProcessingException {
        final long entityId = 123;
        String backendName = "mybackend";

        List<String> fragmentFiles = new ArrayList<>();
        final String fragment1 = "fragment1";
        fragmentFiles.add(fragment1);


        new Expectations(splitAndJoinHelper) {{
            sourceMessage.getEntityId();
            result = entityId;

            userMessageLogDao.findBackendForMessageEntityId(entityId);
            result = backendName;

            splitAndJoinHelper.createMessagingForFragment(sourceMessage, messageGroupEntity, backendName, fragment1, 1);
            times = 1;
        }};

        splitAndJoinHelper.createMessageFragments(sourceMessage, messageGroupEntity, fragmentFiles);

        new Verifications() {{
            messageGroupDao.create(messageGroupEntity);


        }};
    }

}
