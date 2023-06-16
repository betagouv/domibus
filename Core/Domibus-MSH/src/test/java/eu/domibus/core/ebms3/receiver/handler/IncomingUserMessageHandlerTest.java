package eu.domibus.core.ebms3.receiver.handler;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.message.validation.UserMessageValidatorSpiService;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.pmode.PModeConstants;
import eu.domibus.common.ErrorResult;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.ws.attachment.AttachmentCleanupService;
import eu.domibus.core.message.UserMessageErrorCreator;
import eu.domibus.core.message.UserMessageHandlerService;
import eu.domibus.core.message.UserMessagePayloadService;
import eu.domibus.core.message.TestMessageValidator;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.security.AuthorizationServiceImpl;
import eu.domibus.core.util.MessageUtil;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class IncomingUserMessageHandlerTest {

    @Tested
    IncomingUserMessageHandler incomingUserMessageHandler;

    @Injectable
    UserMessagePayloadService userMessagePayloadService;

    @Injectable
    BackendNotificationService backendNotificationService;

    @Injectable
    UserMessageHandlerService userMessageHandlerService;

    @Injectable
    MessageUtil messageUtil;

    @Injectable
    PModeProvider pModeProvider;

    @Injectable
    Ebms3Converter ebms3Converter;

    @Injectable
    SOAPMessage soapRequestMessage;

    @Injectable
    SOAPMessage soapResponseMessage;

    @Injectable
    AttachmentCleanupService attachmentCleanupService;

    @Injectable
    AuthorizationServiceImpl authorizationService;

    @Injectable
    UserMessageValidatorSpiService userMessageValidatorSpiService;

    @Injectable
    TestMessageValidator testMessageValidator;

    @Injectable
    UserMessageErrorCreator userMessageErrorCreator;

    @Injectable
    MshRoleDao mshRoleDao;

    @Injectable
    MetricRegistry metricRegistry;

    /**
     * Happy flow unit testing with actual data
     */
    @Test
    public void testInvoke_tc1Process_HappyFlow(@Injectable Ebms3Messaging messaging,
                                                @Injectable LegConfiguration legConfiguration,
                                                @Injectable final UserMessage userMessage) throws Exception {

        final String pmodeKey = "blue_gw:red_gw:testService1:tc1Action:OAE:pushTestcase1tc1Action";

        new Expectations() {{
            soapRequestMessage.getProperty(PModeConstants.PMODE_KEY_CONTEXT_PROPERTY);
            result = pmodeKey;

            userMessageHandlerService.handleNewUserMessage(legConfiguration, withEqual(pmodeKey), withEqual(soapRequestMessage), withEqual(userMessage), null, null, false);
            result = soapResponseMessage;
        }};

        incomingUserMessageHandler.processMessage(soapRequestMessage, messaging);

        new Verifications() {{
            backendNotificationService.notifyMessageReceivedFailure(userMessage, (ErrorResult) any);
            times = 0;
        }};
    }


    /**
     * Unit testing with actual data.
     */
    @Test
    public void testInvoke_ErrorInNotifyingIncomingMessage(@Injectable final LegConfiguration legConfiguration,
                                                           @Injectable final Ebms3Messaging messaging,
                                                           @Injectable final UserMessage userMessage) throws Exception {

        final String pmodeKey = "blue_gw:red_gw:testService1:tc1Action:OAE:pushTestcase1tc1Action";

        new Expectations() {{

            legConfiguration.getErrorHandling().isBusinessErrorNotifyConsumer();
            result = true;

            userMessageHandlerService.handleNewUserMessage(legConfiguration, withAny(pmodeKey), withAny(soapRequestMessage), withAny(userMessage), null, null, false);
            result = EbMS3ExceptionBuilder.getInstance().build();

        }};

        try {
            incomingUserMessageHandler.processMessage(soapRequestMessage, messaging);
            fail();
        } catch (WebServiceException e) {
            //OK
        }

        new Verifications() {{
            backendNotificationService.notifyMessageReceivedFailure(userMessage, (ErrorResult) any);
        }};
    }
}
