package eu.domibus.core.message.nonrepudiation;

import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.RawEnvelopeDto;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.audit.envers.ModificationType;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.signal.SignalMessageDao;
import eu.domibus.core.util.SoapUtil;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class NonRepudiationDefaultServiceTest {

    @Tested
    NonRepudiationDefaultService nonRepudiationService;

    @Injectable
    SignalMessageRawService signalMessageRawService;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected UserMessageRawEnvelopeDao rawEnvelopeLogDao;

    @Injectable
    protected SoapUtil soapUtil;

    @Injectable
    private SignalMessageDao signalMessageDao;

    @Injectable
    private AuditService auditService;

    @Injectable
    private SignalMessageRawEnvelopeDao signalMessageRawEnvelopeDao;

    @Injectable
    private UserMessageDao userMessageDao;


    @Test
    public void getUserMessageEnvelope_noMessage() {
        String messageId = "msgid";
        new Expectations(nonRepudiationService) {{
            nonRepudiationService.getUserMessageById(messageId, MSHRole.SENDING);
            result = new MessageNotFoundException(messageId);
        }};

        Assertions.assertThrows(MessageNotFoundException.class,
                () -> nonRepudiationService.getUserMessageEnvelope(messageId, MSHRole.SENDING));

    }

    @Test
    public void getUserMessageEnvelope_noMessageEnvelope(@Mocked UserMessage userMessage) {
        String messageId = "msgid";
        new Expectations(nonRepudiationService) {{
            nonRepudiationService.getUserMessageById(messageId, MSHRole.SENDING);
            result = userMessage;

            rawEnvelopeLogDao.findUserMessageEnvelopeById(userMessage.getEntityId());
            result = null;
        }};

        String result = nonRepudiationService.getUserMessageEnvelope(messageId, MSHRole.SENDING);

        assertNull(result);

        new Verifications() {{
            auditService.addMessageEnvelopesDownloadedAudit(messageId, ModificationType.USER_MESSAGE_ENVELOPE_DOWNLOADED);
            times = 0;
        }};
    }

    @Test
    public void getUserMessageEnvelope_ok(@Injectable UserMessage userMessage, @Injectable RawEnvelopeDto rawEnvelopeDto) {
        String messageId = "msgid", envelopContent = "content";

        new Expectations(nonRepudiationService) {{
            nonRepudiationService.getUserMessageById(messageId, MSHRole.SENDING);
            result = userMessage;

            rawEnvelopeLogDao.findUserMessageEnvelopeById(userMessage.getEntityId());
            result = rawEnvelopeDto;

            rawEnvelopeDto.getRawXmlMessage();
            result = envelopContent;
        }};

        String result = nonRepudiationService.getUserMessageEnvelope(messageId, MSHRole.SENDING);

        assertEquals(envelopContent, result);

        new Verifications() {{
            auditService.addMessageEnvelopesDownloadedAudit(messageId, ModificationType.USER_MESSAGE_ENVELOPE_DOWNLOADED);
            times = 1;
        }};
    }

    @Test
    public void getSignalMessageEnvelope_noMessage() {
        String userMessageId = "msgid";

        new Expectations() {{
            signalMessageRawEnvelopeDao.findSignalMessageByUserMessageId(userMessageId, MSHRole.RECEIVING);
            result = null;
        }};
        String result = nonRepudiationService.getSignalMessageEnvelope(userMessageId, MSHRole.RECEIVING);

        assertNull(result);

        new Verifications() {{
            auditService.addMessageEnvelopesDownloadedAudit(userMessageId, ModificationType.SIGNAL_MESSAGE_ENVELOPE_DOWNLOADED);
            times = 0;
        }};
    }

    @Test
    public void getSignalMessageEnvelope_ok(@Injectable RawEnvelopeDto rawEnvelopeDto) {
        String userMessageId = "msgid";
        String rawXml = "rawXml";

        new Expectations() {{
            signalMessageRawEnvelopeDao.findSignalMessageByUserMessageId(userMessageId, MSHRole.RECEIVING);
            result = rawEnvelopeDto;

            rawEnvelopeDto.getRawXmlMessage();
            result = rawXml;
        }};
        String result = nonRepudiationService.getSignalMessageEnvelope(userMessageId, MSHRole.RECEIVING);

        assertEquals(rawXml, result);

        new Verifications() {{
            auditService.addMessageEnvelopesDownloadedAudit(userMessageId, ModificationType.SIGNAL_MESSAGE_ENVELOPE_DOWNLOADED);
            times = 1;
        }};
    }

    @Test
    public void getMessageEnvelopes() {
        String userMessageId = "msgid", userMessageEnvelope = "userMessageEnvelope", signalEnvelope = "signalEnvelope";
        new Expectations(nonRepudiationService) {{
            nonRepudiationService.getUserMessageEnvelope(userMessageId, MSHRole.SENDING);
            result = userMessageEnvelope;

            nonRepudiationService.getSignalMessageEnvelope(userMessageId, MSHRole.SENDING);
            result = signalEnvelope;
        }};

        Map<String, InputStream> result = nonRepudiationService.getMessageEnvelopes(userMessageId, MSHRole.SENDING);

        assertNotNull(result.get("user_message_envelope.xml"));
        assertNotNull(result.get("signal_message_envelope.xml"));
        assertEquals(2, result.size());
    }
}
