package eu.domibus.plugin.fs;

import eu.domibus.ext.services.FileUtilExtService;
import eu.domibus.plugin.ProcessingType;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.fs.ebms3.*;
import eu.domibus.plugin.fs.exception.FSPluginException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class FSMessageTransformerTest {

    @Injectable
    protected FileUtilExtService fileUtilExtService;

    @Injectable
    protected FSMimeTypeHelper fsMimeTypeHelper;

    @Tested
    FSMessageTransformer fsMessageTransformer;

    private static final String INITIATOR_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator";
    private static final String RESPONDER_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder";

    private static final String UNREGISTERED_PARTY_TYPE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
    private static final String ORIGINAL_SENDER = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1";
    private static final String PROPERTY_ORIGINAL_SENDER = "originalSender";
    private static final String FINAL_RECIPIENT = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4";
    private static final String PROPERTY_FINAL_RECIPIENT = "finalRecipient";

    private static final String DOMIBUS_BLUE = "domibus-blue";
    private static final String DOMIBUS_RED = "domibus-red";

    private static final String SERVICE_NOPROCESS = "bdx:noprocess";
    private static final String CONTENT_ID = "cid:message";
    private static final String CONTENT_ID_MYPAYLOAD = "cid:mypayload";
    private static final String ACTION_TC1LEG1 = "TC1Leg1";
    private static final String SERVICE_TYPE_TC1 = "tc1";
    private static final String CONVERSATIONID_CONV1 = "conv1";
    private static final String MIME_TYPE = "MimeType";
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_XML = "text/xml";
    private static final String AGREEMENT_REF_A1 = "A1";
    private static final String AGREEMENT_REF_TYPE_T1 = "T1";
    private static final String MYPROP = "MyProp";
    private static final String MYPROP_TYPE = "propType";
    private static final String MYPROP_VALUE = "SomeValue";
    private static final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";

    @BeforeEach
    public void setUp() throws Exception {
    }

    @AfterEach
public void tearDown() throws Exception {
    }

    @Test
    public void testTransformFromSubmission_NormalFlow() throws Exception {
        String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        String refToMessageId = "123456-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
        String conversationId = "ae413adb-920c-4d9c-a5a7-b5b2596eaf1c@domibus.eu";
        String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";

        // Submission
        Submission submission = new Submission();
        submission.setMessageId(messageId);
        submission.setRefToMessageId(refToMessageId);
        submission.addFromParty(DOMIBUS_BLUE, UNREGISTERED_PARTY_TYPE);
        submission.setFromRole(INITIATOR_ROLE);
        submission.addToParty(DOMIBUS_RED, UNREGISTERED_PARTY_TYPE);
        submission.setToRole(RESPONDER_ROLE);
        submission.setProcessingType(ProcessingType.PUSH);

        submission.setServiceType(SERVICE_TYPE_TC1);
        submission.setService(SERVICE_NOPROCESS);
        submission.setAction(ACTION_TC1LEG1);
        submission.setAgreementRefType(AGREEMENT_REF_TYPE_T1);
        submission.setAgreementRef(AGREEMENT_REF_A1);
        submission.setConversationId(conversationId);

        submission.addMessageProperty(PROPERTY_ORIGINAL_SENDER, ORIGINAL_SENDER);
        submission.addMessageProperty(PROPERTY_FINAL_RECIPIENT, FINAL_RECIPIENT);

        ByteArrayDataSource dataSource = new ByteArrayDataSource(payloadContent.getBytes(), APPLICATION_XML);
        dataSource.setName("content.xml");
        DataHandler payLoadDataHandler = new DataHandler(dataSource);
        Submission.TypedProperty submissionTypedProperty = new Submission.TypedProperty(MIME_TYPE, APPLICATION_XML);
        Collection<Submission.TypedProperty> listTypedProperty = new ArrayList<>();
        listTypedProperty.add(submissionTypedProperty);
        Submission.Payload submissionPayload = new Submission.Payload(CONTENT_ID, payLoadDataHandler, listTypedProperty, false, null, null);
        submission.addPayload(submissionPayload);

        // Transform FSMessage from Submission
        FSMessage fsMessage = fsMessageTransformer.transformFromSubmission(submission, null);

        // Expected results for FSMessage
        UserMessage userMessage = fsMessage.getMetadata();
        From from = userMessage.getPartyInfo().getFrom();
        Assertions.assertEquals(messageId, userMessage.getMessageInfo().getMessageId());
        Assertions.assertEquals(refToMessageId, userMessage.getMessageInfo().getRefToMessageId());
        Assertions.assertEquals(UNREGISTERED_PARTY_TYPE, from.getPartyId().getType());
        Assertions.assertEquals(DOMIBUS_BLUE, from.getPartyId().getValue());
        Assertions.assertEquals(INITIATOR_ROLE, from.getRole());

        To to = userMessage.getPartyInfo().getTo();
        Assertions.assertEquals(UNREGISTERED_PARTY_TYPE, to.getPartyId().getType());
        Assertions.assertEquals(DOMIBUS_RED, to.getPartyId().getValue());
        Assertions.assertEquals(RESPONDER_ROLE, to.getRole());

        CollaborationInfo collaborationInfo = userMessage.getCollaborationInfo();
        Assertions.assertEquals(SERVICE_TYPE_TC1, collaborationInfo.getService().getType());
        Assertions.assertEquals(SERVICE_NOPROCESS, collaborationInfo.getService().getValue());
        Assertions.assertEquals(ACTION_TC1LEG1, collaborationInfo.getAction());
        Assertions.assertEquals(AGREEMENT_REF_TYPE_T1, collaborationInfo.getAgreementRef().getType());
        Assertions.assertEquals(AGREEMENT_REF_A1, collaborationInfo.getAgreementRef().getValue());

        List<Property> propertyList = userMessage.getMessageProperties().getProperty();
        Assertions.assertEquals(2, propertyList.size());
        Property property0 = propertyList.get(0);
        Assertions.assertEquals(PROPERTY_ORIGINAL_SENDER, property0.getName());
        Assertions.assertEquals(ORIGINAL_SENDER, property0.getValue());
        Property property1 = propertyList.get(1);
        Assertions.assertEquals(PROPERTY_FINAL_RECIPIENT, property1.getName());
        Assertions.assertEquals(FINAL_RECIPIENT, property1.getValue());

        FSPayload fSPayload = fsMessage.getPayloads().get(CONTENT_ID);
        Assertions.assertEquals(APPLICATION_XML, fSPayload.getMimeType());
        Assertions.assertEquals(payloadContent, IOUtils.toString(fSPayload.getDataHandler().getInputStream(), StandardCharsets.UTF_8));
    }

    @Test
    public void testTransformToSubmission_NormalFlow() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_metadata.xml");
        // Transform FSMessage to Submission
        FSMessageTransformer transformer = new FSMessageTransformer(new FSMimeTypeHelperImpl(), null);

        Submission submission = transformer.transformToSubmission(fsMessage);

        assertTransformValues(submission);
        assertDefaultValuesForPayloadInfo(submission);
    }

    @Test
    public void testTransformToSubmission_NormalFlow_WithPayloadInfo() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_WithPayloadInfo_metadata.xml");
        // Transform FSMessage to Submission
        FSMessageTransformer transformer = new FSMessageTransformer(new FSMimeTypeHelperImpl(), null);
        Submission submission = transformer.transformToSubmission(fsMessage);

        assertTransformPayloadInfo(submission);
    }

    @Test
    public void testTransformToFromToSubmission_NormalFlow() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_WithPayloadInfo_metadata.xml");
        // Transform FSMessage to Submission
        new Expectations(fsMessageTransformer) {{
           fileUtilExtService.sanitizeFileName(anyString);
           result = "content.xml";
        }};

        // perform the transformation to - from and back to submission
        Submission submission0 = fsMessageTransformer.transformToSubmission(fsMessage);
        FSMessage fsMessage1 = fsMessageTransformer.transformFromSubmission(submission0, null);
        Submission submission = fsMessageTransformer.transformToSubmission(fsMessage1);

        assertTransformValues(submission);
        assertTransformPayloadInfo(submission);
    }

    @Test
    public void testTransformToSubmission_MultiplePartInfo() throws Exception {
        FSMessage fsMessage = buildMessage("testTransformToSubmissionNormalFlow_MultiplePartInfo_metadata.xml");
        FSMessageTransformer transformer = new FSMessageTransformer(new FSMimeTypeHelperImpl(), null);
        // expect exception on multiple PartInfo in PayloadInfo
        Assertions.assertThrows(FSPluginException.class, () -> {
            transformer.transformToSubmission(fsMessage);

        });
    }

    protected void assertDefaultValuesForPayloadInfo(Submission submission) throws IOException {
        Assertions.assertEquals(1, submission.getPayloads().size());
        Submission.Payload submissionPayload = submission.getPayloads().iterator().next();
        Submission.TypedProperty payloadProperty = submissionPayload.getPayloadProperties().iterator().next();
        Assertions.assertEquals(MIME_TYPE, payloadProperty.getKey());
        Assertions.assertEquals(APPLICATION_XML, payloadProperty.getValue());

        DataHandler payloadDatahandler = submissionPayload.getPayloadDatahandler();
        Assertions.assertEquals(APPLICATION_XML, payloadDatahandler.getContentType());
        Assertions.assertEquals(payloadContent, IOUtils.toString(payloadDatahandler.getInputStream(), StandardCharsets.UTF_8));
    }

    protected void assertTransformPayloadInfo(Submission submission) {
        Assertions.assertEquals(1, submission.getPayloads().size());
        Submission.Payload submissionPayload = submission.getPayloads().iterator().next();
        Assertions.assertEquals(CONTENT_ID_MYPAYLOAD, submissionPayload.getContentId());

        Assertions.assertEquals(3, submissionPayload.getPayloadProperties().size());
        for (Submission.TypedProperty payloadProperty : submissionPayload.getPayloadProperties()) {
            if (MIME_TYPE.equals(payloadProperty.getKey())) {
                Assertions.assertEquals(TEXT_XML, payloadProperty.getValue());
            }
            if (MYPROP.equals(payloadProperty.getKey())) {
                Assertions.assertEquals(MYPROP_TYPE, payloadProperty.getType());
                Assertions.assertEquals(MYPROP_VALUE, payloadProperty.getValue());
            }
        }
    }

    protected void assertTransformValues(Submission submission) throws IOException {

        Assertions.assertNotNull(submission);
        Assertions.assertEquals(1, submission.getFromParties().size());
        Submission.Party fromParty = submission.getFromParties().iterator().next();
        Assertions.assertEquals(DOMIBUS_BLUE, fromParty.getPartyId());
        Assertions.assertEquals(UNREGISTERED_PARTY_TYPE, fromParty.getPartyIdType());
        Assertions.assertEquals(INITIATOR_ROLE, submission.getFromRole());

        Assertions.assertEquals(1, submission.getToParties().size());
        Submission.Party toParty = submission.getToParties().iterator().next();
        Assertions.assertEquals(DOMIBUS_RED, toParty.getPartyId());
        Assertions.assertEquals(UNREGISTERED_PARTY_TYPE, toParty.getPartyIdType());
        Assertions.assertEquals(RESPONDER_ROLE, submission.getToRole());

        Assertions.assertNull(submission.getAgreementRefType());
        Assertions.assertNull(submission.getAgreementRef());
        Assertions.assertEquals(SERVICE_NOPROCESS, submission.getService());
        Assertions.assertEquals(SERVICE_TYPE_TC1, submission.getServiceType());
        Assertions.assertEquals(ACTION_TC1LEG1, submission.getAction());
        Assertions.assertEquals(CONVERSATIONID_CONV1, submission.getConversationId());

        Assertions.assertEquals(2, submission.getMessageProperties().size());
        for (Submission.TypedProperty typedProperty : submission.getMessageProperties()) {
            if (PROPERTY_ORIGINAL_SENDER.equalsIgnoreCase(typedProperty.getKey())) {
                Assertions.assertEquals(ORIGINAL_SENDER, typedProperty.getValue());
            }
            if (PROPERTY_FINAL_RECIPIENT.equalsIgnoreCase(typedProperty.getKey())) {
                Assertions.assertEquals(FINAL_RECIPIENT, typedProperty.getValue());
            }
        }

        Assertions.assertEquals(1, submission.getPayloads().size());
        Submission.Payload submissionPayload = submission.getPayloads().iterator().next();

        DataHandler payloadDatahandler = submissionPayload.getPayloadDatahandler();
        Assertions.assertEquals(APPLICATION_XML, payloadDatahandler.getContentType());
        Assertions.assertEquals(payloadContent, IOUtils.toString(payloadDatahandler.getInputStream(), StandardCharsets.UTF_8));

    }

    private FSMessage buildMessage(String filename) throws JAXBException {
        UserMessage metadata = FSTestHelper.getUserMessage(this.getClass(), filename);

        ByteArrayDataSource dataSource = new ByteArrayDataSource(payloadContent.getBytes(), APPLICATION_XML);
        dataSource.setName("content.xml");
        DataHandler dataHandler = new DataHandler(dataSource);
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(null, dataSource.getName(), dataHandler));

        return new FSMessage(fsPayloads, metadata);
    }

    @Test
    public void getPartyInfoFromSubmissionTest(@Injectable Submission submission, @Injectable Submission.Party fromParty) {

        Set<Submission.Party> parties = new HashSet<>();
        parties.add(fromParty);
        new Expectations(fsMessageTransformer) {{
            submission.getFromParties();
            result = parties;
            submission.getFromRole();
            result = INITIATOR_ROLE;
        }};
        try {
            fsMessageTransformer.getPartyInfoFromSubmission(submission);
            Assertions.fail();
        } catch (FSPluginException ex) {
            Assertions.assertEquals("Mandatory field From PartyId is not provided.", ex.getMessage());
        }

        new Verifications() {{
            fsMessageTransformer.validateFromParty(fromParty, INITIATOR_ROLE);
            times = 1;
        }};

    }

    @Test
    public void validateFromParty() {
        try {
            fsMessageTransformer.validateFromParty(null, null);
            Assertions.fail();
        } catch (FSPluginException ex) {
            Assertions.assertEquals("Mandatory field PartyInfo/From is not provided.", ex.getMessage());
        }
    }

    @Test
    public void validateFromPartyEmptyPartyId(@Injectable Submission submission, @Injectable Submission.Party fromParty) {

        new Expectations() {{
            fromParty.getPartyId();
            result = " ";
        }};
        try {
            fsMessageTransformer.validateFromParty(fromParty, null);
            Assertions.fail();
        } catch (FSPluginException ex) {
            Assertions.assertEquals("Mandatory field From PartyId is not provided.", ex.getMessage());
        }
    }

    @Test
    public void validateFromEmptyRole() {

        try {
            fsMessageTransformer.validateFromRole(" ");
            Assertions.fail();
        } catch (FSPluginException ex) {
            Assertions.assertEquals("Mandatory field From Role is not provided.", ex.getMessage());
        }
    }

    @Test
    public void validateFromValidPartyWithRole(@Injectable Submission.Party fromParty) {

        new Expectations(fsMessageTransformer) {{
            fromParty.getPartyId();
            result = "domibus-blue";
        }};
        fsMessageTransformer.validateFromParty(fromParty, INITIATOR_ROLE);

        new Verifications() {{
            fsMessageTransformer.validateFromRole(INITIATOR_ROLE);
            times = 1;
        }};
    }
}
