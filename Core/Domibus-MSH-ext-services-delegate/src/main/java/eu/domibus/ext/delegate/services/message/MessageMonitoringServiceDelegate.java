package eu.domibus.ext.delegate.services.message;

import eu.domibus.api.message.UserMessageSecurityService;
import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.usermessage.UserMessageRestoreService;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.ext.delegate.mapper.MessageExtMapper;
import eu.domibus.ext.domain.MessageAttemptDTO;
import eu.domibus.ext.exceptions.AuthenticationExtException;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.MessageMonitorExtException;
import eu.domibus.ext.services.MessageMonitorExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Service
public class MessageMonitoringServiceDelegate implements MessageMonitorExtService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageMonitoringServiceDelegate.class);

    protected UserMessageService userMessageService;

    protected MessageExtMapper messageExtMapper;

    protected MessageAttemptService messageAttemptService;

    protected UserMessageSecurityService userMessageSecurityService;

    private AuthUtils authUtils;
    protected UserMessageRestoreService restoreService;

    public MessageMonitoringServiceDelegate(UserMessageService userMessageService,
                                            MessageExtMapper messageExtMapper,
                                            MessageAttemptService messageAttemptService,
                                            UserMessageSecurityService userMessageSecurityService,
                                            AuthUtils authUtils,
                                            UserMessageRestoreService restoreService) {
        this.userMessageService = userMessageService;
        this.messageExtMapper = messageExtMapper;
        this.messageAttemptService = messageAttemptService;
        this.userMessageSecurityService = userMessageSecurityService;
        this.authUtils = authUtils;
        this.restoreService = restoreService;
    }

    @Override
    public List<String> getFailedMessages() throws AuthenticationExtException, MessageMonitorExtException {
        return getFailedMessages(null);
    }

    @Override
    public List<String> getFailedMessages(String finalRecipient) throws AuthenticationExtException, MessageMonitorExtException {
        LOG.debug("Getting failed messages with finalRecipient [{}]", finalRecipient);
        String originalUser = getOriginalUserOrNullIfAdmin();
        return userMessageService.getFailedMessages(finalRecipient, originalUser);
    }

    @Override
    public Long getFailedMessageInterval(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.SENDING);
        return userMessageService.getFailedMessageElapsedTime(messageId);
    }

    @Override
    public void restoreFailedMessage(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.SENDING);
        restoreService.restoreFailedMessage(messageId);
    }

    @Override
    public void sendEnqueuedMessage(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.SENDING);
        userMessageService.sendEnqueuedMessage(messageId);
    }

    @Override
    public List<String> restoreFailedMessagesDuringPeriod(Long begin, Long end) throws AuthenticationExtException, MessageMonitorExtException {
        String originalUser = getOriginalUserOrNullIfAdmin();
        return restoreService.restoreFailedMessagesDuringPeriod(begin, end, null, originalUser);
    }

    @Override
    public void deleteFailedMessage(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.SENDING);
        userMessageService.deleteFailedMessage(messageId);
    }

    @Override
    public List<MessageAttemptDTO> getAttemptsHistory(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.SENDING);
        final List<MessageAttempt> attemptsHistory = messageAttemptService.getAttemptsHistory(messageId);
        return messageExtMapper.messageAttemptToMessageAttemptDTO(attemptsHistory);
    }

    @Override
    public void deleteMessageInFinalStatus(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId);
        try {
            userMessageService.deleteMessageInFinalStatus(messageId, MSHRole.SENDING);
        } catch (MessageNotFoundException ex) {
            LOG.info("Could not find a message with id [{}] and SENDING role. Trying RECEIVING role.", messageId);
            userMessageService.deleteMessageInFinalStatus(messageId, MSHRole.RECEIVING);
        }
    }

    @Override
    public void deleteMessageInFinalStatus(String messageId, eu.domibus.common.MSHRole role) throws AuthenticationExtException, MessageMonitorExtException {
        if (role == null) {
            LOG.debug("Role param is null so calling the method without role");
            deleteMessageInFinalStatus(messageId);
            return;
        }
        eu.domibus.api.model.MSHRole mshRole = eu.domibus.api.model.MSHRole.valueOf(role.name());
        userMessageSecurityService.checkMessageAuthorization(messageId, mshRole);
        userMessageService.deleteMessageInFinalStatus(messageId, mshRole);
    }

    @Override
    public void deleteMessageNotInFinalStatus(String messageId) throws AuthenticationExtException, MessageMonitorExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId);
        try {
            userMessageService.deleteMessageNotInFinalStatus(messageId, MSHRole.SENDING);
        } catch (MessageNotFoundException ex) {
            LOG.info("Could not find a message with id [{}] and SENDING role. Trying RECEIVING role.", messageId);
            userMessageService.deleteMessageNotInFinalStatus(messageId, MSHRole.RECEIVING);
        }
    }

    @Override
    public void deleteMessageNotInFinalStatus(String messageId, eu.domibus.common.MSHRole role) {
        if (role == null) {
            LOG.debug("Role param is null so calling the method without role");
            deleteMessageNotInFinalStatus(messageId);
            return;
        }
        eu.domibus.api.model.MSHRole mshRole = eu.domibus.api.model.MSHRole.valueOf(role.name());
        userMessageSecurityService.checkMessageAuthorization(messageId, mshRole);
        userMessageService.deleteMessageNotInFinalStatus(messageId, mshRole);
    }

    @Override
    public List<String> deleteMessagesDuringPeriod(Long begin, Long end) throws AuthenticationExtException, MessageMonitorExtException {
        String originalUser = getOriginalUserOrNullIfAdmin();
        return userMessageService.deleteMessagesNotInFinalStatusDuringPeriod(begin, end, originalUser);
    }

    @Override
    public List<String> deleteMessagesInFinalStatusDuringPeriod(Long begin, Long end) throws AuthenticationExtException, MessageMonitorExtException {
        String originalUser = getOriginalUserOrNullIfAdmin();
        return userMessageService.deleteMessagesInFinalStatusDuringPeriod(begin, end, originalUser);
    }

    private String getOriginalUserOrNullIfAdmin() {
        return authUtils.getOriginalUserOrNullIfAdmin();
    }
}
