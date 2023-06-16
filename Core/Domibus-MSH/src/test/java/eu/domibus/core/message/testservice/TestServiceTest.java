package eu.domibus.core.message.testservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.api.model.SignalMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.party.PartyService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.model.configuration.Agreement;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.dictionary.ActionDictionaryService;
import eu.domibus.core.message.signal.SignalMessageDao;
import eu.domibus.core.message.signal.SignalMessageLogDao;
import eu.domibus.core.monitoring.ConnectionMonitoringHelper;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.handler.MessageSubmitter;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import javax.activation.DataSource;
import java.io.IOException;

/**
 * @author Sebastian-Ion TINCU
 */
@SuppressWarnings({"ConstantConditions", "SameParameterValue", "ResultOfMethodCallIgnored", "unused"})
@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class TestServiceTest {

    private static final String MESSAGE_PROPERTY_KEY_FINAL_RECIPIENT = (String) ReflectionTestUtils.getField(TestService.class, "MESSAGE_PROPERTY_KEY_FINAL_RECIPIENT");

    private static final String BACKEND_NAME = (String) ReflectionTestUtils.getField(TestService.class, "BACKEND_NAME");

    @Tested
    private TestService testService;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private SignalMessageLogDao signalMessageLogDao;

    @Injectable
    private UserMessageLog userMessageLog;

    @Injectable
    private ErrorLogService errorLogService;

    @Injectable
    private MessageSubmitter messageSubmitter;

    @Injectable
    private SignalMessageDao signalMessageDao;

    @Injectable
    UserMessageDao userMessageDao;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    PartyService partyService;

    @Injectable
    ConnectionMonitoringHelper connectionMonitoringHelper;

    

    @Mocked
    private ObjectMapper gson;

    @Mocked
    SignalMessage signalMessage;

    private String sender;

    private String receiver;

    // TODO Is the receiverType the same as the receiverPartyId?
    private String receiverType;

    private final Submission submission = new Submission();

    private Submission returnedSubmission;

    private String senderPartyId;

    private String receiverPartyId;

    private String serviceType;

    private String initiatorRole;

    private String responderRole;

    private Agreement agreement;

    private String messageId, returnedMessageId;

    private final String partyId = "test";

    private final String userMessageId = "testmessageid";

    @BeforeEach
    public void setUp() throws IOException {
        new Expectations() {{
            new ObjectMapper();
            result = gson;

            gson.readValue(anyString, Submission.class);
            result = submission;
        }};
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectPayload() throws IOException {
        givenSenderAndInitiatorCorrectlySet();

        whenCreatingTheSubmissionMessageData();

        thenThePayloadIsCorrectlyDefined();
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectInitiatorParty() throws IOException {
        givenSenderAndInitiatorCorrectlySet();
        givenSenderPartyId("partyId");

        whenCreatingTheSubmissionMessageData();

        thenTheInitiatorPartyIsCorrectlyDefined();
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectServiceType() throws IOException {
        givenSenderAndInitiatorCorrectlySet();
        givenServiceType("serviceType");

        whenCreatingTheSubmissionMessageData();

        thenTheServiceTypeIsCorrectlyDefined();
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectInitiatorRole() throws IOException {
        givenSenderCorrectlySet();
        givenInitiatorRole("initiator");

        whenCreatingTheSubmissionMessageData();

        thenTheInitiatorRoleIsCorrectlyDefined();
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectResponderRole() throws IOException {
        givenSenderAndInitiatorCorrectlySet();
        givenResponderRole("responder");

        whenCreatingTheSubmissionMessageData();

        thenTheResponderRoleIsCorrectlyDefined();
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectAgreementReference() throws IOException {
        givenSenderAndInitiatorCorrectlySet();
        Agreement agreement = new Agreement();
        agreement.setValue("agreement");
        givenAgreementReference(agreement);

        whenCreatingTheSubmissionMessageData();

        thenTheAgreementReferenceIsCorrectlyDefined();
    }

    @Test
    public void createsTheMessageDataToSubmitHavingTheCorrectConversationIdentifier() throws IOException {
        givenSenderAndInitiatorCorrectlySet();

        whenCreatingTheSubmissionMessageData();

        thenTheConversationIdentifierIsCorrectlyDefined();
    }


    @Test
    public void populatesTheReceiverInsideTheReceivingPartiesWhenSubmittingTheTestMessageNormallyWithoutDynamicDiscovery() throws Exception {
        givenSenderAndInitiatorCorrectlySet();
        givenReceiver("receiver");
        givenReceiverPartyId("receiverPartyId");

        new Expectations() {{
            connectionMonitoringHelper.validateSender("sender");
            connectionMonitoringHelper.validateReceiver(anyString);
        }};

        whenSubmittingTheTestMessageNormallyWithoutDynamicDiscovery();

        thenTheReceiverPartyIsCorrectlyDefinedInsideTheReceivingPartiesCollection();
    }

    @Test
    public void populatesTheReceiverAsMessagePropertyWhenSubmittingTheTestMessageWithDynamicDiscovery() throws Exception {
        givenSenderAndInitiatorCorrectlySet();
        givenReceiver("receiver");
        givenReceiverType("receiverType");
        givenFinalRecipientMessagePropertyContainsInitialValue("urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4");

        new Expectations() {{
            connectionMonitoringHelper.validateSender("sender");
        }};

        whenSubmittingTheTestMessageWithDynamicDiscovery();

        thenTheReceiverPartyIsCorrectlyDefinedInsideTheMessagePropertiesReplacingTheInitialValue();
    }

    @Test
    public void returnsTheMessageIdentifierWhenSubmittingTheTestMessageNormallyWithoutDynamicDiscovery() throws Exception {
        givenSenderAndInitiatorCorrectlySet();
        givenReceiver("receiver");
        givenReceiverPartyId("receiverPartyId");
        givenTheMessageIdentifier("messageId");

        new Expectations() {{
            connectionMonitoringHelper.validateSender("sender");
            connectionMonitoringHelper.validateReceiver(anyString);
        }};

        whenSubmittingTheTestMessageNormallyWithoutDynamicDiscovery();

        thenTheMessageIdentifierIsCorrectlyReturned();
    }

    @Test
    public void returnsTheMessageIdentifierWhenSubmittingTheTestMessageWithDynamicDiscovery() throws Exception {
        givenSenderAndInitiatorCorrectlySet();
        givenReceiver("receiver");
        givenReceiverType("receiverType");
        givenTheMessageIdentifier("messageId");

        new Expectations() {{
            connectionMonitoringHelper.validateSender("sender");
        }};

        whenSubmittingTheTestMessageWithDynamicDiscovery();

        thenTheMessageIdentifierIsCorrectlyReturned();
    }

    private void givenSender(String sender) {
        this.sender = sender;
    }

    private void givenReceiver(String receiver) {
        this.receiver = receiver;
    }

    private void givenReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    private void givenSenderCorrectlySet() {
        givenSender("sender");
    }

    private void givenSenderAndInitiatorCorrectlySet() {
        givenSenderCorrectlySet();
        givenInitiatorRole("initiator");
    }

    private void givenSenderPartyId(String partyId) {
        this.senderPartyId = partyId;
        new Expectations() {{
            pModeProvider.getPartyIdType(sender);
            result = partyId;
        }};
    }

    private void givenReceiverPartyId(String partyId) {
        this.receiverPartyId = partyId;
        new Expectations() {{
            pModeProvider.getPartyIdType(receiver);
            result = partyId;
        }};
    }

    private void givenServiceType(String serviceType) {
        this.serviceType = serviceType;
        new Expectations() {{
            pModeProvider.getServiceType(Ebms3Constants.TEST_SERVICE);
            result = serviceType;
        }};
    }

    private void givenInitiatorRole(String initiatorRole) {
        this.initiatorRole = initiatorRole;
        new Expectations() {{
            pModeProvider.getRole("INITIATOR", Ebms3Constants.TEST_SERVICE);
            result = initiatorRole;
        }};
    }

    private void givenResponderRole(String responderRole) {
        this.responderRole = responderRole;
        new Expectations() {{
            pModeProvider.getRole("RESPONDER", Ebms3Constants.TEST_SERVICE);
            result = responderRole;
        }};
    }

    private void givenAgreementReference(Agreement agreement) {
        this.agreement = agreement;
        new Expectations() {{
            pModeProvider.getAgreementRef(Ebms3Constants.TEST_SERVICE);
            result = agreement;
        }};
    }

    private void givenTheMessageIdentifier(String messageId) throws MessagingProcessingException {
        this.messageId = messageId;
        new Expectations() {{
            messageSubmitter.submit(submission, BACKEND_NAME);
            result = messageId;
        }};
    }

    private void givenFinalRecipientMessagePropertyContainsInitialValue(String finalRecipient) {
        submission.addMessageProperty(MESSAGE_PROPERTY_KEY_FINAL_RECIPIENT, finalRecipient);
    }

    private void whenCreatingTheSubmissionMessageData() throws IOException {
        returnedSubmission = testService.createSubmission(sender);
    }

    private void whenSubmittingTheTestMessageNormallyWithoutDynamicDiscovery() throws Exception {
        returnedMessageId = testService.submitTest(sender, receiver);
    }

    private void whenSubmittingTheTestMessageWithDynamicDiscovery() throws Exception {
        returnedMessageId = testService.submitTestDynamicDiscovery(sender, receiver, receiverType);
    }

    private void thenThePayloadIsCorrectlyDefined() {
        Assertions.assertEquals( 1, returnedSubmission.getPayloads().size(), "There should be only one payload");

        Submission.Payload payload = returnedSubmission.getPayloads().iterator().next();
        Assertions.assertEquals("The content id should have been correctly defined", "cid:message", payload.getContentId());

        Assertions.assertTrue( payload.getPayloadProperties().contains(new Submission.TypedProperty("MimeType", "text/xml")), "The 'MimeType' payload property should have been correctly defined");

        DataSource dataSource = payload.getPayloadDatahandler().getDataSource();
        byte[] source = (byte[]) ReflectionTestUtils.getField(dataSource, "source");
        Assertions.assertArrayEquals( "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello>world</hello>".getBytes(), source, "The payload content should have been correctly defined");
        Assertions.assertEquals( "text/xml", dataSource.getContentType(), "The payload content type should have been correctly defined");
    }

    private void thenTheInitiatorPartyIsCorrectlyDefined() {
        Assertions.assertEquals( 1, returnedSubmission.getFromParties().size(), "There should be only one initiator party");
        Assertions.assertEquals( new Submission.Party(sender, senderPartyId), returnedSubmission.getFromParties().iterator().next(), "The initiator party should have been correctly defined");
    }

    private void thenTheReceiverPartyIsCorrectlyDefinedInsideTheReceivingPartiesCollection() {
        Assertions.assertEquals( 1, submission.getToParties().size(), "There should be only one receiver party");
        Assertions.assertEquals( new Submission.Party(receiver, receiverPartyId), submission.getToParties().iterator().next(), "The receiver party should have been correctly defined");
    }

    private void thenTheReceiverPartyIsCorrectlyDefinedInsideTheMessagePropertiesReplacingTheInitialValue() {
        Assertions.assertEquals( 1, submission.getMessageProperties().size(), "There should be only one message property");
        Assertions.assertEquals(
                new Submission.TypedProperty(MESSAGE_PROPERTY_KEY_FINAL_RECIPIENT, receiver, receiverType), submission.getMessageProperties().iterator().next(), "The receiver party should have been correctly defined inside the message properties");
    }

    private void thenTheMessageIdentifierIsCorrectlyReturned() {
        Assertions.assertEquals("The message identifier should have been correctly returned", messageId, returnedMessageId);
    }

    private void thenTheServiceTypeIsCorrectlyDefined() {
        Assertions.assertEquals("The service type should have been correctly defined", serviceType, returnedSubmission.getServiceType());
    }

    private void thenTheInitiatorRoleIsCorrectlyDefined() {
        Assertions.assertEquals("The initiator role should have been correctly defined", initiatorRole, returnedSubmission.getFromRole());
    }

    private void thenTheResponderRoleIsCorrectlyDefined() {
        Assertions.assertEquals("The responder role should have been correctly defined", responderRole, returnedSubmission.getToRole());
    }

    private void thenTheAgreementReferenceIsCorrectlyDefined() {
        Assertions.assertEquals("The agreement reference should have been correctly defined", agreement.getValue(), returnedSubmission.getAgreementRef());
    }

    private void thenTheConversationIdentifierIsCorrectlyDefined() {
        Assertions.assertEquals("The conversation identifier should have been correctly defined since it's required and the Access Point MUST set its value to \"1\" " +
                "according to section 4.3 of the [ebMS3CORE] specification", "1", returnedSubmission.getConversationId());
    }
}
