package eu.domibus.ext.delegate.services.message;

import eu.domibus.api.message.validation.UserMessageValidatorServiceDelegate;
import eu.domibus.core.spi.validation.UserMessageValidatorSpi;
import eu.domibus.ext.delegate.mapper.DomibusExtMapper;
import eu.domibus.ext.domain.UserMessageDTO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.soap.SOAPMessage;
import java.io.InputStream;

@Service
public class UserMessageValidatorServiceDelegateImpl implements UserMessageValidatorServiceDelegate {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageValidatorServiceDelegateImpl.class);

    protected UserMessageValidatorSpi userMessageValidatorSpi;
    protected DomibusExtMapper domibusExtMapper;

    public UserMessageValidatorServiceDelegateImpl(@Autowired(required = false) UserMessageValidatorSpi userMessageValidatorSpi,
                                                   DomibusExtMapper domibusExtMapper) {
        this.userMessageValidatorSpi = userMessageValidatorSpi;
        this.domibusExtMapper = domibusExtMapper;
    }

    @Override
    public void validateIncomingMessage(SOAPMessage request, eu.domibus.api.usermessage.domain.UserMessage userMessage) {
        if (!isUserMessageValidatorActive()) {
            LOG.debug("Validation skipped: validator SPI is not active");
            return;
        }
        LOG.debug("Validating incoming user message");

        final UserMessageDTO userMessageDto = domibusExtMapper.userMessageToUserMessageDTO(userMessage);
        userMessageValidatorSpi.validateIncomingUserMessage(request, userMessageDto);

        LOG.debug("Finished validating incoming user message");
    }

    @Override
    public void validate(eu.domibus.api.usermessage.domain.UserMessage userMessage) {
        if (!isUserMessageValidatorActive()) {
            LOG.debug("Validation skipped: validator SPI is not active");
            return;
        }
        LOG.debug("Validating user message");

        final UserMessageDTO userMessageDto = domibusExtMapper.userMessageToUserMessageDTO(userMessage);
        userMessageValidatorSpi.validateUserMessage(userMessageDto);

        LOG.debug("Finished validating user message");
    }

    @Override
    public void validatePayload(InputStream payload, String mimeType) {
        if (!isUserMessageValidatorActive()) {
            LOG.debug("Validation skipped: validator SPI is not active");
            return;
        }
        LOG.debug("Validating payload");

        userMessageValidatorSpi.validatePayload(payload, mimeType);

        LOG.debug("Finished validating payload");
    }

    protected boolean isUserMessageValidatorActive() {
        return userMessageValidatorSpi != null;
    }
}
