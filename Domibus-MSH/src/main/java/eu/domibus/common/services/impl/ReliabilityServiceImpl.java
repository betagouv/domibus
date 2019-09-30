package eu.domibus.common.services.impl;

import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.dao.MessagingDao;
import eu.domibus.common.dao.UserMessageLogDao;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.logging.UserMessageLog;
import eu.domibus.common.services.ReliabilityService;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.message.fragment.SplitAndJoinService;
import eu.domibus.ebms3.common.model.Messaging;
import eu.domibus.ebms3.common.model.UserMessage;
import eu.domibus.ebms3.receiver.BackendNotificationService;
import eu.domibus.ebms3.sender.ReliabilityChecker;
import eu.domibus.ebms3.sender.ResponseHandler;
import eu.domibus.ebms3.sender.ResponseResult;
import eu.domibus.ebms3.sender.UpdateRetryLoggingService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.soap.SOAPMessage;

/**
 * @author Thomas Dussart
 * @author Cosmin Baciu
 * @since 3.3
 */

@Service
public class ReliabilityServiceImpl implements ReliabilityService {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(ReliabilityServiceImpl.class);

    @Autowired
    private UserMessageLogDefaultService userMessageLogService;

    @Autowired
    private BackendNotificationService backendNotificationService;

    @Autowired
    private MessagingDao messagingDao;

    @Autowired
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Autowired
    private UserMessageHandlerService userMessageHandlerService;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    protected UserMessageService userMessageService;

    @Autowired
    protected SplitAndJoinService splitAndJoinService;

    @Autowired
    protected ResponseHandler responseHandler;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReliabilityInNewTransaction(String messageId, Messaging messaging, UserMessageLog userMessageLog, final ReliabilityChecker.CheckResult reliabilityCheckSuccessful, SOAPMessage responseSoapMessage, final ResponseResult responseResult, final LegConfiguration legConfiguration) {
        LOG.debug("Handling reliability in a new transaction");
        handleReliability(messageId, messaging, userMessageLog, reliabilityCheckSuccessful, responseSoapMessage, responseResult, legConfiguration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleReliability(String messageId, Messaging messaging, UserMessageLog userMessageLog, final ReliabilityChecker.CheckResult reliabilityCheckSuccessful, SOAPMessage responseSoapMessage, final ResponseResult responseResult, final LegConfiguration legConfiguration) {
        LOG.debug("Handling reliability");

        final Boolean isTestMessage = userMessageLog.isTestMessage();
        final UserMessage userMessage = messaging.getUserMessage();

        switch (reliabilityCheckSuccessful) {
            case OK:
                responseHandler.saveResponse(responseSoapMessage, messaging, responseResult.getResponseMessaging());

                ResponseHandler.ResponseStatus responseStatus = responseResult.getResponseStatus();
                switch (responseStatus) {
                    case OK:
                        userMessageLogService.setMessageAsAcknowledged(userMessage, userMessageLog);

                        if (userMessage.isUserMessageFragment()) {
                            splitAndJoinService.incrementSentFragments(userMessage.getMessageFragment().getGroupId());
                        }
                        break;
                    case WARNING:
                        userMessageLogService.setMessageAsAckWithWarnings(userMessage, userMessageLog);
                        break;
                    default:
                        assert false;
                }
                if (!isTestMessage) {
                    backendNotificationService.notifyOfSendSuccess(userMessageLog);
                }
                userMessageLog.setSendAttempts(userMessageLog.getSendAttempts() + 1);

                messagingDao.clearPayloadData(userMessage);
                LOG.businessInfo(isTestMessage ? DomibusMessageCode.BUS_TEST_MESSAGE_SEND_SUCCESS : DomibusMessageCode.BUS_MESSAGE_SEND_SUCCESS,
                        userMessage.getFromFirstPartyId(), userMessage.getToFirstPartyId());
                break;
            case WAITING_FOR_CALLBACK:
                updateRetryLoggingService.updateWaitingReceiptMessageRetryLogging(messageId, legConfiguration);
                break;
            case SEND_FAIL:
                updateRetryLoggingService.updatePushedMessageRetryLogging(messageId, legConfiguration);
                break;
            case ABORT:
                updateRetryLoggingService.messageFailedInANewTransaction(userMessage, userMessageLog);

                if (userMessage.isUserMessageFragment()) {
                    userMessageService.scheduleSplitAndJoinSendFailed(userMessage.getMessageFragment().getGroupId(), String.format("Message fragment [%s] has failed to be sent", messageId));
                }
                break;
        }

        LOG.debug("Finished handling reliability");
    }
}
