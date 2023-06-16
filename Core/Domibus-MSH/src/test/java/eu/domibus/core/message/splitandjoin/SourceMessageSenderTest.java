package eu.domibus.core.message.splitandjoin;

import eu.domibus.api.message.attempt.MessageAttempt;
import eu.domibus.api.message.attempt.MessageAttemptService;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.ebms3.sender.retry.UpdateRetryLoggingService;
import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.message.PartInfoDao;
import eu.domibus.core.message.reliability.ReliabilityChecker;
import eu.domibus.core.pmode.provider.PModeProvider;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class SourceMessageSenderTest {

    @Tested
    private SourceMessageSender sourceMessageSender;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private MSHDispatcher mshDispatcher;

    @Injectable
    private EbMS3MessageBuilder messageBuilder;

    @Injectable
    private ReliabilityChecker reliabilityChecker;

    @Injectable
    private MessageAttemptService messageAttemptService;

    @Injectable
    private MessageExchangeService messageExchangeService;

    @Injectable
    protected DomainTaskExecutor domainTaskExecutor;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected UpdateRetryLoggingService updateRetryLoggingService;

    @Injectable
    protected SplitAndJoinService splitAndJoinService;

    @Injectable
    protected UserMessage userMessage;

    @Injectable
    protected UserMessageLog userMessageLog;

    @Injectable
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Injectable
    protected PartInfoDao partInfoDao;

    @Test
    public void sendMessage(@Injectable Runnable task,
                            @Injectable Domain currentDomain) {

        new Expectations(sourceMessageSender) {{
            domainContextProvider.getCurrentDomain();
            result = currentDomain;
        }};

        sourceMessageSender.sendMessage(userMessage, userMessageLog);
        new FullVerifications() {{
            domainContextProvider.getCurrentDomain();
            times = 1;
            domainTaskExecutor.submitLongRunningTask(withAny(task), withAny(currentDomain));
            times = 1;
        }};
    }

    @Test
    public void doSendMessageThrowsException(@Injectable MSHRole mshRole,
                                             @Injectable SOAPFault fault,
                                             @Injectable LegConfiguration legConfiguration,
                                             @Injectable Party party,
                                             @Injectable SOAPMessage soapMessage,
                                             @Injectable MessageAttempt attempt) throws EbMS3Exception {
        String pModeKey = "pModeKey";
        String messageId = "test";
        String senderParty = "red_gw";
        new Expectations() {
            {
                userMessage.getMessageId();
                result = messageId;
                pModeProvider.findUserMessageExchangeContext(userMessage, mshRole.SENDING).getPmodeKey();
                result = pModeKey;
                pModeProvider.getLegConfiguration(pModeKey);
                result = legConfiguration;
                times = 1;
                party.getName();
                result = senderParty;
                pModeProvider.getSenderParty(pModeKey);
                result = party;
                pModeProvider.getReceiverParty(pModeKey);
                result = party;
                messageExchangeService.verifyReceiverCertificate(legConfiguration, senderParty);
                times = 1;
                messageExchangeService.verifySenderCertificate(legConfiguration, senderParty);
                times = 1;
            }
        };

        sourceMessageSender.doSendMessage(userMessage);
        new Verifications() {{
            messageAttemptService.create(withAny(attempt));
            times = 1;
        }};
    }
}
