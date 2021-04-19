package eu.domibus.core.message.pull;

import eu.domibus.api.ebms3.model.Ebms3Error;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3SignalMessage;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.message.UserMessageException;
import eu.domibus.api.model.*;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.message.MessageExchangeConfiguration;
import eu.domibus.core.message.UserMessageDefaultService;
import eu.domibus.core.message.UserMessageHandlerService;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.status.DomibusStatusService;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.messaging.MessageConstants;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Dussart
 * @since 3.3
 * <p>
 * Jms listener in charge of sending pullrequest.
 */
@Component
public class PullMessageSender {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PullMessageSender.class);

    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    private MSHDispatcher mshDispatcher;

    @Autowired
    private EbMS3MessageBuilder messageBuilder;

    @Qualifier("jaxbContextEBMS")
    @Autowired
    private JAXBContext jaxbContext;

    @Autowired
    private UserMessageHandlerService userMessageHandlerService;

    @Autowired
    private BackendNotificationService backendNotificationService;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private DomibusStatusService domibusStatusService;

    @Autowired
    private UserMessageDefaultService userMessageDefaultService;

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Autowired
    protected Ebms3Converter ebms3Converter;

    @Autowired
    private PullFrequencyHelper pullFrequencyHelper;

    @SuppressWarnings("squid:S2583") //TODO: SONAR version updated!
    @Transactional(propagation = Propagation.REQUIRED)
    //@TODO unit test this method.
    @Timer(clazz = PullMessageSender.class, value = "outgoing_pull_request")
    @Counter(clazz = PullMessageSender.class, value = "outgoing_pull_request")
    public void processPullRequest(final Message map) {
        if (domibusStatusService.isNotReady()) {
            return;
        }
        LOG.clearCustomKeys();

        String domainCode;
        try {
            domainCode = map.getStringProperty(MessageConstants.DOMAIN);
            domainContextProvider.setCurrentDomain(domainCode);
        } catch (JMSException e) {
            LOG.error("Could not get domain from pull request jms message:", e);
            return;
        }
        LOG.debug("Initiate pull request");
        boolean notifyBusinessOnError = false;
        String messageId = null;
        String mpcName = null;
        UserMessage userMessage = null;
        List<PartInfo> partInfos = null;

        try {
            final String mpcQualifiedName = map.getStringProperty(PullContext.MPC);
            final String pModeKey = map.getStringProperty(PullContext.PMODE_KEY);
            notifyBusinessOnError = Boolean.valueOf(map.getStringProperty(PullContext.NOTIFY_BUSINNES_ON_ERROR));
            SignalMessage signalMessage = new SignalMessage();
            PullRequest pullRequest = new PullRequest();
            pullRequest.setMpc(mpcQualifiedName);
//            signalMessage.setPullRequest(pullRequest);
            LOG.debug("Sending pull request with mpc:[{}]", mpcQualifiedName);
            final LegConfiguration legConfiguration = pModeProvider.getLegConfiguration(pModeKey);
            mpcName = legConfiguration.getDefaultMpc().getName();
            Party receiverParty = pModeProvider.getReceiverParty(pModeKey);
            final Policy policy = getPolicy(legConfiguration);
            LOG.trace("Build soap message");
            SOAPMessage soapMessage = messageBuilder.buildSOAPMessage(signalMessage, null);
            LOG.trace("Send soap message");
            final SOAPMessage response = mshDispatcher.dispatch(soapMessage, receiverParty.getEndpoint(), policy, legConfiguration, pModeKey);
            pullFrequencyHelper.success(legConfiguration.getDefaultMpc().getName());
            Ebms3Messaging ebms3Messaging = messageUtil.getMessage(response);

            if (ebms3Messaging.getUserMessage() == null && ebms3Messaging.getSignalMessage() != null) {
                LOG.trace("No message for sent pull request with mpc:[{}]", mpcQualifiedName);
                logError(ebms3Messaging.getSignalMessage());
                return;
            }

            userMessage = ebms3Converter.convertFromEbms3(ebms3Messaging.getUserMessage());
            messageId = userMessage.getMessageId();

            partInfos = userMessageHandlerService.handlePayloads(response, ebms3Messaging);
            handleResponse(response, userMessage, partInfos);

            String sendMessageId = messageId;
            if (userMessageHandlerService.checkSelfSending(pModeKey)) {
                sendMessageId += UserMessageHandlerService.SELF_SENDING_SUFFIX;
            }
            try {
                userMessageDefaultService.scheduleSendingPullReceipt(sendMessageId, pModeKey);
            } catch (Exception ex) {
                LOG.warn("Message[{}] exception while sending receipt asynchronously.", messageId, ex);
            }
        } catch (TransformerException | SOAPException | IOException | JAXBException | JMSException e) {
            LOG.error(e.getMessage(), e);
            throw new UserMessageException(DomibusCoreErrorCode.DOM_001, "Error handling new UserMessage", e);
        } catch (final EbMS3Exception e) {
            try {
                if (notifyBusinessOnError && userMessage != null) {
                    backendNotificationService.notifyMessageReceivedFailure(userMessage, partInfos, userMessageHandlerService.createErrorResult(e));
                }
            } catch (Exception ex) {
                LOG.businessError(DomibusMessageCode.BUS_BACKEND_NOTIFICATION_FAILED, ex, messageId);
            }
            checkConnectionProblem(e, mpcName);
        }
    }

    protected void handleResponse(final SOAPMessage response, UserMessage userMessage, List<PartInfo> partInfos) throws TransformerException, SOAPException, IOException, JAXBException, EbMS3Exception {
        LOG.trace("handle message");
        Boolean testMessage = userMessageHandlerService.checkTestMessage(userMessage);

        // Find legConfiguration for the received UserMessage
        MessageExchangeConfiguration userMessageExchangeConfiguration = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.RECEIVING);
        String pModeKey = userMessageExchangeConfiguration.getPmodeKey();
        LOG.debug("pModeKey for received userMessage is [{}]", pModeKey);

        LegConfiguration legConfiguration = pModeProvider.getLegConfiguration(pModeKey);
        LOG.debug("legConfiguration for received userMessage is [{}]", legConfiguration.getName());
        userMessageHandlerService.handleNewUserMessage(legConfiguration, pModeKey, response, userMessage, partInfos, testMessage);

        LOG.businessInfo(testMessage ? DomibusMessageCode.BUS_TEST_MESSAGE_RECEIVED : DomibusMessageCode.BUS_MESSAGE_RECEIVED,
                userMessage.getPartyInfo().getFromParty(), userMessage.getPartyInfo().getToParty());

    }

    private Policy getPolicy(LegConfiguration legConfiguration) throws EbMS3Exception {
        try {
            return policyService.getPolicy(legConfiguration);
        } catch (final ConfigurationException e) {
            EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0010, "Policy configuration invalid", null, e);
            ex.setMshRole(MSHRole.SENDING);
            throw ex;
        }
    }


    private void logError(Ebms3SignalMessage signalMessage) {
        Set<Ebms3Error> error = signalMessage.getError();
        for (Ebms3Error error1 : error) {
            LOG.info(error1.getErrorCode() + " " + error1.getShortDescription());
        }
    }

    private void checkConnectionProblem(EbMS3Exception e, String mpcName) {
        if (e.getErrorCode() == ErrorCode.EbMS3ErrorCode.EBMS_0005) {
            LOG.warn("ConnectionFailure ", e);
            pullFrequencyHelper.increaseError(mpcName);
        } else {
            throw new WebServiceException(e);
        }
    }
}
