package eu.domibus.core.ebms3.receiver;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.message.SignalMessageSoapEnvelopeSpiDelegate;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.receiver.handler.IncomingMessageHandler;
import eu.domibus.core.ebms3.receiver.handler.IncomingMessageHandlerFactory;
import eu.domibus.core.ebms3.sender.client.DispatchClientDefaultProvider;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;

/**
 * @author Arun Raj
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class MSHWebServiceTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MSHWebServiceTest.class);
    private static final String VALID_PMODE_CONFIG_URI = "samplePModes/domibus-configuration-valid.xml";

    @Tested
    MSHWebservice mshWebservice;

    @Injectable
    MessageUtil messageUtil;

    @Injectable
    IncomingMessageHandlerFactory incomingMessageHandlerFactory;

    @Injectable
    SignalMessageSoapEnvelopeSpiDelegate signalMessageSoapEnvelopeSpiDelegate;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Test
    @Disabled("EDELIVERY-6896")
    public void testInvokeHappyFlow(@Injectable SOAPMessage request,
                                    @Injectable Ebms3Messaging messaging,
                                    @Injectable IncomingMessageHandler messageHandler,
                                    @Mocked PhaseInterceptorChain interceptors) throws EbMS3Exception {
        new Expectations() {{
            PhaseInterceptorChain.getCurrentMessage().get(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY);
            result = messaging;

            incomingMessageHandlerFactory.getMessageHandler(request, messaging);
            result = messageHandler;
        }};

        mshWebservice.invoke(request);

        new Verifications() {{
            messageHandler.processMessage(request, messaging);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    void testInvokeNoHandlerFound(@Injectable SOAPMessage request,
                                         @Injectable Ebms3Messaging messaging,
                                         @Injectable IncomingMessageHandler messageHandler) throws EbMS3Exception {
        new Expectations() {{
            PhaseInterceptorChain.getCurrentMessage().get(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY);
            result = messaging;

            incomingMessageHandlerFactory.getMessageHandler(request, messaging);
            result = null;
        }};

        Assertions.assertThrows(WebServiceException. class,() -> mshWebservice.invoke(request));

        new Verifications() {{
            messageHandler.processMessage(request, messaging);
            times = 0;
        }};
    }
}
