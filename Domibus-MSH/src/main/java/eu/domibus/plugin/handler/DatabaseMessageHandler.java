/*
 * Copyright 2015 e-CODEX Project
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl5
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.domibus.plugin.handler;

/**
 * @author Christian Koch, Stefan Mueller, Federico Martini, Ioana Dragusanu
 * @Since 3.0
 */

import eu.domibus.api.jms.JMSManager;
import eu.domibus.common.*;
import eu.domibus.common.dao.*;
import eu.domibus.common.exception.EbMS3Exception;
import eu.domibus.common.exception.MessagingExceptionFactory;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Mpc;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.logging.ErrorLogEntry;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.model.logging.UserMessageLogBuilder;
import eu.domibus.common.validators.PayloadProfileValidator;
import eu.domibus.common.validators.PropertyProfileValidator;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.ebms3.common.model.*;
import eu.domibus.ebms3.security.util.AuthUtils;
import eu.domibus.messaging.DuplicateMessageException;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.transformer.impl.SubmissionAS4Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Queue;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseMessageHandler implements MessageSubmitter<Submission>, MessageRetriever<Submission> {

    private static final Log LOG = LogFactory.getLog(DatabaseMessageHandler.class);

    private final ObjectFactory ebMS3Of = new ObjectFactory();

    @Autowired
    JMSManager jmsManager;

    @Autowired
    @Qualifier("sendMessageQueue")
    private Queue sendMessageQueue;

    @Autowired
    private CompressionService compressionService;

    @Autowired
    private SubmissionAS4Transformer transformer;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private SignalMessageDao signalMessageDao;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private SignalMessageLogDao signalMessageLogDao;

    @Autowired
    private ErrorLogDao errorLogDao;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private MessageIdGenerator messageIdGenerator;

    @Autowired
    private PayloadProfileValidator payloadProfileValidator;

    @Autowired
    private PropertyProfileValidator propertyProfileValidator;

    @Autowired
    AuthUtils authUtils;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Submission downloadMessage(final String messageId) throws MessageNotFoundException {
        if (!authUtils.isUnsecureLoginAllowed())
            authUtils.hasUserOrAdminRole();

        String originalUser = authUtils.getOriginalUserFromSecurityContext(SecurityContextHolder.getContext());
        LOG.debug("Authorized as " + (originalUser == null ? "super user" : originalUser));

        LOG.info("Searching message with id [" + messageId + "]");
        final UserMessageLog userMessageLog;
        final UserMessage userMessage;
        try {
            userMessage = messagingDao.findUserMessageByMessageId(messageId);
            userMessageLog = userMessageLogDao.findByMessageId(messageId, MSHRole.RECEIVING);
            if (userMessageLog == null) {
                throw new MessageNotFoundException("Message with id [" + messageId + "] was not found");
            }
        } catch (final NoResultException nrEx) {
            LOG.debug("Message with id [" + messageId + "] was not found", nrEx);
            throw new MessageNotFoundException("Message with id [" + messageId + "] was not found");
        }

        validateOriginalUser(userMessage, originalUser, MessageConstants.FINAL_RECIPIENT);

        // Deleting the message and signal message if the retention download is zero
        if (0 == pModeProvider.getRetentionDownloadedByMpcURI(userMessage.getMpc())) {
            messagingDao.clearPayloadData(messageId);
            List<SignalMessage> signalMessages = signalMessageDao.findSignalMessagesByRefMessageId(messageId);
            if (!signalMessages.isEmpty()) {
                for (SignalMessage signalMessage : signalMessages) {
                    signalMessageDao.clear(signalMessage);
                }
            }
            // Sets the log status to deleted.
            userMessageLogDao.setMessageAsDeleted(messageId);
            // Sets the log status to deleted also for the signal messages (if present).
            List<String> signalMessageIds = signalMessageDao.findSignalMessageIdsByRefMessageId(messageId);
            if (!signalMessageIds.isEmpty()) {
                for (String signalMessageId : signalMessageIds) {
                    signalMessageLogDao.setMessageAsDeleted(signalMessageId);
                }
            }
        }
        //userMessageLogDao.setMessageAsDownloaded(messageId);
        return transformer.transformFromMessaging(userMessage);
    }

    private void validateOriginalUser(UserMessage userMessage, String authOriginalUser, String type) {
        if (authOriginalUser != null) {
            LOG.debug("OriginalUser is [" + authOriginalUser + "]");
            /* check the message belongs to the authenticated user */
            String originalUser = getOriginalUser(userMessage, type);
            if (originalUser != null && !originalUser.equals(authOriginalUser)) {
                LOG.debug("User [" + authOriginalUser + "] is trying to submit/access a message having as final recipient: " + originalUser);
                throw new AccessDeniedException("You are not allowed to handle this message. You are authorized as [" + authOriginalUser + "]");
            }
        }
    }

    private String getOriginalUser(UserMessage userMessage, String type) {
        if (userMessage == null || userMessage.getMessageProperties() == null || userMessage.getMessageProperties().getProperty() == null) {
            return null;
        }
        String originalUser = null;
        for (Property property : userMessage.getMessageProperties().getProperty()) {
            if (property.getName() != null && property.getName().equals(type)) {
                originalUser = property.getValue();
                break;
            }
        }
        return originalUser;
    }

    @Override
    public MessageStatus getMessageStatus(final String messageId) {
        if (!authUtils.isUnsecureLoginAllowed())
            authUtils.hasAdminRole();

        return userMessageLogDao.getMessageStatus(messageId);
    }

    @Override
    public List<? extends ErrorResult> getErrorsForMessage(final String messageId) {
        if (!authUtils.isUnsecureLoginAllowed())
            authUtils.hasAdminRole();

        return errorLogDao.getErrorsForMessage(messageId);
    }


    @Override
    @Transactional
    public String submit(final Submission messageData, final String backendName) throws MessagingProcessingException {

        if (!authUtils.isUnsecureLoginAllowed()) {
            authUtils.hasUserOrAdminRole();
        }

        String originalUser = authUtils.getOriginalUserFromSecurityContext(SecurityContextHolder.getContext());
        LOG.debug("Authorized as " + (originalUser == null ? "super user" : originalUser));

        UserMessage userMessage = transformer.transformFromSubmission(messageData);

        validateOriginalUser(userMessage, originalUser, MessageConstants.ORIGINAL_SENDER);

        try {
            // MessageInfo is always initialized in the get method
            MessageInfo messageInfo = userMessage.getMessageInfo();
            String messageId = messageInfo.getMessageId();
            if (messageId == null || messageId.trim().isEmpty()) {
                messageId = messageIdGenerator.generateMessageId();
                messageInfo.setMessageId(messageId);
            } else if (messageId.length() > 255) {
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0008, "MessageId value is too long (over 255 characters)", null, null);
            }
            String refToMessageId = messageInfo.getRefToMessageId();
            if (refToMessageId != null && refToMessageId.length() > 255) {
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0008, "RefToMessageId value is too long (over 255 characters)", refToMessageId, null);
            }
            // handle if the messageId is unique. This should only fail if the ID is set from the outside
            if (!MessageStatus.NOT_FOUND.equals(userMessageLogDao.getMessageStatus(messageId))) {
                throw new DuplicateMessageException("Message with id [" + messageId + "] already exists. Message identifiers must be unique");
            }

            Messaging message = ebMS3Of.createMessaging();
            message.setUserMessage(userMessage);

            String pModeKey = pModeProvider.findPModeKeyForUserMessage(userMessage);
            Party from = pModeProvider.getSenderParty(pModeKey);
            Party to = pModeProvider.getReceiverParty(pModeKey);
            // Verifies that the initiator and responder party are not the same.
            if (from.getName().equals(to.getName())) {
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "The initiator party's name is the same as the responder party's one[" + from.getName() + "]", null, null);
            }
            // Verifies that the message is not for the current gateway.
            Configuration config = pModeProvider.getConfigurationDAO().read();
            if (config.getParty().equals(to)) {
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "It is forbidden to submit a message to the sending access point[" + to.getName() + "]", null, null);
            }
            // Verifies that the message is being sent by the same party as the one configured for the sending access point
            if (!config.getParty().equals(from)) {
                throw new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "The initiator party's name [" + from.getName() + "] does not correspond to the access point's name [" + config.getParty().getName() + "]", null, null);
            }

            LegConfiguration legConfiguration = pModeProvider.getLegConfiguration(pModeKey);

            fillMpc(userMessage, legConfiguration, to);

            payloadProfileValidator.validate(message, pModeKey);
            propertyProfileValidator.validate(message, pModeKey);

            boolean compressed = compressionService.handleCompression(userMessage, legConfiguration);
            LOG.debug("Compression for message with id: " + messageId + " applied: " + compressed);

            // We do not create MessageIds for SignalMessages, as those should never be submitted via the backend
            messagingDao.create(message);
            // TODO Should we store the user message log before it is dispatched to the queue ?
            // Sends message to the proper queue
            jmsManager.sendMessageToQueue(new DispatchMessageCreator(messageId, to.getEndpoint()).createMessage(), sendMessageQueue);
            // Builds the user message log
            UserMessageLogBuilder umlBuilder = UserMessageLogBuilder.create()
                    .setMessageId(userMessage.getMessageInfo().getMessageId())
                    .setMessageStatus(MessageStatus.SEND_ENQUEUED)
                    .setMshRole(MSHRole.SENDING)
                    .setNotificationStatus(legConfiguration.getErrorHandling().isBusinessErrorNotifyProducer() ? NotificationStatus.REQUIRED : NotificationStatus.NOT_REQUIRED)
                    .setMpc(message.getUserMessage().getMpc())
                    .setSendAttemptsMax(legConfiguration.getReceptionAwareness() == null ? 1 : legConfiguration.getReceptionAwareness().getRetryCount())
                    .setBackendName(backendName)
                    .setEndpoint(to.getEndpoint());

            userMessageLogDao.create(umlBuilder.build());

            return userMessage.getMessageInfo().getMessageId();

        } catch (final EbMS3Exception ebms3Ex) {
            if (ebms3Ex.getErrorCode().equals(ErrorCode.EbMS3ErrorCode.EBMS_0303)) {
                errorLogDao.create(new ErrorLogEntry(ebms3Ex));
            }
            LOG.error("Error submitting the message [" + userMessage.getMessageInfo().getMessageId() + "] to [" + backendName + "]", ebms3Ex);
            throw MessagingExceptionFactory.transform(ebms3Ex);
        }
    }

    private void fillMpc(UserMessage userMessage, LegConfiguration legConfiguration, Party to) {
        final Map<Party, Mpc> mpcMap = legConfiguration.getPartyMpcMap();
        String mpc = Mpc.DEFAULT_MPC;
        if (legConfiguration.getDefaultMpc() != null) {
            mpc = legConfiguration.getDefaultMpc().getQualifiedName();
        }
        if (mpcMap != null && mpcMap.containsKey(to)) {
            mpc = mpcMap.get(to).getQualifiedName();
        }
        userMessage.setMpc(mpc);
    }

}
