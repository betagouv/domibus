package eu.domibus.core.message;

import eu.domibus.api.model.*;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.configuration.connectionMonitoring.ConnectionMonitoringModuleConfiguration;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.message.dictionary.NotificationStatusDao;
import eu.domibus.core.message.signal.SignalMessageLogDao;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;
import java.util.Date;

import static eu.domibus.api.model.MSHRole.SENDING;
import static eu.domibus.api.model.MessageStatus.DELETED;
import static eu.domibus.api.model.MessageStatus.SEND_ENQUEUED;
import static eu.domibus.api.model.NotificationStatus.NOTIFIED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
@ExtendWith(JMockitExtension.class)
public class UserMessageLogDefaultServiceTest {

    @Tested
    UserMessageLogDefaultService userMessageLogDefaultService;

    @Injectable
    UserMessageLogDao userMessageLogDao;

    @Injectable
    SignalMessageLogDao signalMessageLogDao;

    @Injectable
    BackendNotificationService backendNotificationService;

    @Injectable
    protected MessageStatusDao messageStatusDao;

    @Injectable
    protected MshRoleDao mshRoleDao;

    @Injectable
    protected NotificationStatusDao notificationStatusDao;

//    @Injectable
//    ConnectionMonitoringConfigurationManager connectionMonitoringConfigurationManager;

    @Injectable
    EventService eventService;

    @Injectable
    AlertConfigurationService alertConfigurationService;

    @Test
    public void setSignalMessageAsDeleted_signalIsNull() {
        assertFalse(userMessageLogDefaultService.setSignalMessageAsDeleted((SignalMessage) null));
    }

    @Test
    public void setSignalMessageAsDeleted_messageIdIsNull(@Injectable final SignalMessage signalMessage) {
        new Expectations() {{
            signalMessage.getSignalMessageId();
            result = null;

        }};
        assertFalse(userMessageLogDefaultService.setSignalMessageAsDeleted(signalMessage));
        new FullVerifications() {{
            signalMessage.toString();
        }};
    }

    @Test
    public void setSignalMessageAsDeleted_messageIdIsBlank(@Injectable final SignalMessage signalMessage) {
        new Expectations() {{
            signalMessage.getSignalMessageId();
            result = "";

        }};
        assertFalse(userMessageLogDefaultService.setSignalMessageAsDeleted(signalMessage));
        new FullVerifications() {{
            signalMessage.toString();
        }};
    }

    @Test
    public void setSignalMessageAsDeleted_ok(@Injectable final SignalMessage signalMessage,
                                             @Injectable final SignalMessageLog signalMessageLog) {
        String messageId = "1";
        final MessageStatusEntity messageStatusEntity = new MessageStatusEntity();
        messageStatusEntity.setMessageStatus(DELETED);
        new Expectations() {{
            signalMessage.getSignalMessageId();
            result = messageId;

            messageStatusDao.findOrCreate(MessageStatus.DELETED);
            result = messageStatusEntity;
        }};

        assertTrue(userMessageLogDefaultService.setSignalMessageAsDeleted(signalMessage));

        new Verifications() {{
            signalMessageLog.setDeleted((Date) any);
            times = 1;

            signalMessageLog.setMessageStatus(messageStatusEntity);
            times = 1;
        }};
    }

    @Test
    public void testSave() {
        final String messageId = "1";
        final String messageStatus = SEND_ENQUEUED.toString();
        final String notificationStatus = NOTIFIED.toString();
        final String mshRole = SENDING.toString();
        final Integer maxAttempts = 10;
        final String backendName = "JMS";

        UserMessage userMessage = new UserMessage();
        userMessage.setMessageId(messageId);
        userMessage.setConversationId(messageId);

        MSHRoleEntity mshRoleEntity = new MSHRoleEntity();
        mshRoleEntity.setRole(SENDING);

        MessageStatusEntity messageStatusEntity = new MessageStatusEntity();
        messageStatusEntity.setMessageStatus(SEND_ENQUEUED);

        NotificationStatusEntity notificationStatusEntity = new NotificationStatusEntity();
        notificationStatusEntity.setStatus(NOTIFIED);

        new Expectations() {{
            messageStatusDao.findOrCreate(SEND_ENQUEUED);
            result = messageStatusEntity;

            mshRoleDao.findOrCreate(SENDING);
            result = mshRoleEntity;

            notificationStatusDao.findOrCreate(NOTIFIED);
            result = notificationStatusEntity;
        }};

        userMessageLogDefaultService.save(userMessage, messageStatus, notificationStatus, mshRole, maxAttempts, backendName);

        new Verifications() {{
            backendNotificationService.notifyOfMessageStatusChange(userMessage, withAny(new UserMessageLog()), SEND_ENQUEUED, withAny(new Timestamp(System.currentTimeMillis())));
            times = 1;

            UserMessageLog userMessageLog;
            userMessageLogDao.create(userMessageLog = withCapture());
            times = 1;
            Assertions.assertEquals(messageId, userMessageLog.getUserMessage().getMessageId());
            Assertions.assertEquals(SEND_ENQUEUED, userMessageLog.getMessageStatus());
            Assertions.assertEquals(NOTIFIED, userMessageLog.getNotificationStatus().getStatus());
            Assertions.assertEquals(SENDING, userMessageLog.getMshRole().getRole());
            Assertions.assertEquals(maxAttempts.intValue(), userMessageLog.getSendAttemptsMax());
            Assertions.assertEquals(backendName, userMessageLog.getBackend());
        }};
    }

