package eu.domibus.core.ebms3.receiver.handler;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pmode.PModeConstants;
import eu.domibus.common.ErrorResult;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.core.ebms3.sender.ResponseHandler;
import eu.domibus.core.ebms3.ws.attachment.AttachmentCleanupService;
import eu.domibus.core.generator.id.MessageIdGenerator;
import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.message.MessagingService;
import eu.domibus.core.message.UserMessageHandlerService;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.compression.CompressionService;
import eu.domibus.core.message.nonrepudiation.UserMessageRawEnvelopeDao;
import eu.domibus.core.message.pull.PullMessageService;
import eu.domibus.core.message.pull.PullRequestHandler;
import eu.domibus.core.message.reliability.ReliabilityChecker;
import eu.domibus.core.message.reliability.ReliabilityMatcher;
import eu.domibus.core.message.reliability.ReliabilityService;
import eu.domibus.core.message.signal.SignalMessageDao;
import eu.domibus.core.message.signal.SignalMessageLogDao;
import eu.domibus.core.payload.PayloadProfileValidator;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pmode.validation.validators.PropertyProfileValidator;
import eu.domibus.core.security.AuthorizationServiceImpl;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.TimestampDateFormatter;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerFactory;
import javax.xml.ws.WebServiceException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JMockit.class)
public class IncomingUserMessageHandlerTest {

    @Tested
    IncomingUserMessageHandler incomingUserMessageHandler;

    @Injectable
    BackendNotificationService backendNotificationService;

    @Injectable
    IncomingMessageHandlerFactory incomingMessageHandlerFactory;

    @Injectable
    UserMessageRawEnvelopeDao rawEnvelopeLogDao;

    @Injectable
    MessagingService messagingService;

    @Injectable
    SignalMessageDao signalMessageDao;

    @Injectable
    SignalMessageLogDao signalMessageLogDao;

    @Injectable
    MessageFactory messageFactory;

    @Injectable
    UserMessageLogDao userMessageLogDao;

    @Injectable
    JAXBContext jaxbContext;

    @Injectable
    TransformerFactory transformerFactory;

    @Injectable
    PModeProvider pModeProvider;

    @Injectable
    TimestampDateFormatter timestampDateFormatter;

    @Injectable
    CompressionService compressionService;

    @Injectable
    MessageIdGenerator messageIdGenerator;

    @Injectable
    PayloadProfileValidator payloadProfileValidator;

    @Injectable
    PropertyProfileValidator propertyProfileValidator;

    @Injectable
    CertificateService certificateService;

    @Injectable
    SOAPMessage soapRequestMessage;

    @Injectable
    SOAPMessage soapResponseMessage;

    @Injectable
    MessageExchangeService messageExchangeService;

    @Injectable
    EbMS3MessageBuilder messageBuilder;

    @Injectable
    UserMessageHandlerService userMessageHandlerService;

    @Injectable
    ResponseHandler responseHandler;

    @Injectable
    ReliabilityChecker reliabilityChecker;


    @Injectable
    ReliabilityMatcher pullReceiptMatcher;

    @Injectable
    ReliabilityMatcher pullRequestMatcher;

    @Injectable
    PullRequestHandler pullRequestHandler;

    @Injectable
    ReliabilityService reliabilityService;

    @Injectable
    PullMessageService pullMessageService;

    @Injectable
    MessageUtil messageUtil;

    @Injectable
    AttachmentCleanupService attachmentCleanupService;

    @Injectable
    AuthorizationServiceImpl authorizationService;
    @Injectable
    Ebms3Converter ebms3Converter;


    /**
     * Happy flow unit testing with actual data
     */
    @Test
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void testInvoke_tc1Process_HappyFlow(@Injectable Ebms3Messaging messaging,
                                                @Injectable LegConfiguration legConfiguration,
                                                @Injectable final UserMessage userMessage) throws Exception {

        final String pmodeKey = "blue_gw:red_gw:testService1:tc1Action:OAE:pushTestcase1tc1Action";

        new Expectations(incomingUserMessageHandler) {{
            soapRequestMessage.getProperty(PModeConstants.PMODE_KEY_CONTEXT_PROPERTY);
            result = pmodeKey;

            userMessageHandlerService.handleNewUserMessage(legConfiguration, withEqual(pmodeKey), withEqual(soapRequestMessage), withEqual(userMessage), null, null, false);
            result = soapResponseMessage;
        }};

        incomingUserMessageHandler.processMessage(soapRequestMessage, messaging);

        new Verifications() {{
            backendNotificationService.notifyMessageReceivedFailure(userMessage, null, (ErrorResult) any);
            times = 0;
        }};
    }


    /**
     * Unit testing with actual data.
     */
    @Test
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void testInvoke_ErrorInNotifyingIncomingMessage(@Injectable final LegConfiguration legConfiguration,
                                                           @Injectable final Ebms3Messaging messaging,
                                                           @Injectable final UserMessage userMessage) throws Exception {

        final String pmodeKey = "blue_gw:red_gw:testService1:tc1Action:OAE:pushTestcase1tc1Action";

        new Expectations(incomingUserMessageHandler) {{

            legConfiguration.getErrorHandling().isBusinessErrorNotifyConsumer();
            result = true;

            userMessageHandlerService.handleNewUserMessage(legConfiguration, withAny(pmodeKey), withAny(soapRequestMessage), withAny(userMessage), null, null, false);
            result = EbMS3ExceptionBuilder.getInstance().build();

        }};

        try {
            incomingUserMessageHandler.processMessage(soapRequestMessage, messaging);
            fail();
        } catch (Exception e) {
            assertTrue("Expecting Webservice exception!", e instanceof WebServiceException);
        }

        new Verifications() {{
            backendNotificationService.notifyMessageReceivedFailure(userMessage, null, (ErrorResult) any);
        }};
    }
}