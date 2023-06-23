package eu.domibus.core.plugin.handler;

import eu.domibus.api.message.UserMessageSecurityService;
import eu.domibus.api.message.validation.UserMessageValidatorSpiService;
import eu.domibus.api.payload.PartInfoService;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.generator.id.MessageIdGenerator;
import eu.domibus.core.message.*;
import eu.domibus.core.message.dictionary.MpcDictionaryService;
import eu.domibus.core.message.pull.PullMessageService;
import eu.domibus.core.message.splitandjoin.SplitAndJoinConfigurationService;
import eu.domibus.core.message.splitandjoin.SplitAndJoinHelper;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.transformer.SubmissionAS4Transformer;
import eu.domibus.core.pmode.PModeDefaultService;
import eu.domibus.core.pmode.provider.PModeProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
@ExtendWith(JMockitExtension.class)
public class MessageSubmitterImplTest {
    @Tested
    MessageSubmitterImpl messageSubmitter;

    @Injectable
    protected AuthUtils authUtils;

    @Injectable
    protected UserMessageDefaultService userMessageService;

    @Injectable
    protected SplitAndJoinHelper splitAndJoinHelper;

    @Injectable
    protected PModeDefaultService pModeDefaultService;

    @Injectable
    private SubmissionAS4Transformer transformer;

    @Injectable
    private MessagingService messagingService;

    @Injectable
    private UserMessageLogDefaultService userMessageLogService;

    @Injectable
    private PayloadFileStorageProvider storageProvider;

    @Injectable
    private ErrorLogService errorLogService;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private MessageIdGenerator messageIdGenerator;

    @Injectable
    private BackendMessageValidator backendMessageValidator;

    @Injectable
    private MessageExchangeService messageExchangeService;

    @Injectable
    private PullMessageService pullMessageService;

    @Injectable
    protected MessageFragmentDao messageFragmentDao;

    @Injectable
    protected MpcDictionaryService mpcDictionaryService;

    @Injectable
    protected UserMessageHandlerServiceImpl userMessageHandlerService;

    @Injectable
    protected UserMessageValidatorSpiService userMessageValidatorSpiService;

    @Injectable
    protected UserMessageSecurityService userMessageSecurityService;

    @Injectable
    protected PartInfoService partInfoService;

    @Injectable
    protected SplitAndJoinConfigurationService splitAndJoinConfigurationService;

    @Injectable
    protected MessageSubmitterHelper messageSubmitterHelper;

    @Injectable
    protected TestMessageValidator testMessageValidator;

    @Injectable
    BackendConnectorService backendConnectorService;


    @Test
    public void testcreateNewParty() {
        String mpc = "mpc_qn";
        String initiator = "initiator";
        // Given
        new Expectations() {{
            messageExchangeService.extractInitiator(mpc);
            result = initiator;
        }};
        Party party = messageSubmitter.createNewParty(mpc);
        Assertions.assertNotNull(party);
        Assertions.assertEquals(initiator, party.getName());
    }

    @Test
    public void testcreateNewPartyNull() {
        Party party = messageSubmitter.createNewParty(null);
        Assertions.assertNull(party);
    }

}
