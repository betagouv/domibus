package eu.domibus.core.plugin.handler;

import eu.domibus.api.message.UserMessageSecurityService;
import eu.domibus.api.messaging.DuplicateMessageFoundException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.pmode.PModeConstants;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.common.ErrorResult;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.message.MessagingService;
import eu.domibus.core.message.UserMessageDefaultService;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.messaging.DuplicateMessageException;
import eu.domibus.messaging.MessageNotFoundException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
@ExtendWith(JMockitExtension.class)
public class MessageRetrieverImplTest {
    private static final String MESS_ID = UUID.randomUUID().toString();

    private static final String GREEN = "green_gw";
    private static final String RED = "red_gw";
    private static final String AGREEMENT = "";
    private static final String SERVICE = "testService1";
    private static final String ACTION = "TC2Leg1";
    private static final String LEG = "pushTestcase1tc2Action";

    private final String pModeKey = GREEN + PModeConstants.PMODEKEY_SEPARATOR +
            RED + PModeConstants.PMODEKEY_SEPARATOR +
            SERVICE + PModeConstants.PMODEKEY_SEPARATOR +
            ACTION + PModeConstants.PMODEKEY_SEPARATOR +
            AGREEMENT + PModeConstants.PMODEKEY_SEPARATOR +
            LEG;

    @Tested
    MessageRetrieverImpl messageRetriever;

    @Injectable
    protected UserMessageSecurityService userMessageSecurityService;

    @Injectable
    protected UserMessageDefaultService userMessageService;

    @Injectable
    private MessagingService messagingService;

    @Injectable
    private UserMessageLogDefaultService userMessageLogService;

    @Injectable
    private ErrorLogService errorLogService;

    @Injectable
    protected ApplicationEventPublisher applicationEventPublisher;

    @Injectable
    AuthUtils authUtils;

    @Test
    public void testDownloadMessageOK(@Injectable UserMessage userMessage,
                                      @Injectable UserMessageLog userMessageLog) throws Exception {

        new Expectations() {{
            userMessageService.getByMessageId(MESS_ID, MSHRole.RECEIVING);
            result = userMessage;
            userMessageLogService.findById(anyLong);
            result = userMessageLog;
        }};

        messageRetriever.downloadMessage(MESS_ID);

        new Verifications() {{
            userMessageLogService.setMessageAsDownloaded(userMessage, userMessageLog);

        }};
    }

    @Test
    public void testDownloadMessageOK_RetentionNonZero(@Injectable UserMessage userMessage,
                                                       @Injectable final UserMessageLog messageLog) throws Exception {
        new Expectations(messageRetriever) {{
            userMessageService.getByMessageId(MESS_ID, MSHRole.RECEIVING);
            result = userMessage;

            userMessageLogService.findById(anyLong);
            result = messageLog;

        }};

        messageRetriever.downloadMessage(MESS_ID);

        new Verifications() {{
            userMessageLogService.setMessageAsDownloaded(userMessage, messageLog);
        }};
    }

    @Test
    public void testGetErrorsForMessageOk(@Injectable ErrorLogEntry errorLogEntry, @Injectable UserMessageLog userMessageLog) throws MessageNotFoundException, DuplicateMessageException {
        List<ErrorLogEntry> list = new ArrayList<>();
        list.add(errorLogEntry);
        new Expectations() {{
            errorLogService.getErrorsForMessage(MESS_ID);
            result = list;
        }};

        final List<? extends ErrorResult> results = messageRetriever.getErrorsForMessage(MESS_ID);

        new Verifications() {{
            errorLogService.convert(errorLogEntry);
            times = 1;
            Assertions.assertNotNull(results);
        }};

    }


    @Test
    public void browseMessage(@Injectable UserMessage userMessage) {
        String messageId = "123";

        new Expectations(messageRetriever) {{
            userMessageService.getByMessageId(messageId, MSHRole.RECEIVING);
            result = userMessage;
        }};

        messageRetriever.browseMessage(messageId);

        new Verifications() {{
//            messageRetriever.checkMessageAuthorization(userMessage);
            messagingService.getSubmission(userMessage);
        }};
    }

}
