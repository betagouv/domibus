package eu.domibus.core.ebms3.sender;

import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.core.message.TestMessageValidator;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.UserMessageDefaultService;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.reliability.ReliabilityService;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Entrypoint for sending AS4 messages to C3. Contains common validation and rescheduling logic
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class MessageSenderService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageSenderService.class);

    private static final Set<MessageStatus> ALLOWED_STATUSES_FOR_SENDING = EnumSet.of(MessageStatus.SEND_ENQUEUED, MessageStatus.WAITING_FOR_RETRY);
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private UserMessageDefaultService userMessageService;

    @Autowired
    private UserMessageDao userMessageDao;

    @Autowired
    UserMessageLogDao userMessageLogDao;

    @Autowired
    MessageSenderFactory messageSenderFactory;

    @Autowired
    protected ReliabilityService reliabilityService;

    @Autowired
    protected TestMessageValidator testMessageValidator;

    @Timer(clazz = MessageSenderService.class, value = "sendUserMessage")
    @Counter(clazz = MessageSenderService.class, value = "sendUserMessage")
    public void sendUserMessage(final String messageId, Long messageEntityId, int retryCount) {
        LOG.debug("Searching user message log with id [{}] and entity id [{}].", messageId, messageEntityId);
        final UserMessageLog userMessageLog = userMessageLogDao.findByEntityId(messageEntityId);
        if (userMessageLog == null) {
            throw new MessageNotFoundException(messageId);
        }
        MessageStatus messageStatus = userMessageLog.getMessageStatus();
        LOG.debug("Status of user message with id [{}] is [{}].", messageId, messageStatus);

        if (!ALLOWED_STATUSES_FOR_SENDING.contains(messageStatus)) {
            LOG.warn("Message [{}] has a status [{}] which is not allowed for sending. Only the statuses [{}] are allowed", messageId, messageStatus, ALLOWED_STATUSES_FOR_SENDING);
            return;
        }

        final UserMessage userMessage = userMessageDao.findByEntityId(messageEntityId);
        final MessageSender messageSender = messageSenderFactory.getMessageSender(userMessage);
        final Boolean testMessage = testMessageValidator.checkTestMessage(userMessage);

        LOG.businessInfo(testMessage ? DomibusMessageCode.BUS_TEST_MESSAGE_SEND_INITIATION : DomibusMessageCode.BUS_MESSAGE_SEND_INITIATION,
                userMessage.getPartyInfo().getFromParty(), userMessage.getPartyInfo().getToParty());

        messageSender.sendMessage(userMessage, userMessageLog);
    }

    protected MessageStatus getMessageStatus(final UserMessageLog userMessageLog) {
        if (userMessageLog == null) {
            return MessageStatus.NOT_FOUND;
        }
        return userMessageLog.getMessageStatus();
    }

}
