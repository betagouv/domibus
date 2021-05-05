package eu.domibus.core.message.pull;

import mockit.integration.junit4.JMockit;
import org.junit.runner.RunWith;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@RunWith(JMockit.class)
public class Ebms3PullRequestHandlerImplTest {

   /* @Injectable
    MessageExchangeService messageExchangeService;

    @Injectable
    MessagingDao messagingDao;

    @Injectable
    EbMS3MessageBuilder messageBuilder;

    @Injectable
    ReliabilityChecker reliabilityChecker;

    @Injectable
    ReliabilityMatcher pullRequestMatcher;

    @Injectable
    MessageAttemptService messageAttemptService;

    @Injectable
    RetryService retryService;

    @Injectable
    ReliabilityService reliabilityService;

    @Injectable
    PullMessageService pullMessageService;

    @Tested
    PullRequestHandler pullRequestHandler;


    @Test
    public void testHandlePullRequestMessageFoundWithErro(
            @Mocked final Process process,
            @Mocked final LegConfiguration legConfiguration,
            @Mocked final PullContext pullContext) throws EbMS3Exception {
        final String mpcQualifiedName = "defaultMPC";

        Messaging messaging = new Messaging();
        SignalMessage signalMessage = new SignalMessage();
        final PullRequest pullRequest = new PullRequest();
        pullRequest.setMpc(mpcQualifiedName);
        signalMessage.setPullRequest(pullRequest);
        messaging.setSignalMessage(signalMessage);

        final UserMessage userMessage = new MessageTestUtility().createSampleUserMessage();
        final String messageId = userMessage.getMessageInfo().getMessageId();
        final EbMS3Exception ebMS3Exception = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0001, "Payload in body must be valid XML", messageId, null);


        new Expectations() {{

            pullContext.filterLegOnMpc();
            result = legConfiguration;

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

            messageBuilder.buildSOAPMessage(userMessage, legConfiguration);
            result = ebMS3Exception;
        }};
        pullRequestHandler.handleRequest(messageId, pullContext);
        new Verifications() {{
            Error error;
            messageBuilder.buildSOAPFaultMessage(error = withCapture());
            error.equals(ebMS3Exception.getFaultInfoError());
            times = 1;

            pullMessageService.updatePullMessageAfterRequest(userMessage, messageId, legConfiguration, ReliabilityChecker.CheckResult.PULL_FAILED);

            times = 1;

        }};
    }

    @Test
    public void testHandlePullRequestMessageFound(
            @Mocked final PhaseInterceptorChain pi,
            @Mocked final Process process,
            @Mocked final LegConfiguration legConfiguration,
            @Mocked final PullContext pullContext) throws EbMS3Exception {
        final String mpcQualifiedName = "defaultMPC";

        Messaging messaging = new Messaging();
        SignalMessage signalMessage = new SignalMessage();
        final PullRequest pullRequest = new PullRequest();
        pullRequest.setMpc(mpcQualifiedName);
        signalMessage.setPullRequest(pullRequest);
        messaging.setSignalMessage(signalMessage);

        final UserMessage userMessage = new MessageTestUtility().createSampleUserMessage();
        final String messageId = userMessage.getMessageInfo().getMessageId();


        new Expectations() {{
            legConfiguration.getReliability().isNonRepudiation();
            result = true;

            pullRequestMatcher.matchReliableCallBack(withAny(legConfiguration.getReliability()));
            result = true;

            pullContext.filterLegOnMpc();
            result = legConfiguration;

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;
        }};
        pullRequestHandler.handleRequest(messageId, pullContext);
        new Verifications() {{

            PhaseInterceptorChain.getCurrentMessage().getExchange().put(MSHDispatcher.MESSAGE_TYPE_OUT, MessageType.USER_MESSAGE);
            times = 1;

            PhaseInterceptorChain.getCurrentMessage().getExchange().put(DispatchClientDefaultProvider.MESSAGE_ID, messageId);
            times = 1;

            messageBuilder.buildSOAPMessage(userMessage, legConfiguration);
            times = 1;

            pullMessageService.updatePullMessageAfterRequest(userMessage, messageId, legConfiguration, ReliabilityChecker.CheckResult.WAITING_FOR_CALLBACK);
            times = 1;

        }};
    }

    @Test
    public void testHandlePullRequestNoMessageFound(@Injectable ReliabilityMatcher pullReceiptMatcher,
                                                    @Injectable ReliabilityMatcher pullRequestMatcher,
                                                    @Mocked final PhaseInterceptorChain pi,
                                                    @Mocked final Process process,
                                                    @Mocked final LegConfiguration legConfiguration,
                                                    @Mocked final PullContext pullContext) throws EbMS3Exception {

        pullRequestHandler.notifyNoMessage(pullContext, null);
        new Verifications() {{

            EbMS3Exception exception;
            messageBuilder.getSoapMessage(exception = withCapture());
            Assert.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0006, exception.getErrorCode());
        }};
    }

    @Test
    public void testHandlePullRequestWithInvalidSenderCertificate(
            @Mocked final PhaseInterceptorChain pi,
            @Mocked final Process process,
            @Mocked final UserMessage userMessage,
            @Mocked final LegConfiguration legConfiguration,
            @Mocked final PullContext pullContext) throws EbMS3Exception {

        final String messageId = "whatEverId";

        new Expectations() {{

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;
            messageExchangeService.verifySenderCertificate(legConfiguration, pullContext.getResponder().getName());
            result = new DomibusCertificateException("test");


        }};
        pullRequestHandler.handleRequest(messageId, pullContext);
        new Verifications() {{
            EbMS3Exception e = null;
            reliabilityChecker.handleEbms3Exception(e = withCapture(), messageId);
            times = 1;
            Assert.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0101, e.getErrorCode());
            Error faultInfo = null;
            messageBuilder.buildSOAPFaultMessage(faultInfo = withCapture());
            times = 1;
            Assert.assertEquals("EBMS:0101", faultInfo.getErrorCode());

            pullMessageService.updatePullMessageAfterRequest(userMessage, messageId, legConfiguration, ReliabilityChecker.CheckResult.PULL_FAILED);
            times = 1;
            MessageAttempt attempt = null;
            messageAttemptService.create(withAny(attempt));
            times = 1;


        }};
    }

    @Test
    public void testHandlePullRequestConfigurationException(
            @Mocked final LegConfiguration legConfiguration,
            @Mocked final UserMessage userMessage,
            @Mocked final PullContext pullContext) throws EbMS3Exception {

        final String messageId = "whatEverId";

        new Expectations() {{

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

            messageExchangeService.verifySenderCertificate(legConfiguration, pullContext.getResponder().getName());
            result = new ConfigurationException();

        }};
        pullRequestHandler.handleRequest(messageId, pullContext);
        new Verifications() {{
            EbMS3Exception e = null;
            reliabilityChecker.handleEbms3Exception(e = withCapture(), messageId);
            times = 1;
            Assert.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0010, e.getErrorCode());
            Error faultInfo = null;
            messageBuilder.buildSOAPFaultMessage(faultInfo = withCapture());
            times = 1;
            Assert.assertEquals("EBMS:0010", faultInfo.getErrorCode());

            pullMessageService.updatePullMessageAfterRequest(userMessage, messageId, legConfiguration, ReliabilityChecker.CheckResult.PULL_FAILED);
            times = 1;
            MessageAttempt attempt = null;
            messageAttemptService.create(withAny(attempt));
            times = 1;

        }};
    }

    @Test
    public void testHandlePullRequestWithInvalidReceiverCertificate(
            @Mocked final UserMessage userMessage,
            @Mocked final LegConfiguration legConfiguration,
            @Mocked final PullContext pullContext) throws EbMS3Exception {

        final String messageId = "whatEverID";
        new Expectations() {{

            messagingDao.findUserMessageByMessageId(messageId);
            result = userMessage;

            messageExchangeService.verifyReceiverCertificate(legConfiguration, pullContext.getInitiator().getName());
            result = new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, "invalid certificate");


        }};
        pullRequestHandler.handleRequest(messageId, pullContext);
        new Verifications() {{
            messageBuilder.buildSOAPFaultMessage(withAny(new Error()));
            times = 0;
            pullMessageService.updatePullMessageAfterRequest(userMessage, messageId, legConfiguration, ReliabilityChecker.CheckResult.ABORT);
            times = 1;
            MessageAttempt attempt = null;
            messageAttemptService.create(withAny(attempt));
            times = 0;
        }};
    }*/
}