package eu.domibus.core.message.pull;

import eu.domibus.api.ebms3.model.Ebms3SignalMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Mpc;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.message.*;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pulling.PullRequestDao;
import eu.domibus.core.status.DomibusStatusService;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.messaging.MessageConstants;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.apache.neethi.Policy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jms.JMSException;
import javax.jms.Message;

import static eu.domibus.logging.DomibusLogger.MDC_PROPERTY_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(JMockitExtension.class)
public class PullMessageSenderTest {
    @Tested
    private PullMessageSender pullMessageSender;
    
    @Injectable
    protected MessageUtil messageUtil;

    @Injectable
    private MSHDispatcher mshDispatcher;

    @Injectable
    private EbMS3MessageBuilder messageBuilder;

    @Injectable
    private UserMessageHandlerService userMessageHandlerService;

    @Injectable
    protected TestMessageValidator testMessageValidator;

    @Injectable
    private UserMessageErrorCreator userMessageErrorCreator;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private PolicyService policyService;

    @Injectable
    private DomibusStatusService domibusStatusService;

    @Injectable
    private UserMessageDefaultService userMessageDefaultService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    protected Ebms3Converter ebms3Converter;

    @Injectable
    private PullFrequencyHelper pullFrequencyHelper;

    @Injectable
    private PullRequestDao pullRequestDao;

    @Injectable
    protected UserMessagePayloadService userMessagePayloadService;

    @Test
    public void processPullRequestClearCustomKeys(@Injectable Message map) {
        String domibusKey = MDC_PROPERTY_PREFIX + "some_key";
        String otherKey = "some_key";
        MDC.put(domibusKey, "val1");
        MDC.put(otherKey, "val2");

        new Expectations() {{
            domibusStatusService.isNotReady();
            result = false;
        }};

        pullMessageSender.processPullRequest(map);

        assertNull(MDC.get(domibusKey));
        assertNotNull(MDC.get(otherKey));
    }

    @Test
    public void test(@Injectable Message map) throws JMSException, EbMS3Exception {
        String domainCode = "domainCode";
        String mpcQualifiedName = "mpcQualifiedName";
        String pullRequestId = "pullRequestId";
        new Expectations() {{
            map.getStringProperty(MessageConstants.DOMAIN);
            result = domainCode;
            domainContextProvider.setCurrentDomainWithValidation(domainCode);

            map.getStringProperty(PullContext.MPC);
            result = mpcQualifiedName;
            map.getStringProperty(PullContext.PMODE_KEY);
            String pModeKey = "pModeKey";
            result = pModeKey;
            map.getStringProperty(PullContext.PULL_REQUEST_ID);
            result = pullRequestId;
            map.getStringProperty(PullContext.NOTIFY_BUSINNES_ON_ERROR);
            String notifyBusinessOnError = "false";
            result = notifyBusinessOnError;

            pModeProvider.getLegConfiguration(pModeKey);
            LegConfiguration legConfiguration = new LegConfiguration();
            legConfiguration.setDefaultMpc(new Mpc());
            legConfiguration.getDefaultMpc().setName(mpcQualifiedName);
            result = legConfiguration;

            Party receiverParty = new Party();
            pModeProvider.getReceiverParty(pModeKey);
            result = receiverParty;

            policyService.getPolicy(legConfiguration);
            Policy policy = new Policy();
            result = policy;
        }};

        pullMessageSender.processPullRequest(map);

        new Verifications(){{
            Ebms3SignalMessage signalMessage;
            messageBuilder.buildSOAPMessage(signalMessage = withCapture(), null);
            assertEquals(mpcQualifiedName, signalMessage.getPullRequest().getMpc());
            pullFrequencyHelper.success(mpcQualifiedName);
        }};
    }

}
