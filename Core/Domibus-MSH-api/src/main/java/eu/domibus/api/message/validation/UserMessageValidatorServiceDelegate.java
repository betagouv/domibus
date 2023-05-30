package eu.domibus.api.message.validation;

import eu.domibus.api.usermessage.domain.UserMessage;

import javax.xml.soap.SOAPMessage;
import java.io.InputStream;

public interface UserMessageValidatorServiceDelegate {

    void validateIncomingMessage(SOAPMessage request, eu.domibus.api.usermessage.domain.UserMessage userMessage);

    void validatePayload(InputStream payload, String mimeType);

    void validate(UserMessage userMessageModel);
}
