package eu.domibus.core.message.reliability;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.model.splitandjoin.MessageGroupEntity;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.CertificateException;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.crypto.DomainCryptoServiceFactory;
import eu.domibus.core.crypto.SecurityProfileService;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.sender.ResponseHandler;
import eu.domibus.core.ebms3.sender.ResponseResult;
import eu.domibus.core.ebms3.sender.retry.UpdateRetryLoggingService;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.message.nonrepudiation.NonRepudiationService;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.core.message.splitandjoin.MessageGroupDao;
import eu.domibus.core.message.splitandjoin.SplitAndJoinService;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.soap.SOAPMessage;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SMART_RETRY_ENABLED;

/**
 * @author Thomas Dussart
 * @author Cosmin Baciu
 * @since 3.3
 */

@Service
public class ReliabilityServiceImpl implements ReliabilityService {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(ReliabilityServiceImpl.class);
    public static final String SUCCESS = "SUCCESS";

    @Autowired
    private UserMessageLogDefaultService userMessageLogService;

    @Autowired
    private BackendNotificationService backendNotificationService;

    @Autowired
    private MessageGroupDao messageGroupDao;

    @Autowired
    private UpdateRetryLoggingService updateRetryLoggingService;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private PartyStatusDao partyStatusDao;

    @Autowired
    protected UserMessageService userMessageService;

    @Autowired
    protected SplitAndJoinService splitAndJoinService;

    @Autowired
    protected ResponseHandler responseHandler;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    MessageRetentionDefaultService messageRetentionService;

    @Autowired
    protected NonRepudiationService nonRepudiationService;

    @Autowired
    protected SecurityProfileService securityProfileService;

    @Autowired
    protected DomainCryptoServiceFactory domainCryptoServiceFactory;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    protected PModeProvider pModeProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkIfAcknowledgmentSigningCertificateIsInTheTrustStore(final LegConfiguration legConfiguration, UserMessage userMessage) {
        String acknowledgementSenderName;
        try {
            acknowledgementSenderName = pModeProvider.findReceiverParty(userMessage);
        } catch (EbMS3Exception e) {
            String exceptionMessage = String.format("Error while retrieving senderParty from UserMessage: %s", e.getMessage());
            throw new ConfigurationException(exceptionMessage);
        }

        String aliasForSigning = securityProfileService.getAliasForSigning(legConfiguration, acknowledgementSenderName);

        try {
            X509Certificate cert = multiDomainCertificateProvider.getCertificateFromTruststore(domainContextProvider.getCurrentDomain(), aliasForSigning);
            if (cert == null) {
                String exceptionMessage = String.format("Signing certificate for sender [%s] could not be found in the TrustStore", acknowledgementSenderName);
                throw new CertificateException(DomibusCoreErrorCode.DOM_005, exceptionMessage);
            }
        } catch (KeyStoreException e) {
            String exceptionMessage = String.format("Failed to get signing certificate for sender [%s] from truststore: %s", acknowledgementSenderName, e.getMessage());
            throw new CertificateException(DomibusCoreErrorCode.DOM_005, exceptionMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void handleReliability(UserMessage userMessage, UserMessageLog userMessageLog, final ReliabilityChecker.CheckResult reliabilityCheckResult, String requestRawXMLMessage, SOAPMessage responseSoapMessage, final ResponseResult responseResult, final LegConfiguration legConfiguration, final MessageAttempt attempt) {
        LOG.debug("Handling reliability");

        checkIfAcknowledgmentSigningCertificateIsInTheTrustStore(legConfiguration, userMessage);

        switch (reliabilityCheckResult) {
            case OK:
                if(StringUtils.isNotBlank(requestRawXMLMessage)) {
                    nonRepudiationService.saveRawEnvelope(requestRawXMLMessage, userMessage);
                }
                responseHandler.saveResponse(responseSoapMessage, userMessage, responseResult.getResponseMessaging());

                ResponseHandler.ResponseStatus responseStatus = responseResult.getResponseStatus();
                switch (responseStatus) {
                    case OK:
                        userMessageLogService.setMessageAsAcknowledged(userMessage, userMessageLog);

                        if (userMessage.isMessageFragment()) {
                            MessageGroupEntity messageGroupEntity = messageGroupDao.findByUserMessageEntityId(userMessage.getEntityId());
                            splitAndJoinService.incrementSentFragments(messageGroupEntity.getGroupId());
                        }
                        break;
                    case WARNING:
                        userMessageLogService.setMessageAsAckWithWarnings(userMessage, userMessageLog);
                        break;
                    default:
                        assert false;
                }

                backendNotificationService.notifyOfSendSuccess(userMessage, userMessageLog);

                userMessageLog.setSendAttempts(userMessageLog.getSendAttempts() + 1);
                messageRetentionService.deletePayloadOnSendSuccess(userMessage, userMessageLog);
                userMessageLogDao.update(userMessageLog);
                break;
            case WAITING_FOR_CALLBACK:
                updateRetryLoggingService.updateWaitingReceiptMessageRetryLogging(userMessage, legConfiguration);
                break;
            case SEND_FAIL:
                updateRetryLoggingService.updatePushedMessageRetryLogging(userMessage, legConfiguration, attempt);
                break;
            case ABORT:
                updateRetryLoggingService.messageFailedAndDeleteRawEnvelope(userMessage, userMessageLog);

                if (userMessage.isMessageFragment()) {
                    MessageGroupEntity messageGroupEntity = messageGroupDao.findByUserMessageEntityId(userMessage.getEntityId());
                    userMessageService.scheduleSplitAndJoinSendFailed(messageGroupEntity.getGroupId(), String.format("Message fragment [%s] has failed to be sent", userMessage.getMessageId()));
                }
                break;
        }

        LOG.debug("Finished handling reliability");
    }

    @Override
    public void updatePartyState(String status, String partyName) {
        if (!partyStatusDao.existsWithName(partyName)) {
            PartyStatusEntity newPs = new PartyStatusEntity();
            newPs.setConnectivityStatus(status);
            newPs.setPartyName(partyName);
            partyStatusDao.create(newPs);
            LOG.debug("Connectivity status entry created for party [{}] with value: [{}]", partyName, status);
        } else {
            PartyStatusEntity existingPs = partyStatusDao.findByName(partyName);
            if (!existingPs.getConnectivityStatus().equals(status)) {
                existingPs.setConnectivityStatus(status);
                partyStatusDao.update(existingPs);
                LOG.debug("Connectivity status for party [{}] is now: [{}]", partyName, status);
            }
        }
    }

    @Override
    public boolean isPartyReachable(String partyName) {
        PartyStatusEntity existingPs = partyStatusDao.findByName(partyName);
        if (existingPs!=null) {
            return SUCCESS.equals(existingPs.getConnectivityStatus());
        }
        return true; //if no entry exists for the party in the status table, let the send attempt to execute for the first time
    }

    @Override
    public boolean isSmartRetryEnabledForParty(String partyName) {
        String smartRetryPropVal = domibusPropertyProvider.getProperty(DOMIBUS_SMART_RETRY_ENABLED);
        if (StringUtils.isBlank(smartRetryPropVal)) {
            return false;
        }
        List<String> smartRetryEnabledParties = Arrays.asList(smartRetryPropVal.split(","));
        smartRetryEnabledParties = smartRetryEnabledParties.stream()
                .map(enabledPartyId -> StringUtils.trim(enabledPartyId))
                .collect(Collectors.toList());
        return smartRetryEnabledParties.contains(partyName);
    }


}
