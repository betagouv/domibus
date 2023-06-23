package eu.domibus.plugin.ws.backend.reliability.queue;

import eu.domibus.api.security.AuthUtils;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.AuthenticationExtService;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogDao;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import eu.domibus.plugin.ws.backend.dispatch.WSPluginMessageSender;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.JMSException;
import javax.jms.Message;

import static eu.domibus.plugin.ws.backend.WSBackendMessageType.RECEIVE_SUCCESS;
import static eu.domibus.plugin.ws.backend.WSBackendMessageType.SUBMIT_MESSAGE;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class WSSendMessageListenerTest {
    public static final long ID = 1L;
    @Tested
    private WSSendMessageListener wsSendMessageListener;

    @Injectable
    private WSPluginMessageSender wsPluginMessageSender;
    @Injectable
    private WSBackendMessageLogDao wsBackendMessageLogDao;
    @Injectable
    private DomainContextExtService domainContextExtService;
    @Injectable
    private AuthenticationExtService authenticationExtService;
    @Injectable
    private Message message;

    @Injectable
    private WSBackendMessageLogEntity backendMessage;

    @Test
    public void onMessage_JMSException() throws JMSException {
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = new JMSException("ERROR");
        }};
        wsSendMessageListener.doOnMessage(message);

        new FullVerifications() {
        };
    }

    @Test
    public void onMessage_noMessage() throws JMSException {
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = MessageConstants.DOMAIN;

            message.getStringProperty(MessageConstants.MESSAGE_ID);
            result = MessageConstants.MESSAGE_ID;

            message.getLongProperty(WSSendMessageListener.ID);
            result = ID;

            message.getStringProperty(WSSendMessageListener.TYPE);
            result = SUBMIT_MESSAGE.name();

            wsBackendMessageLogDao.getById(ID);
            result = null;

        }};

        wsSendMessageListener.doOnMessage(message);

        new FullVerifications() {{
            DomainDTO domain;
            domainContextExtService.setCurrentDomain(domain = withCapture());

            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getCode());
            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getName());
        }};
    }

    @Test
    public void onMessage_wrongId() throws JMSException {
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = MessageConstants.DOMAIN;

            message.getStringProperty(MessageConstants.MESSAGE_ID);
            result = MessageConstants.MESSAGE_ID;

            message.getLongProperty(WSSendMessageListener.ID);
            result = ID;

            message.getStringProperty(WSSendMessageListener.TYPE);
            result = SUBMIT_MESSAGE.name();

            wsBackendMessageLogDao.getById(ID);
            result = backendMessage;

            backendMessage.getMessageId();
            result = "nope";

            backendMessage.getMessageIds();
            result = "nope";

            backendMessage.getType();
            result = WSBackendMessageType.DELETED_BATCH;
        }};
        wsSendMessageListener.doOnMessage(message);

        new FullVerifications() {{
            DomainDTO domain;
            domainContextExtService.setCurrentDomain(domain = withCapture());

            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getCode());
            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getName());
        }};
    }

    @Test
    public void onMessage_wrongType() throws JMSException {
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = MessageConstants.DOMAIN;

            message.getStringProperty(MessageConstants.MESSAGE_ID);
            result = MessageConstants.MESSAGE_ID;

            message.getLongProperty(WSSendMessageListener.ID);
            result = ID;

            message.getStringProperty(WSSendMessageListener.TYPE);
            result = SUBMIT_MESSAGE.name();

            wsBackendMessageLogDao.getById(ID);
            result = backendMessage;

            backendMessage.getMessageId();
            result = MessageConstants.MESSAGE_ID;

            backendMessage.getType();
            result = RECEIVE_SUCCESS;
        }};
        wsSendMessageListener.doOnMessage(message);

        new FullVerifications() {{
            DomainDTO domain;
            domainContextExtService.setCurrentDomain(domain = withCapture());

            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getCode());
            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getName());
        }};
    }

    @Test
    public void onMessage_() throws JMSException {
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = MessageConstants.DOMAIN;

            message.getStringProperty(MessageConstants.MESSAGE_ID);
            result = MessageConstants.MESSAGE_ID;

            message.getLongProperty(WSSendMessageListener.ID);
            result = ID;

            message.getStringProperty(WSSendMessageListener.TYPE);
            result = SUBMIT_MESSAGE.name();

            wsBackendMessageLogDao.getById(ID);
            result = backendMessage;

            backendMessage.getMessageId();
            result = MessageConstants.MESSAGE_ID;

            backendMessage.getType();
            result = SUBMIT_MESSAGE;
        }};
        wsSendMessageListener.doOnMessage(message);

        new FullVerifications() {{
            DomainDTO domain;
            domainContextExtService.setCurrentDomain(domain = withCapture());

            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getCode());
            Assertions.assertEquals(MessageConstants.DOMAIN, domain.getName());

            wsPluginMessageSender.sendNotification(backendMessage);
            times = 1;

            backendMessage.setScheduled(false);
            times = 1;
        }};
    }
}
