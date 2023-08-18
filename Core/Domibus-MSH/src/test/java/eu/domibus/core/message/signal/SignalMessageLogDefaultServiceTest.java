package eu.domibus.core.message.signal;

import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.api.model.SignalMessage;
import eu.domibus.api.model.SignalMessageLog;
import eu.domibus.core.message.MessageStatusDao;
import eu.domibus.core.message.dictionary.MshRoleDao;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * @author Tiago Miguel
 * @since 4.0
 */
@SuppressWarnings("DataFlowIssue")
class SignalMessageLogDefaultServiceTest {

    @Tested
    SignalMessageLogDefaultService signalMessageLogDefaultService;

    @Injectable
    SignalMessageLogDao signalMessageLogDao;

    @Injectable
    MessageStatusDao messageStatusDao;

    @Injectable
    MshRoleDao mshRoleDao;

    static Stream<Arguments> testSave() {
        return Stream.of(
                Arguments.of("service", "action"),
                Arguments.of(Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} - {1}")
    @MethodSource
    void testSave(String userMessageService, String userMessageAction) {
        final String messageId = "1";
        final String signalMessageId = "signal-" + messageId;
        SignalMessage signalMessage = new SignalMessage();
        signalMessage.setSignalMessageId(signalMessageId);
        signalMessage.setRefToMessageId(messageId);
        signalMessageLogDefaultService.save(signalMessage, userMessageService, userMessageAction);

        new Verifications() {{
            SignalMessageLog signalMessageLog;
            signalMessageLogDao.create(signalMessageLog = withCapture());

            Assertions.assertEquals(signalMessageId, signalMessageLog.getSignalMessage().getSignalMessageId());
            Assertions.assertEquals(messageId, signalMessageLog.getSignalMessage().getRefToMessageId());
        }};
    }
}
