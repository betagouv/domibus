package eu.domibus.core.message;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.*;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.nonrepudiation.UserMessageRawEnvelopeDao;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.ProcessingType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Transactional
public class MessageExchangeServiceIT extends AbstractIT {

    @Autowired
    MessagesLogServiceImpl messagesLogService;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    @Autowired
    MessageExchangeService messageExchangeService;

    @Autowired
    UserMessageRawEnvelopeDao userMessageRawEnvelopeDao;

    @Test
    @Transactional
    public void findUserMessageById() {

        String messageId = "msg1";
        String testXMLEnvelope = "testXMLEnvelope";

        messageDaoTestUtil.createUserMessageLog(messageId, new Date(), MSHRole.RECEIVING, MessageStatus.RECEIVED);
        messageExchangeService.saveRawXml(testXMLEnvelope, messageId, MSHRole.RECEIVING);
        RawEnvelopeDto rawXmlByMessageId = userMessageRawEnvelopeDao.findRawXmlByMessageIdAndRole(messageId, MSHRole.RECEIVING);
        String rawXmlMessage = rawXmlByMessageId.getRawXmlMessage();
        Assertions.assertEquals(testXMLEnvelope, rawXmlMessage);

        UserMessageRaw byReference = userMessageRawEnvelopeDao.findByReference(rawXmlByMessageId.getId());
        Assertions.assertTrue(Arrays.equals(byReference.getRawXML(), testXMLEnvelope.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @Transactional
    public void getMessageStatus_PUSH() throws EbMS3Exception {

        UserMessageLog userMessageLog = messageDaoTestUtil.createUserMessageLog("msg1", new Date(), MSHRole.SENDING, MessageStatus.SEND_FAILURE, false, true, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC", new Date(), false);

        MessageExchangeConfiguration userMessageExchangeContext = pModeProvider.findUserMessageExchangeContext(userMessageLog.getUserMessage(), MSHRole.SENDING, false, ProcessingType.PUSH);

        MessageStatusEntity messageStatus = messageExchangeService.getMessageStatus(userMessageExchangeContext);

        Assertions.assertEquals(MessageStatus.SEND_ENQUEUED, messageStatus.getMessageStatus());
    }

    @Test
    @Transactional
    public void getMessageStatus_PULL() throws EbMS3Exception {

        UserMessageLog userMessageLog = messageDaoTestUtil.createUserMessageLog("msg1", new Date(), MSHRole.SENDING, MessageStatus.SEND_FAILURE, false, true, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull", new Date(), false);

        MessageExchangeConfiguration userMessageExchangeContext = pModeProvider.findUserMessageExchangeContext(userMessageLog.getUserMessage(), MSHRole.SENDING, true, ProcessingType.PULL);

        MessageStatusEntity messageStatus = messageExchangeService.getMessageStatus(userMessageExchangeContext);

        Assertions.assertEquals(MessageStatus.READY_TO_PULL, messageStatus.getMessageStatus());
    }

}
