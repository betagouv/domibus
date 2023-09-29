package eu.domibus.core.ebms3.receiver;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.message.SignalMessageSoapEnvelopeSpiDelegate;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.receiver.handler.IncomingMessageHandler;
import eu.domibus.core.ebms3.receiver.handler.IncomingMessageHandlerFactory;
import eu.domibus.core.ebms3.sender.client.DispatchClientDefaultProvider;
import eu.domibus.core.util.MessageUtil;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.jupiter.api.Assertions;
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
    public void testInvokeHappyFlow(@Injectable SOAPMessage request,
                                    @Injectable Ebms3Messaging messaging,
                                    @Injectable IncomingMessageHandler messageHandler) throws EbMS3Exception {
        MessageImpl message = getMessage(messaging);
        new MockUp<PhaseInterceptorChain>() {
            @Mock
            public Message getCurrentMessage() {
                return message;
            }
        };
        new Expectations() {{
            incomingMessageHandlerFactory.getMessageHandler(request, messaging);
            result = messageHandler;
        }};

        mshWebservice.invoke(request);

        new Verifications() {{
            messageHandler.processMessage(request, messaging);
        }};
    }

    public static MessageImpl getMessage(Ebms3Messaging messaging) {
        MessageImpl message1 = new MessageImpl();
        ExchangeImpl e = new ExchangeImpl();
        message1.setExchange(e);
        message1.put(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY, messaging);
        e.put(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY, messaging);
        return message1;
    }

    @Test
    void testInvokeNoHandlerFound(@Injectable SOAPMessage request,
                                  @Injectable Ebms3Messaging messaging,
                                  @Injectable IncomingMessageHandler messageHandler) throws EbMS3Exception {
        MessageImpl message = getMessage(messaging);
        new MockUp<PhaseInterceptorChain>() {
            @Mock
            public Message getCurrentMessage() {
                return message;
            }
        };
        new Expectations() {{
            incomingMessageHandlerFactory.getMessageHandler(request, messaging);
            result = null;
        }};

        Assertions.assertThrows(WebServiceException.class, () -> mshWebservice.invoke(request));

        new Verifications() {{
            messageHandler.processMessage(request, messaging);
            times = 0;
        }};
    }
}
