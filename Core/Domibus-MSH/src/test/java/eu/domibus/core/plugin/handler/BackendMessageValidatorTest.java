package eu.domibus.core.plugin.handler;


import eu.domibus.api.model.*;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Role;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.payload.PayloadProfileValidator;
import eu.domibus.core.pmode.validation.validators.MessagePropertyValidator;
import eu.domibus.core.pmode.validation.validators.PropertyProfileValidator;
import eu.domibus.api.property.DomibusGeneralConstants;
import eu.domibus.core.util.DomibusStringUtilImpl;
import eu.domibus.messaging.DuplicateMessageException;
import eu.domibus.plugin.Submission;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PAYLOAD_LIMIT_28ATTACHMENTS_PER_MESSAGE;
import static eu.domibus.core.plugin.handler.BackendMessageValidator.MESSAGE_WITH_ID_STR;

/**
 * @author Arun Raj
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class BackendMessageValidatorTest {
    private static final String MESSAGE_ID_PATTERN = "^[\\x20-\\x7E]*$";
    private static final String RED = "red_gw";
    private static final String BLUE = "blue_gw";
    private static final String INITIATOR_ROLE_NAME = "defaultInitiatorRole";
    private static final String RESPONDER_ROLE_NAME = "defaultResponderRole";
    private static final String INITIATOR_ROLE_VALUE = "defaultInitiator";
    private static final String RESPONDER_ROLE_VALUE = "defaultResponder";
    private static final String MESS_ID = UUID.randomUUID().toString();


    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private MessagePropertyValidator messagePropertyValidator;

    @Injectable
    private PayloadProfileValidator payloadProfileValidator;

    @Injectable
    private PropertyProfileValidator propertyProfileValidator;

    @Tested
    private BackendMessageValidator backendMessageValidatorObj;

    @Injectable
    private DomibusStringUtilImpl domibusStringUtil;

    @Test
    public void validateMessageId() throws Exception {

        new Expectations() {{
            domibusPropertyProvider.getProperty(BackendMessageValidator.KEY_MESSAGEID_PATTERN);
            result = MESSAGE_ID_PATTERN;
        }};

        /*Happy Flow No error should occur*/
        try {
            String messageId1 = "1234567890-123456789-01234567890/1234567890/`~!@#$%^&*()-_=+\\|,<.>/?;:'\"|\\[{]}.567890.1234567890-1234567890?1234567890#1234567890!1234567890$1234567890%1234567890|12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012";
            backendMessageValidatorObj.validateMessageId(messageId1, MSHRole.SENDING);

            String messageId1_1 = "40b0-9ffc-3f4cfa88bf8b@domibus.eu";
            backendMessageValidatorObj.validateMessageId(messageId1_1, MSHRole.SENDING);

            String messageId1_2 = "APP-RESPONSE-d8d85972-64fb-4161-a1fb-996aa7a9c39c-DOCUMENT-BUNDLE";
            backendMessageValidatorObj.validateMessageId(messageId1_2, MSHRole.SENDING);

            String messageId1_3 = "<1234>";
            backendMessageValidatorObj.validateMessageId(messageId1_3, MSHRole.SENDING);

            String messageId1_4 = "^12^3$4";
            backendMessageValidatorObj.validateMessageId(messageId1_4, MSHRole.SENDING);

        } catch (EbMS3Exception e1) {
            Assertions.fail("Exception was not expected in happy scenarios");
        }
        /*Happy Flow No error should occur*/

        /*Message Id with leading and/or trailing whitespaces should throw error*/
        try {
            String messageId2 = "\t\t346ea37f-7583-40b0-9ffc-3f4cfa88bf8b@domibus.eu\t\t";
            backendMessageValidatorObj.validateMessageId(messageId2, MSHRole.SENDING);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0009", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message Id with leading and/or trailing whitespaces should throw error*/


        /*Message Id containing non printable control characters should result in error*/
        try {
            String messageId4 = "346ea\b37f-7583-40\u0010b0-9ffc-3f4\u007Fcfa88bf8b@d\u0001omibus.eu";
            backendMessageValidatorObj.validateMessageId(messageId4, MSHRole.SENDING);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0009", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message Id containing non printable control characters should result in error*/

        /*Message Id containing only non printable control characters should result in error*/
        try {
            String messageId5 = "\b\u0010\u0030\u007F\u0001";
            backendMessageValidatorObj.validateMessageId(messageId5, MSHRole.SENDING);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0009", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }

        /*Message Id containing non printable control characters should result in error*/

        /*Message id more than 255 characters long should result in error*/
        try {
            String messageId6 = "1234567890-123456789-01234567890/1234567890/1234567890.1234567890.123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890@domibus.eu";

            new Expectations() {{
                domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(messageId6);
                result = true;
            }};

            backendMessageValidatorObj.validateMessageId(messageId6, MSHRole.SENDING);
            Assertions.fail("Expected exception EBMS_0003 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0003", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message id more than 255 characters long should result in error*/
    }


    @Test
    public void validateRefToMessageId() throws Exception {

        new Expectations() {{
            domibusPropertyProvider.getProperty(BackendMessageValidator.KEY_MESSAGEID_PATTERN);
            result = MESSAGE_ID_PATTERN;

        }};

        /*Happy Flow No error should occur*/
        try {
            String refTomessageId1 = "1234567890-123456789-01234567890/1234567890/`~!@#$%^&*()-_=+\\|,<.>/?;:'\"|\\[{]}.567890.1234567890-1234567890?1234567890#1234567890!1234567890$1234567890%1234567890|12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId1);

            String refTomessageId1_1 = "40b0-9ffc-3f4cfa88bf8b@domibus.eu";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId1_1);

            String refTomessageId1_2 = "APP-RESPONSE-d8d85972-64fb-4161-a1fb-996aa7a9c39c-DOCUMENT-BUNDLE";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId1_2);

            String refTomessageId1_3 = "<1234>";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId1_3);

            String refTomessageId1_4 = "^12^3$4";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId1_4);

        } catch (EbMS3Exception e1) {
            Assertions.fail("Exception was not expected in happy scenarios");
        }
        /*Happy Flow No error should occur*/

        /*Message Id with leading and/or trailing whitespaces should throw error*/
        try {
            String refTomessageId2 = "\t\t346ea37f-7583-40b0-9ffc-3f4cfa88bf8b@domibus.eu\t\t";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId2);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0009", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message Id with leading and/or trailing whitespaces should throw error*/


        /*Message Id containing non printable control characters should result in error*/
        try {
            String refTomessageId4 = "346ea\b37f-7583-40\u0010b0-9ffc-3f4\u007Fcfa88bf8b@d\u0001omibus.eu";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId4);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0009", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message Id containing non printable control characters should result in error*/

        /*Message Id containing only non printable control characters should result in error*/
        try {
            String refTomessageId5 = "\b\u0010\u0030\u007F\u0001";

            new Expectations() {{
                domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(refTomessageId5);
                result = true;
            }};
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId5);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0003", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message Id containing non printable control characters should result in error*/

        /*Message id more than 255 characters long should result in error*/
        try {
            String refTomessageId6 = "1234567890-123456789-01234567890/1234567890/1234567890.1234567890.123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890@domibus.eu";

            new Expectations() {{
                domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(refTomessageId6);
                result = true;
            }};

            backendMessageValidatorObj.validateRefToMessageId(refTomessageId6);
            Assertions.fail("Expected exception EBMS_0009 was not raised!");
        } catch (EbMS3Exception e2) {
            Assertions.assertEquals("EBMS:0003", e2.getErrorCode().getCode().getErrorCode().getErrorCodeName());
        }
        /*Message id more than 255 characters long should result in error*/

        /*Ref To Message id can be null*/
        try {
            backendMessageValidatorObj.validateRefToMessageId(null);

        } catch (EbMS3Exception e2) {
            Assertions.fail("RefToMessageId is an optional element and null should be handled!");
        }
        /*Ref To Message id can be null*/

    }

    @Test
    public void testConfigurationNotSpecified() {

        new Expectations() {{
            domibusPropertyProvider.getProperty(BackendMessageValidator.KEY_MESSAGEID_PATTERN);
            result = null;
        }};

        /*If the domibus-configuration file does not have the message id format, then message id pattern validation must be skipped. No exception expected*/
        try {
            String refTomessageId1 = "1234567890-123456789-01234567890/1234567890/`~!@#$%^&*()-_=+\\|,<.>/?;:'\"|\\[{]}.567890.1234567890-1234567890?1234567890#1234567890!1234567890$1234567890%1234567890|12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012";
            backendMessageValidatorObj.validateMessageId(refTomessageId1, MSHRole.SENDING);

            String refTomessageId1_1 = "40b0-9ffc-3f4cfa88bf8b@domibus.eu";
            backendMessageValidatorObj.validateRefToMessageId(refTomessageId1_1);

        } catch (Exception e1) {
            Assertions.fail("When MessageId pattern configuration is not specified, then skip the format validation and no exception is expected!!");
        }
    }

    @Test
    /*
    Verifies that
    the initiator
    and the
    responder parties
    are different.*/

    public void validatePartiesOk() throws Exception {

        final Party from = new Party();
        from.setName(RED);

        final Party to = new Party();
        to.setName(BLUE);

        backendMessageValidatorObj.validateParties(from, to);

        new Verifications() {{
            Assertions.assertNotEquals(from, to);
        }};

    }

    @Test
    /*
    Verifies that
    the message
    is being
    sent by
    the same
    party as
    the one
    configured for
    the sending
    access point */

    public void validateInitiatorPartyOk() throws Exception {

        final Party gatewayParty = new Party();
        gatewayParty.setName(BLUE);

        final Party from = new Party();
        from.setName(BLUE);

        backendMessageValidatorObj.validateInitiatorParty(gatewayParty, from);

        new Verifications() {{
            Assertions.assertEquals(gatewayParty, from);
        }};

    }

    @Test
    /*
    Verifies that
    the message
    is NOT
    being sent
    by the
    same party
    as the
    one configured for
    the sending
    access point */

    public void validateInitiatorPartyNOk() throws Exception {

        final Party gatewayParty = new Party();
        gatewayParty.setName(RED);

        final Party from = new Party();
        from.setName(BLUE);

        try {
            backendMessageValidatorObj.validateInitiatorParty(gatewayParty, from);
            Assertions.fail("It should throw " + EbMS3Exception.class.getCanonicalName());
        } catch (EbMS3Exception ex) {
            assert (ex.getErrorCode().equals(ErrorCode.EbMS3ErrorCode.EBMS_0010));
            assert (ex.getErrorDetail().contains("does not correspond to the access point's name"));
            assert (ex.getMshRole().equals(MSHRole.SENDING));
        }

    }

    @Test
    /*
    Verifies that
    the message
    is not for
    the current
    gateway . */

    public void validateResponderPartyOk() throws Exception {

        final Party gatewayParty = new Party();
        gatewayParty.setName(BLUE);

        final Party to = new Party();
        to.setName(RED);

        backendMessageValidatorObj.validateResponderParty(gatewayParty, to);

        new Verifications() {{
            Assertions.assertNotEquals(gatewayParty, to);
        }};

    }

    @Test
    /*
    Verifies that
    the parties' roles are different. */

    public void validatePartiesRolesOk() throws Exception {

        final Role fromRole = new Role();
        fromRole.setName(INITIATOR_ROLE_NAME);
        fromRole.setValue(INITIATOR_ROLE_VALUE);

        final Role toRole = new Role();
        toRole.setName(RESPONDER_ROLE_NAME);
        toRole.setValue(RESPONDER_ROLE_VALUE);

        backendMessageValidatorObj.validatePartiesRoles(fromRole, toRole);

        new Verifications() {{
            Assertions.assertNotEquals(fromRole, toRole);
        }};

    }

    @Test
    /*
    Verifies that
    the parties' roles are the same. */

    public void validatePartiesRolesNOk() throws Exception {

        final Role fromRole = new Role();
        fromRole.setName(INITIATOR_ROLE_NAME);
        fromRole.setValue(INITIATOR_ROLE_VALUE);


        final Role toRole = new Role();
        toRole.setName(INITIATOR_ROLE_NAME);
        toRole.setValue(INITIATOR_ROLE_VALUE);

        try {
            backendMessageValidatorObj.validatePartiesRoles(fromRole, toRole);
            Assertions.fail("It should throw " + EbMS3Exception.class.getCanonicalName());
        } catch (EbMS3Exception ex) {
            assert (ex.getErrorCode().equals(ErrorCode.EbMS3ErrorCode.EBMS_0010));
            assert (ex.getErrorDetail().contains("The initiator party's role is the same as the responder party's one"));
        }

    }

    @Test
    public void validateAgreementRef_OK() {
        try {
            backendMessageValidatorObj.validateAgreementRef("AgreementRefValue", "AgreementRefType");
        } catch (EbMS3Exception e) {
            Assertions.fail("Exception was not expected here!");
        }
    }

    @Test
    public void validateAgreementRef_NullCheck(@Injectable AgreementRefEntity agreementRef) {
        try {
            backendMessageValidatorObj.validateAgreementRef(null, null);
        } catch (EbMS3Exception e) {
            Assertions.fail("Exception was not expected here as agreementRef is optional and can be null!");
        }
    }

    @Test
    public void validateAgreementRef_TypeTooLong() throws EbMS3Exception {
        String type = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        String value = "AgreementRefValue";

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(type);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateAgreementRef(value, type));
        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("AgreementRef Type is too long (over 255 characters)"));

    }

    @Test
    public void validateAgreementRef_ValueTooLong() throws EbMS3Exception {
        AgreementRefEntity agreementRef = new AgreementRefEntity();
        //agreementRef.setPmode("AgreementRefPMode");
        String type = "AgreementRefType";
        String value = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(value);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateAgreementRef(value, type));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("AgreementRef Value is too long (over 255 characters)"));

    }


    @Test
    public void validateConversationId_ValueTooLong() throws EbMS3Exception {
        String conversationId = StringUtils.repeat("01234", 51) + "1";

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(conversationId);
            result = true;
        }};


        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateConversationId(conversationId));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("ConversationId is too long (over 255 characters)"));

    }

    @Test
    public void validateConversationId_BlankValueTooLong() throws EbMS3Exception {
        String conversationId = StringUtils.repeat(" ", 256);

        backendMessageValidatorObj.validateConversationId(conversationId);
    }

    @Test
    public void validateConversationId_Value255Long() throws EbMS3Exception {
        String conversationId = StringUtils.repeat("01234", 51);
        backendMessageValidatorObj.validateConversationId(conversationId);
    }

    @Test
    public void validateConversationId_ValueNull() throws EbMS3Exception {
        
        backendMessageValidatorObj.validateConversationId(null);
    }

    @Test
    public void validateUserMessageForPmodeMatch_UserMessageNull() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class,
                () -> backendMessageValidatorObj.validateUserMessageForPmodeMatch(null, MSHRole.SENDING));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory header metadata UserMessage is not provided."));
    }

    @Test
    public void validateMessageInfo_MessageInfoNull() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateMessageInfo(null));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory header metadata UserMessage/MessageInfo is not provided."));

    }

    @Test
    public void validateFromPartyId_EmptyFromParties() throws EbMS3Exception {
        Submission submission = new Submission();
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromPartyId(submission));
        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field From PartyId is not provided."));

    }

    @Test
    public void validateFromPartyId_BlankFromPartId() {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party("        ", "        "));

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromPartyId(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field From PartyId is not provided."));


    }

    private From getFrom(String value, String type) {
        From from = new From();
        from.setFromPartyId(new PartyId());
        from.getFromPartyId().setValue(value);
        from.getFromPartyId().setType(type);
        return from;
    }

    private To getTo(String value, String type) {
        To to = new To();
        to.setToPartyId(new PartyId());
        to.getToPartyId().setValue(value);
        to.getToPartyId().setType(type);
        return to;
    }

    @Test
    public void validateFromPartyId_FromPartIdTooLong() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getFromParties().add(new Submission.Party(StringUtils.repeat("X", 256), "        "));

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(submission.getFromParties().iterator().next().getPartyId());
            result = true;
        }};


        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromPartyId(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("From PartyId" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));
    }

    @Test
    public void validateFromPartyId_FromPartIdTypeBlank() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getFromParties().add(new Submission.Party(StringUtils.repeat("X", 255), "        "));


        backendMessageValidatorObj.validateFromPartyId(submission);

    }

    @Test
    public void validateFromPartyId_FromPartIdTypeTooLong() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getFromParties().add(new Submission.Party(StringUtils.repeat("X", 255), StringUtils.repeat("X", 256)));

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(submission.getFromParties().iterator().next().getPartyIdType());
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromPartyId(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("From PartyIdType" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateFromRole_FromNull() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromRole(null));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field From Role is not provided."));
    }

    @Test
    public void validateFromRole_FromRoleBlank() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromRole("   "));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field From Role is not provided."));
    }

    private PartyRole getRole(String value) {
        PartyRole partyRole = new PartyRole();
        partyRole.setValue(value);
        return partyRole;
    }

    @Test
    public void validateFromRole_FromRoleTooLong() throws EbMS3Exception {
        String fromRole = StringUtils.repeat("X", 256);

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(fromRole);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateFromRole(fromRole));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("From Role" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));
    }

    @Test
    public void validateToPartyIdForPModeMatch_EmptyToParties() throws EbMS3Exception {
        Submission submission = new Submission();
        
        backendMessageValidatorObj.validateToPartyIdForPModeMatch(submission);
    }

    @Test
    public void validateToPartyIdForPModeMatch_BlankToPartId() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party("        ", "        "));

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateToPartyIdForPModeMatch(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field To PartyId is not provided."));


    }

    @Test
    public void validateToPartyIdForPModeMatch_ToPartIdTooLong() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party(StringUtils.repeat("X", 256), "        "));

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(submission.getToParties().iterator().next().getPartyId());
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateToPartyIdForPModeMatch(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("To PartyId" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateToPartyIdForPModeMatch_ToPartIdTypeBlank() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party(StringUtils.repeat("X", 255), "        "));

        
        backendMessageValidatorObj.validateToPartyIdForPModeMatch(submission);

    }

    @Test
    public void validateToPartyIdForPModeMatch_ToPartIdTypeTooLong() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party(StringUtils.repeat("X", 255), StringUtils.repeat("X", 256)));

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(submission.getToParties().iterator().next().getPartyIdType());
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateToPartyIdForPModeMatch(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("To PartyIdType" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateToRole_ToNull() {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party(StringUtils.repeat("X", 255), StringUtils.repeat("X", 256)));
        submission.setToRole(null);

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateToRoleForPModeMatch(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field To Role is not provided."));

    }

    @Test
    public void validateToRole_ToRoleBlank() {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party(StringUtils.repeat("X", 255), StringUtils.repeat("X", 256)));
        submission.setToRole("   ");

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateToRoleForPModeMatch(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field To Role is not provided."));

    }

    @Test
    public void validateToRole_ToRoleTooLong() throws EbMS3Exception {
        Submission submission = new Submission();
        submission.getToParties().add(new Submission.Party(StringUtils.repeat("X", 255), StringUtils.repeat("X", 256)));
        submission.setToRole(StringUtils.repeat("X", 256));
        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(submission.getToRole());
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateToRoleForPModeMatch(submission));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("To Role" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateCollaborationInfo_CollaborationInfoNull() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateCollaborationInfo(null));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field UserMessage/CollaborationInfo is not provided."));

    }

    @Test
    public void validateService_ServiceBlank() throws EbMS3Exception {
        String value = "\t";
        String type = "\t";
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateService(value, type));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field Service is not provided."));

    }

    @Test
    public void validateService_ServiceTooLong() throws EbMS3Exception {
        String value = StringUtils.repeat("X", 256);
        String type = "\t";

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(value);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateService(value, type));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Service" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateService_ServiceTypeBlank() throws EbMS3Exception {
        String value = StringUtils.repeat("X", 255);
        String type = "\t";

        
        backendMessageValidatorObj.validateService(value, type);
    }

    @Test
    public void validateService_ServiceTypeTooLong() throws EbMS3Exception {
        String value = StringUtils.repeat("X", 255);
        String type = StringUtils.repeat("X", 256);

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(type);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateService(value, type));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("ServiceType" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateAction_ActionBlank() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateAction("\t"));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field Action is not provided."));
    }

    @Test
    public void validateAction_ActionBlank2() {
        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateAction(null));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Mandatory field Action is not provided."));
    }

    @Test
    public void validateAction_ActionTooLong() throws EbMS3Exception {
        String action = StringUtils.repeat("X", 256);

        new Expectations() {{
            domibusStringUtil.isTrimmedStringLengthLongerThanDefaultMaxLength(action);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> backendMessageValidatorObj.validateAction(action));
        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Action" + DomibusStringUtilImpl.ERROR_MSG_STRING_LONGER_THAN_DEFAULT_STRING_LENGTH));

    }

    @Test
    public void validateMessageIsUnique_notFound() {
        new Expectations() {{
            userMessageLogDao.getMessageStatus("messageId", MSHRole.RECEIVING);
            result = MessageStatus.READY_TO_PULL;
        }};

        DuplicateMessageException messageId = Assertions.assertThrows(DuplicateMessageException.class,
                () -> backendMessageValidatorObj.validateMessageIsUnique("messageId", MSHRole.RECEIVING));

        MatcherAssert.assertThat(messageId.getMessage(), CoreMatchers.containsString(MESSAGE_WITH_ID_STR + "messageId" + " and role " + MSHRole.RECEIVING + BackendMessageValidator.ALREADY_EXISTS_MESSAGE_IDENTIFIERS_MUST_BE_UNIQUE));

    }

    @Test
    public void validateMessageIsUnique_ok() throws DuplicateMessageException {
        new Expectations() {{
            userMessageLogDao.getMessageStatus("messageId", MSHRole.RECEIVING);
            result = MessageStatus.NOT_FOUND;
        }};
        backendMessageValidatorObj.validateMessageIsUnique("messageId", MSHRole.RECEIVING);
    }

    @Test
    public void validateSubmissionPayloadProperty_PayloadPropertyTooLong(@Mocked Submission.TypedProperty payloadProperty) throws EbMS3Exception {
        String payloadPropertyValue = StringUtils.rightPad("TestString", 1025, "Test1");
        new Expectations() {{
            payloadProperty.getValue();
            result = payloadPropertyValue;

            domibusStringUtil.isStringLengthLongerThan1024Chars(payloadPropertyValue);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class,
                () -> backendMessageValidatorObj.validateSubmissionPayloadProperty(payloadProperty, MSHRole.SENDING));
        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("PartProperty is too long (over 1024 characters)."));

    }

    @Test
    public void validateSubmissionPayload_MoreThan28Attachments(@Mocked Submission mockSubmission) throws EbMS3Exception {

        Set<Submission.Payload> payloadSet = new HashSet<>();
        for (int i = 0; i < (DomibusGeneralConstants.DOMIBUS_MAX_ATTACHMENT_COUNT + 1); i++) {
            Submission.Payload mockPayload = new Submission.Payload(Integer.toString(i), null, null, true, null, null);
            payloadSet.add(mockPayload);
        }

        new Expectations() {{
            mockSubmission.getPayloads();
            result = payloadSet;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PAYLOAD_LIMIT_28ATTACHMENTS_PER_MESSAGE);
            result = true;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class,
                () -> backendMessageValidatorObj.validateSubmissionPayload(mockSubmission, MSHRole.SENDING));

        MatcherAssert.assertThat(ebMS3Exception.getMessage(), CoreMatchers.containsString("Maximum number of attachments Domibus can accept in a message is 28."));

    }
}