    @Test
    public void testUpdateMessageStatus(@Injectable final UserMessageLog messageLog,
                                        @Injectable final UserMessage userMessage) {
        final MessageStatus messageStatus = SEND_ENQUEUED;

        userMessageLogDefaultService.updateUserMessageStatus(userMessage, messageLog, messageStatus);

        new Verifications() {{
            backendNotificationService.notifyOfMessageStatusChange(userMessage, messageLog, messageStatus, withAny(new Timestamp(System.currentTimeMillis())));
            userMessageLogDao.setMessageStatus(messageLog, messageStatus);
        }};
    }

    @Test
    public void testSetMessageAsDeleted(@Injectable final UserMessage userMessage,
                                        @Injectable final UserMessageLog userMessageLog) {

        new Expectations() {{
            userMessage.isTestMessage();
            result = true;
        }};

        userMessageLogDefaultService.setMessageAsDeleted(userMessage, userMessageLog);

        new Verifications() {{
            userMessageLogDao.setMessageStatus(userMessageLog, MessageStatus.DELETED);
            times = 1;
        }};
    }

    @Test
    public void testSetMessageAsDownloaded(@Injectable UserMessage userMessage,
                                           @Injectable UserMessageLog userMessageLog) {

        userMessageLogDefaultService.setMessageAsDownloaded(userMessage, userMessageLog);

        new Verifications() {{
            backendNotificationService.notifyOfMessageStatusChange(userMessage, userMessageLog, MessageStatus.DOWNLOADED, (Timestamp) any);
            times = 1;

            userMessageLogDao.setMessageStatus(userMessageLog, MessageStatus.DOWNLOADED);
            times = 1;
        }};
    }

    @Test
    public void testSetMessageAsAcknowledged(@Injectable UserMessage userMessage,
                                             @Injectable UserMessageLog userMessageLog) {

        userMessageLogDefaultService.setMessageAsAcknowledged(userMessage, userMessageLog);

        new Verifications() {{
            backendNotificationService.notifyOfMessageStatusChange(userMessage, userMessageLog, MessageStatus.ACKNOWLEDGED, (Timestamp) any);
            times = 1;

            userMessageLogDao.setMessageStatus(userMessageLog, MessageStatus.ACKNOWLEDGED);
            times = 1;
        }};
    }

    @Test
    public void testSetMessageAsAckWithWarnings(@Injectable UserMessage userMessage,
                                                @Injectable UserMessageLog userMessageLog) {

        userMessageLogDefaultService.setMessageAsAckWithWarnings(userMessage, userMessageLog);


        new Verifications() {{
            backendNotificationService.notifyOfMessageStatusChange(userMessage, userMessageLog, MessageStatus.ACKNOWLEDGED_WITH_WARNING, (Timestamp) any);
            times = 1;

            userMessageLogDao.setMessageStatus(userMessageLog, MessageStatus.ACKNOWLEDGED_WITH_WARNING);
            times = 1;
        }};
    }

    @Test
    public void tesSetMessageAsSendFailure(@Injectable UserMessage userMessage,
                                           @Injectable UserMessageLog userMessageLog) {

        userMessageLogDefaultService.setMessageAsSendFailure(userMessage, userMessageLog);

        new Verifications() {{
            backendNotificationService.notifyOfMessageStatusChange(userMessage, userMessageLog, MessageStatus.SEND_FAILURE, (Timestamp) any);
            times = 1;

            userMessageLogDao.setMessageStatus(userMessageLog, MessageStatus.SEND_FAILURE);
            times = 1;
        }};
    }

}
