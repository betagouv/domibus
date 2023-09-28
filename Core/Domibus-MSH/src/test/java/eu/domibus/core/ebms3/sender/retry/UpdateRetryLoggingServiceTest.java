
package eu.domibus.core.ebms3.sender.retry;

import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.model.*;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.message.MessageStatusDao;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.message.nonrepudiation.UserMessageRawEnvelopeDao;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.core.message.splitandjoin.MessageGroupDao;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.scheduler.ReprogrammableService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;

import static eu.domibus.core.ebms3.sender.retry.UpdateRetryLoggingService.MESSAGE_EXPIRATION_DELAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class UpdateRetryLoggingServiceTest {

    private static final int RETRY_TIMEOUT_IN_MINUTES = 60;

    private static final int RETRY_COUNT = 4;

    private static final long SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 = 1451602800000L; //This is the reference time returned by System.correctTImeMillis() mock

    private static final long FIVE_MINUTES_BEFORE_FIRST_OF_JANUARY_2016 = SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 - (60 * 5 * 1000);

    private static final long ONE_HOUR_BEFORE_FIRST_OF_JANUARY_2016 = SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 - (60 * 60 * 1000);

    @Tested
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private UserMessageLogDefaultService messageLogService;

    @Injectable
    private UserMessageRawEnvelopeDao rawEnvelopeLogDao;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    MessageAttemptService messageAttemptService;

    @Injectable
    PModeProvider pModeProvider;

    @Injectable
    MessageRetentionDefaultService messageRetentionService;

    @Injectable
    UserMessageDao userMessageDao;

    @Injectable
    MessageGroupDao messageGroupDao;

    @Injectable
    private ReprogrammableService reprogrammableService;

    @Injectable
    MessageStatusDao messageStatusDao;


    /**
     * Max retries limit reached
     * Timeout limit not reached
     * Notification is enabled
     * Expected result: MessageLogDao#setAsNotified() is called
     * MessageLogDao#setMessageAsSendFailure is called
     * MessagingDao#clearPayloadData() is called
     *
     */
    @Test
    public void testUpdateRetryLogging_maxRetriesReachedNotificationEnabled_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                                                  @Injectable UserMessageLog userMessageLog,
                                                                                                  @Injectable LegConfiguration legConfiguration)  {
        final long entityId = 123;

        new Expectations(updateRetryLoggingService) {{
            userMessage.getEntityId();
            result = entityId;

            userMessageLogDao.findByEntityId(entityId);
            result = userMessageLog;

            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = false;

            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
            updateRetryLoggingService.getScheduledStartDate(userMessageLog);
        }};


        updateRetryLoggingService.updateRetryLogging(userMessage, legConfiguration, MessageStatus.WAITING_FOR_RETRY, null);

        new Verifications() {{
            userMessageLogDao.update(userMessageLog);
            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
        }};
    }


    /**
     * Message was restored
     * NextAttempt is set correctly
     */
    @Test
    public void testUpdateRetryLogging_Restored(@Injectable UserMessage userMessage,
                                                @Injectable LegConfiguration legConfiguration,
                                                @Injectable UserMessageLog userMessageLog)  {
//        new SystemMockFirstOfJanuary2016(); //current timestamp

        final long entityId = 123;


        new Expectations() {{
            userMessage.getEntityId();
            result = entityId;

            userMessageLogDao.findByEntityId(entityId);
            result = userMessageLog;

            userMessageLog.getSendAttempts();
            result = 2;
        }};


        updateRetryLoggingService.updateRetryLogging(userMessage, legConfiguration, MessageStatus.WAITING_FOR_RETRY, null);

        new Verifications() {{
            userMessageLog.setSendAttempts(3);
        }};
    }

    @Test
    public void testUpdateMessageLogNextAttemptDateForRestoredMessage(@Injectable LegConfiguration legConfiguration,
                                                                      @Injectable UserMessageLog userMessageLog) {

        new Expectations() {{
            userMessageLog.getNextAttempt();
            result = FIVE_MINUTES_BEFORE_FIRST_OF_JANUARY_2016;

            legConfiguration.getReceptionAwareness().getStrategy().getAlgorithm();
            result = RetryStrategy.CONSTANT.getAlgorithm();

            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = RETRY_TIMEOUT_IN_MINUTES;

            legConfiguration.getReceptionAwareness().getRetryCount();
            result = RETRY_COUNT;

            domibusPropertyProvider.getLongProperty(MESSAGE_EXPIRATION_DELAY);
            result = 10L;
        }};

        updateRetryLoggingService.updateMessageLogNextAttemptDate(legConfiguration, userMessageLog);


        new FullVerifications() {{
            reprogrammableService.setRescheduleInfo(userMessageLog, (Date) any);
        }};
    }

    /**
     * Max retries limit reached
     * Notification is disabled
     * Clear payload is default (false)
     * Expected result: MessagingDao#clearPayloadData is not called
     * MessageLogDao#setMessageAsSendFailure is called
     * MessageLogDao#setAsNotified() is not called
     */
    @Test
    public void testUpdateRetryLogging_maxRetriesReachedNotificationDisabled_ExpectedMessageStatus_ClearPayloadDisabled(@Injectable UserMessage userMessage,
                                                                                                                        @Injectable UserMessageLog userMessageLog,
                                                                                                                        @Injectable LegConfiguration legConfiguration)  {
        final long entityId = 123;

        new Expectations() {{
            userMessageLog.getSendAttempts();
            result = 2;

            userMessageLog.getSendAttemptsMax();
            result = 3;

            userMessage.getEntityId();
            result = entityId;

            userMessageLogDao.findByEntityId(entityId);
            result = userMessageLog;
        }};

        updateRetryLoggingService.updatePushedMessageRetryLogging(userMessage, legConfiguration, null);

        new Verifications() {{
            messageLogService.setMessageAsSendFailure(userMessage, userMessageLog);
            userMessageLogDao.setAsNotified(userMessageLog);
            times = 0;
        }};

    }

    /**
     * Max retries limit not reached
     * Timeout limit reached
     * Notification is enabled
     * Expected result: MessagingDao#clearPayloadData is called
     * MessageLogDao#setMessageAsSendFailure is called
     * MessageLogDao#setAsNotified() is called
     */
    @Test
    public void testUpdateRetryLogging_timeoutNotificationEnabled_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                                        @Injectable UserMessageLog userMessageLog,
                                                                                        @Injectable LegConfiguration legConfiguration,
                                                                                        @Injectable NotificationStatusEntity notificationStatus) {
//        new SystemMockFirstOfJanuary2016();

        long userMessageEntityId = 123;

        new Expectations() {{
            userMessageLog.getSendAttempts();
            result = 0;

            userMessageLog.getSendAttemptsMax();
            result = 3;

            userMessageLog.getNotificationStatus();
            result = notificationStatus;

            userMessage.getEntityId();
            result = userMessageEntityId;

            userMessageLogDao.findByEntityId(userMessageEntityId);
            result = userMessageLog;
        }};


        updateRetryLoggingService.updatePushedMessageRetryLogging(userMessage, legConfiguration, null);


        new Verifications() {{
            messageLogService.setMessageAsSendFailure(userMessage, userMessageLog);
            messageRetentionService.deletePayloadOnSendFailure(userMessage, userMessageLog);
        }};

    }


    /**
     * Max retries limit not reached
     * Timeout limit reached
     */
    @Test
    public void testUpdateRetryLogging_timeoutNotificationDisabled_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                                         @Injectable UserMessageLog userMessageLog,
                                                                                         @Injectable LegConfiguration legConfiguration) {
        int retryTimeout = 1;

        new Expectations(updateRetryLoggingService) {{
            userMessageLog.getSendAttempts();
            result = 0;

            userMessageLog.getSendAttemptsMax();
            result = 3;

            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = retryTimeout;

            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = ONE_HOUR_BEFORE_FIRST_OF_JANUARY_2016;
        }};


        Assertions.assertFalse(updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration));
    }

    @Test
    public void testUpdateRetryLogging_success_ExpectedMessageStatus(@Injectable UserMessage userMessage,
                                                                     @Injectable UserMessageLog userMessageLog,
                                                                     @Injectable LegConfiguration legConfiguration,
                                                                     @Injectable MessageAttempt messageAttempt)  {

        long userMessageEntityId = 123;
        new Expectations(updateRetryLoggingService) {{
            userMessage.getEntityId();
            result = userMessageEntityId;

            userMessageLogDao.findByEntityId(userMessageEntityId);
            result = userMessageLog;

            updateRetryLoggingService.hasAttemptsLeft(userMessageLog, legConfiguration);
            result = true;

            userMessage.isTestMessage();
            result = false;

            updateRetryLoggingService.updateNextAttemptAndNotify(userMessage, legConfiguration, MessageStatus.WAITING_FOR_RETRY, userMessageLog);
        }};

        updateRetryLoggingService.updateRetryLogging(userMessage, legConfiguration, MessageStatus.WAITING_FOR_RETRY, messageAttempt);

        new Verifications() {{
            userMessageLogDao.update(userMessageLog);
            updateRetryLoggingService.updateNextAttemptAndNotify(userMessage, legConfiguration, MessageStatus.WAITING_FOR_RETRY, userMessageLog);
            messageAttemptService.createAndUpdateEndDate(messageAttempt);
        }};
    }

    @Test
    public void testMessageExpirationDate(@Injectable final UserMessageLog userMessageLog,
                                          @Injectable final LegConfiguration legConfiguration)  {
        final int timeOutInMin = 10; // in minutes
        final long timeOutInMillis = 60000L * timeOutInMin;
        final long restoredTime = System.currentTimeMillis();
        final Date expectedDate = new Date(restoredTime + timeOutInMillis);


        new Expectations(updateRetryLoggingService) {{
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = timeOutInMin;

            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = restoredTime;
        }};

        Date messageExpirationDate = updateRetryLoggingService.getMessageExpirationDate(userMessageLog, legConfiguration);

        assertEquals(expectedDate, messageExpirationDate);
    }

    @Test
    public void testMessageExpirationDateInTheFarFuture(@Injectable final UserMessageLog userMessageLog,
                                                        @Injectable final LegConfiguration legConfiguration)  {
        final int timeOutInMin = 90 * 24 * 60; // 90 days in minutes
        final long timeOutInMillis = 60000L * timeOutInMin;
        final long restoredTime = System.currentTimeMillis();
        final Date expectedDate = new Date(restoredTime + timeOutInMillis);

        new Expectations(updateRetryLoggingService) {{
            legConfiguration.getReceptionAwareness().getRetryTimeout();
            result = timeOutInMin;

            updateRetryLoggingService.getScheduledStartTime(userMessageLog);
            result = restoredTime;
        }};
        Date messageExpirationDate = updateRetryLoggingService.getMessageExpirationDate(userMessageLog, legConfiguration);

        assertEquals(expectedDate, messageExpirationDate);
    }

    @Test
    public void testIsExpired(@Injectable final UserMessageLog userMessageLog,
                              @Injectable final LegConfiguration legConfiguration)  {

        long delay = 10;

        new Expectations(updateRetryLoggingService) {{
            domibusPropertyProvider.getLongProperty(MESSAGE_EXPIRATION_DELAY);
            result = delay;

            updateRetryLoggingService.getMessageExpirationDate(userMessageLog, legConfiguration);
            result = SYSTEM_DATE_IN_MILLIS_FIRST_OF_JANUARY_2016 - delay - 100;

        }};

        boolean result = updateRetryLoggingService.isExpired(legConfiguration, userMessageLog);
        assertTrue(result);
    }

    @Test
    public void test_failIfExpired_MessageExpired_NotSourceMessage(final @Injectable UserMessage userMessage) {
        long userMessageEntityId = 123;

        final UserMessageLog userMessageLog = new UserMessageLog();
        userMessageLog.setSendAttempts(2);
        userMessageLog.setSendAttemptsMax(3);
        MessageStatusEntity messageStatus = new MessageStatusEntity();
        messageStatus.setMessageStatus(MessageStatus.WAITING_FOR_RETRY);
        userMessageLog.setMessageStatus(messageStatus);

        final LegConfiguration legConfiguration = new LegConfiguration();
        legConfiguration.setName("myLegConfiguration");

        new Expectations(updateRetryLoggingService) {{
            userMessage.getEntityId();
            result = userMessageEntityId;

            userMessageLogDao.findByEntityId(userMessageEntityId);
            result = userMessageLog;

            updateRetryLoggingService.isExpired(legConfiguration, userMessageLog);
            result = true;

            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
        }};

        //tested method
        boolean result = updateRetryLoggingService.failIfExpired(userMessage, legConfiguration);
        Assertions.assertTrue(result);

        new FullVerifications(updateRetryLoggingService) {{
            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);

            updateRetryLoggingService.setMessageFailed(userMessage, userMessageLog);
        }};
    }

    @Test
    public void test_failIfExpired_MessageNotExpired_NotSourceMessage(final @Injectable UserMessage userMessage) {
        long userMessageEntityId = 123;

        final UserMessageLog userMessageLog = new UserMessageLog();
        userMessageLog.setSendAttempts(2);
        userMessageLog.setSendAttemptsMax(3);
        MessageStatusEntity messageStatus = new MessageStatusEntity();
        messageStatus.setMessageStatus(MessageStatus.WAITING_FOR_RETRY);
        userMessageLog.setMessageStatus(messageStatus);

        final LegConfiguration legConfiguration = new LegConfiguration();
        legConfiguration.setName("myLegConfiguration");

        new Expectations(updateRetryLoggingService) {{
            userMessage.getEntityId();
            result = userMessageEntityId;

            userMessage.getMessageId();
            result = "some id";

            userMessageLogDao.findByEntityId(userMessageEntityId);
            result = userMessageLog;

            updateRetryLoggingService.isExpired(legConfiguration, userMessageLog);
            result = false;
        }};

        //tested method
        boolean result = updateRetryLoggingService.failIfExpired(userMessage, legConfiguration);
        Assertions.assertFalse(result);

        new FullVerifications() {};
    }

    @Test
    public void setSourceMessageAsFailed_null(@Injectable UserMessage userMessage) {
        final long entityId = 123;

        new Expectations() {{
            userMessage.getEntityId();
            result = entityId;

            userMessage.getMessageId();
            result = "some id";

            userMessageLogDao.findByEntityIdSafely(entityId);
            result = null;
        }};

        updateRetryLoggingService.setSourceMessageAsFailed(userMessage);

        new FullVerifications() {
        };
    }

    @Test
    public void setSourceMessageAsFailed(@Injectable UserMessage userMessage,
                                         @Injectable UserMessageLog messageLog) {
        final long entityId = 123;

        new Expectations() {{
            userMessage.getEntityId();
            result = entityId;

            userMessageLogDao.findByEntityIdSafely(entityId);
            result = messageLog;
        }};

        updateRetryLoggingService.setSourceMessageAsFailed(userMessage);

        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, messageLog);
            times = 1;
        }};
    }

}
