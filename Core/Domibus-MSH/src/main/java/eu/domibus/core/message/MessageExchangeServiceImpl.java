package eu.domibus.core.message;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.jms.JMSMessageBuilder;
import eu.domibus.api.model.*;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.reliability.ReliabilityException;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.ChainCertificateInvalidException;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.generator.id.MessageIdGenerator;
import eu.domibus.core.message.nonrepudiation.UserMessageRawEnvelopeDao;
import eu.domibus.core.message.pull.*;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pulling.PullRequest;
import eu.domibus.core.pulling.PullRequestDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.ProcessingType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Queue;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_RECEIVER_CERTIFICATE_VALIDATION_ONSENDING;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONSENDING;
import static eu.domibus.core.message.pull.PullContext.*;
import static eu.domibus.jms.spi.InternalJMSConstants.PULL_MESSAGE_QUEUE;

/**
 * @author Thomas Dussart
 * @since 3.3
 * {@inheritDoc}
 */

@Service
public class MessageExchangeServiceImpl implements MessageExchangeService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageExchangeServiceImpl.class);

    @Autowired
    UserMessageDao userMessageDao;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier(PULL_MESSAGE_QUEUE)
    private Queue pullMessageQueue;

    @Autowired
    protected JMSManager jmsManager;

    @Autowired
    private UserMessageRawEnvelopeDao rawEnvelopeLogDao;

    @Autowired
    private PullProcessValidator pullProcessValidator;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private PolicyService policyService;

    @Autowired
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    protected CertificateService certificateService;

    @Autowired
    protected DomainContextProvider domainProvider;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    private PullMessageService pullMessageService;

    @Autowired
    private MpcService mpcService;

    @Autowired
    private PullFrequencyHelper pullFrequencyHelper;

    @Autowired
    protected MessageStatusDao messageStatusDao;

    @Autowired
    protected PullRequestDao pullRequestDao;

    @Autowired
    protected MessageIdGenerator messageIdGenerator;

    @Autowired
    protected SecurityProfileService securityProfileService;

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageStatusEntity getMessageStatus(final MessageExchangeConfiguration messageExchangeConfiguration, ProcessingType processingType) {
        MessageStatus messageStatus = MessageStatus.SEND_ENQUEUED;
        if (ProcessingType.PULL.equals(processingType)) {
            List<Process> processes = pModeProvider.findPullProcessesByMessageContext(messageExchangeConfiguration);
            pullProcessValidator.validatePullProcess(Lists.newArrayList(processes));
            messageStatus = MessageStatus.READY_TO_PULL;
        }
        return messageStatusDao.findOrCreate(messageStatus);
    }

    @Override
    public MessageStatusEntity getMessageStatus(final MessageExchangeConfiguration messageExchangeConfiguration) {
        MessageStatus messageStatus = MessageStatus.SEND_ENQUEUED;
        List<Process> processes = pModeProvider.findPullProcessesByMessageContext(messageExchangeConfiguration);
        if (!processes.isEmpty()) {
            pullProcessValidator.validatePullProcess(Lists.newArrayList(processes));
            messageStatus = MessageStatus.READY_TO_PULL;
        } else {
            LOG.debug("No pull process found for message configuration");
        }
        return messageStatusDao.findOrCreate(messageStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageStatusEntity getMessageStatusForPush() {
        return messageStatusDao.findOrCreate(MessageStatus.SEND_ENQUEUED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public MessageStatusEntity retrieveMessageRestoreStatus(final String messageId, MSHRole role) {
        final UserMessage userMessage = userMessageDao.findByMessageId(messageId, role);
        try {
            if (forcePullOnMpc(userMessage)) {
                return messageStatusDao.findOrCreate(MessageStatus.READY_TO_PULL);
            }
            MessageExchangeConfiguration userMessageExchangeContext = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING);
            return getMessageStatus(userMessageExchangeContext);
        } catch (EbMS3Exception e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_001, "Could not get the PMode key for message [" + messageId + "]", e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void initiatePullRequest() {
        initiatePullRequest(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void initiatePullRequest(final String mpc) {
        if (!pModeProvider.isConfigurationLoaded()) {
            LOG.debug("A configuration problem occurred while initiating the pull request. Probably no configuration is loaded.");
            return;
        }
        Party initiator = pModeProvider.getGatewayParty();
        List<Process> pullProcesses = pModeProvider.findPullProcessesByInitiator(initiator);
        LOG.trace("Initiating pull requests:");
        if (pullProcesses.isEmpty()) {
            LOG.trace("No pull process configured !");
            return;
        }

        final List<Process> validPullProcesses = getValidProcesses(pullProcesses);
        final Set<String> mpcNames = validPullProcesses.stream()
                .flatMap(pullProcess -> pullProcess.getLegs().stream().map(leg -> leg.getDefaultMpc().getName()))
                .collect(Collectors.toSet());
        pullFrequencyHelper.setMpcNames(mpcNames);

        final Integer maxPullRequestNumber = pullFrequencyHelper.getTotalPullRequestNumberPerJobCycle();
        if (pause(maxPullRequestNumber)) {
            return;
        }
        validPullProcesses.forEach(pullProcess ->
                pullProcess.getLegs().stream().filter(distinctByKey(LegConfiguration::getDefaultMpc)).
                        forEach(legConfiguration ->
                                preparePullRequestForMpc(mpc, initiator, pullProcess, legConfiguration)));
    }

    private List<Process> getValidProcesses(List<Process> pullProcesses) {
        final List<Process> validPullProcesses = new ArrayList<>();
        for (Process pullProcess : pullProcesses) {
            try {
                pullProcessValidator.validatePullProcess(Lists.newArrayList(pullProcess));
                validPullProcesses.add(pullProcess);
            } catch (PModeException e) {
                LOG.warn("Invalid pull process configuration found during pull try", e);
            }
        }
        return validPullProcesses;
    }

    private void preparePullRequestForMpc(String mpc, Party initiator, Process pullProcess, LegConfiguration legConfiguration) {
        for (Party responder : pullProcess.getResponderParties()) {
            String mpcQualifiedName = legConfiguration.getDefaultMpc().getQualifiedName();
            if (mpc != null && !mpc.equals(mpcQualifiedName)) {
                continue;
            }
            //@thom remove the pullcontext from here.
            PullContext pullContext = new PullContext(pullProcess,
                    responder,
                    mpcQualifiedName);
            MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration(pullContext.getAgreement(),
                    responder.getName(),
                    initiator.getName(),
                    legConfiguration.getService().getName(),
                    legConfiguration.getAction().getName(),
                    legConfiguration.getName());
            LOG.debug("messageExchangeConfiguration:[{}]", messageExchangeConfiguration);
            String mpcName = legConfiguration.getDefaultMpc().getName();
            Integer pullRequestNumberForResponder = pullFrequencyHelper.getPullRequestNumberForMpc(mpcName);
            LOG.debug("Sending:[{}] pull request for mpcFQN:[{}] to mpc:[{}]", pullRequestNumberForResponder, mpcQualifiedName, mpcName);
            for (int i = 0; i < pullRequestNumberForResponder; i++) {
                PullRequest pullRequest = new PullRequest();
                String uuid = messageIdGenerator.generatePullRequestId();
                pullRequest.setUuid(uuid);
                pullRequest.setMpc(mpcQualifiedName);
                LOG.trace("Sending pull request with UUID:[{}], MPC:[{}]", pullRequest.getUuid(), pullRequest.getMpc());
                pullRequestDao.savePullRequest(pullRequest);
                jmsManager.sendMapMessageToQueue(JMSMessageBuilder.create()
                        .property(MPC, mpcQualifiedName)
                        .property(PULL_REQUEST_ID, uuid)
                        .property(PMODE_KEY, messageExchangeConfiguration.getReversePmodeKey())
                        .property(PullContext.NOTIFY_BUSINNES_ON_ERROR, String.valueOf(legConfiguration.getErrorHandling().isBusinessErrorNotifyConsumer()))
                        .build(), pullMessageQueue);
            }

        }
    }

    protected <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> mcpMap = new ConcurrentHashMap<>();
        return t -> mcpMap.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private boolean pause(Integer maxPullRequestNumber) {
        LOG.trace("Checking if the system should pause the pulling mechanism.");
        final long ongoingPullRequestNumber = pullRequestDao.countPendingPullRequest();

        final boolean shouldPause = ongoingPullRequestNumber > maxPullRequestNumber;

        if (shouldPause) {
            LOG.debug("[PULL]:Size of the pulling queue:[{}] is higher then the number of pull requests to send:[{}]. Pause adding to the queue so the system can consume the requests.", ongoingPullRequestNumber, maxPullRequestNumber);
        } else {
            LOG.trace("[PULL]:Size of the pulling queue:[{}], the number of pull requests to send:[{}].", ongoingPullRequestNumber, maxPullRequestNumber);
        }
        return shouldPause;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String retrieveReadyToPullUserMessageId(final String mpc, final Party initiator) {
        Set<String> partyIds = getPartyIds(mpc, initiator);
        for (String partyId : partyIds) {
            String pullMessageId = pullMessageService.getPullMessageId(partyId, mpc);
            if (pullMessageId != null) {
                return pullMessageId;
            }
        }
        return null;
    }

    protected Set<String> getPartyIds(String mpc, Party initiator) {
        if (initiator != null && CollectionUtils.isNotEmpty(initiator.getIdentifiers())) {
            Set<String> collect = initiator.getIdentifiers().stream().map(identifier -> identifier.getPartyId()).collect(Collectors.toSet());
            LOG.trace("Retrieving party id(s), initiator list with size:[{}] found", collect.size());
            return collect;
        }
        if (pullProcessValidator.allowDynamicInitiatorInPullProcess()) {
            LOG.debug("Pmode initiator list is empty, extracting partyId from mpc [{}]", mpc);
            return Sets.newHashSet(mpcService.extractInitiator(mpc));
        }
        return Sets.newHashSet();
    }

    /**
     * µ
     * {@inheritDoc}
     */
    @Override
    public PullContext extractProcessOnMpc(final String mpcQualifiedName) {
        try {
            String mpc = mpcQualifiedName;
            final Party gatewayParty = pModeProvider.getGatewayParty();
            List<Process> processes = pModeProvider.findPullProcessByMpc(mpc);
            if (CollectionUtils.isEmpty(processes) && mpcService.forcePullOnMpc(mpc)) {
                LOG.debug("No process corresponds to mpc:[{}]", mpc);
                mpc = mpcService.extractBaseMpc(mpc);
                processes = pModeProvider.findPullProcessByMpc(mpc);
            }
            if (LOG.isDebugEnabled()) {
                for (Process process : processes) {
                    LOG.debug("Process:[{}] correspond to mpc:[{}]", process.getName(), mpc);
                }
            }
            pullProcessValidator.validatePullProcess(processes);
            return new PullContext(processes.get(0), gatewayParty, mpc);
        } catch (IllegalArgumentException e) {
            throw new PModeException(DomibusCoreErrorCode.DOM_003, "No pmode configuration found");
        }
    }


    @Override
    public RawEnvelopeDto findPulledMessageRawXmlByMessageId(final String messageId, MSHRole role) {
        final RawEnvelopeDto rawXmlByMessageId = rawEnvelopeLogDao.findRawXmlByMessageIdAndRole(messageId, role);
        if (rawXmlByMessageId == null) {
            throw new ReliabilityException(DomibusCoreErrorCode.DOM_004, "There should always have a raw message for message " + messageId);
        }
        return rawXmlByMessageId;
    }

    /**
     * This method is a bit weird as we delete and save a xml message for the same message id.
     * Saving the raw xml message in the case of the pull is occurring on the last outgoing interceptor in order
     * to have all the cxf message modification saved (reliability check.) Unfortunately this saving is not done in the
     * same transaction.
     *  @param rawXml    the soap envelope
     * @param messageId the user message
     * @param mshRole
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveRawXml(String rawXml, String messageId, MSHRole mshRole) {
        LOG.debug("Saving rawXML for message [{}]", messageId);

        UserMessageRaw newRawEnvelopeLog = new UserMessageRaw();
        newRawEnvelopeLog.setRawXML(rawXml);
        UserMessage userMessage = userMessageDao.findByMessageId(messageId, mshRole);
        newRawEnvelopeLog.setUserMessage(userMessage);
        rawEnvelopeLogDao.create(newRawEnvelopeLog);
    }

    @Override
    public void verifyReceiverCertificate(final LegConfiguration legConfiguration, String receiverName) {
        SecurityProfile securityProfile = legConfiguration.getSecurity().getProfile();
        Policy policy = policyService.parsePolicy("policies/" + legConfiguration.getSecurity().getPolicy(), securityProfile);
        if (policyService.isNoSecurityPolicy(policy) || policyService.isNoEncryptionPolicy(policy)) {
            LOG.debug("Validation of the receiver certificate is skipped.");
            return;
        }

        String alias = securityProfileService.getCertificateAliasForPurpose(receiverName, securityProfile, CertificatePurpose.ENCRYPT);

        if (domibusPropertyProvider.getBooleanProperty(DOMIBUS_RECEIVER_CERTIFICATE_VALIDATION_ONSENDING)) {
            String chainExceptionMessage = "Cannot send message: receiver certificate is not valid or it has been revoked [" + alias + "]";
            try {
                boolean certificateChainValid = multiDomainCertificateProvider.isCertificateChainValid(domainProvider.getCurrentDomain(), alias);
                if (!certificateChainValid) {
                    throw new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, chainExceptionMessage);
                }
                LOG.info("Receiver certificate exists and is valid [{}]", alias);
            } catch (DomibusCertificateException | CryptoException e) {
                throw new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, chainExceptionMessage, e);
            }
        }
    }

    @Override
    public boolean forcePullOnMpc(String mpc) {
        return mpcService.forcePullOnMpc(mpc);
    }

    @Override
    public boolean forcePullOnMpc(UserMessage userMessage) {
        return mpcService.forcePullOnMpc(userMessage);
    }

    @Override
    public String extractInitiator(String mpc) {
        return mpcService.extractInitiator(mpc);
    }

    @Override
    public String extractBaseMpc(String mpc) {
        return mpcService.extractBaseMpc(mpc);
    }

    @Override
    public void verifySenderCertificate(final LegConfiguration legConfiguration, String senderName) {
        SecurityProfile securityProfile = legConfiguration.getSecurity().getProfile();
        Policy policy = policyService.parsePolicy("policies/" + legConfiguration.getSecurity().getPolicy(), securityProfile);
        if (policyService.isNoSecurityPolicy(policy)) {
            LOG.debug("Validation of the sender certificate is skipped.");
            return;
        }

        String alias = securityProfileService.getCertificateAliasForPurpose(senderName, securityProfile, CertificatePurpose.SIGN);

        if (domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_VALIDATION_ONSENDING)) {
            String chainExceptionMessage = "Cannot send message: sender certificate is not valid or it has been revoked [" + alias + "]";
            try {
                X509Certificate certificate = multiDomainCertificateProvider.getCertificateFromKeystore(domainProvider.getCurrentDomain(), alias);
                if (certificate == null) {
                    throw new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, "Cannot send message: " +
                            "sender[" + senderName + "] certificate with alias [" + alias + "] not found in Keystore");
                }
                if (!certificateService.isCertificateValid(certificate)) {
                    throw new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, chainExceptionMessage);
                }
                LOG.info("Sender certificate exists and is valid [{}]", alias);
            } catch (DomibusCertificateException | KeyStoreException | CryptoException ex) {
                // Is this an error and we stop the sending or we just log a warning that we were not able to validate the cert?
                // my opinion is that since the option is enabled, we should validate no matter what => this is an error
                throw new ChainCertificateInvalidException(DomibusCoreErrorCode.DOM_001, chainExceptionMessage, ex);
            }
        }
    }
}

