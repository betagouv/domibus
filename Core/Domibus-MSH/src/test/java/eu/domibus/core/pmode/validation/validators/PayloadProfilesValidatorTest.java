package eu.domibus.core.pmode.validation.validators;

import eu.domibus.common.model.configuration.Attachment;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Payload;
import eu.domibus.common.model.configuration.PayloadProfile;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

/**
 * @author Catalin Enache
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PayloadProfilesValidatorTest {

    @Tested
    PayloadProfilesValidator payloadProfilesValidator;

    @Injectable
    PModeValidationHelper pModeValidationHelper;

    @Test
    public void test_validate(final @Injectable Configuration configuration,
                              final @Injectable PayloadProfile payloadProfile) {


        final Set<PayloadProfile> payloadProfileList = Collections.singleton(payloadProfile);

        new Expectations() {{
            configuration.getBusinessProcesses().getPayloadProfiles();
            result = payloadProfileList;

            configuration.getBusinessProcesses().getPayloads();
            result = new HashSet<>();
        }};


        payloadProfilesValidator.validate(configuration);

        new FullVerifications() {{
            payloadProfilesValidator.validatePayloadProfile(payloadProfile, (Set<Payload>) any);
        }};
    }

    @Test
    public void test_validateEmptyPayloadProfile(final @Injectable Configuration configuration,
                                                 final @Injectable PayloadProfile payloadProfile) {


        new Expectations(payloadProfilesValidator) {{
            configuration.getBusinessProcesses().getPayloadProfiles();
            result = null;

            configuration.getBusinessProcesses().getPayloads();
            result = new HashSet<>();
        }};


        payloadProfilesValidator.validate(configuration);

        new FullVerifications() {{
        }};
    }

    @Test
    public void test_validatePayloadProfile(final @Injectable PayloadProfile payloadProfile) {
        final List<Attachment> attachmentList = new ArrayList<>();

        new Expectations() {{
            pModeValidationHelper.getAttributeValue(payloadProfile, "attachment", List.class);
            result = attachmentList;

            payloadProfile.getMaxSize();
            result = 400;
        }};

        payloadProfilesValidator.validatePayloadProfile(payloadProfile, new HashSet<>());

        new FullVerifications() {{
        }};
    }

    @Test
    public void test_validatePayloadProfile_MaxSizeNegative(final @Injectable PayloadProfile payloadProfile,
                                                            final @Injectable Payload payload,
                                                            final @Injectable Attachment attachment) {
        new Expectations(payloadProfilesValidator) {{
            pModeValidationHelper.getAttributeValue(payloadProfile, "attachment", List.class);
            result = Collections.singletonList(attachment);

            payload.getName();
            result = "test payload";

            attachment.getName();
            result = "attachment";

            payloadProfile.getMaxSize();
            result = -20;
        }};

        payloadProfilesValidator.validatePayloadProfile(payloadProfile, Collections.singleton(payload));

        new FullVerifications(payloadProfilesValidator) {{
            payloadProfilesValidator.createIssue(payloadProfile, anyString, anyString);
        }};
    }

    @Test
    public void test_createIssue(final @Injectable PayloadProfile payloadProfile) {
        final String message = "message";
        final String name = "name";

        //tested method
        payloadProfilesValidator.createIssue(payloadProfile, name, message);

        new FullVerifications() {{
            pModeValidationHelper.createValidationIssue(message, name, payloadProfile.getName());
        }};
    }

    @Test
    public void test_validatePayloadProfileCaseInsensitive(final @Injectable PayloadProfile payloadProfile,
                                                           final @Injectable Payload payload,
                                                           final @Injectable Attachment attachment) {

        List<Attachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);
        Set<Payload> validPayloads = new HashSet<>();
        validPayloads.add(payload);
        new Expectations(payloadProfilesValidator) {{
            pModeValidationHelper.getAttributeValue(payloadProfile, "attachment", List.class);
            result = attachmentList;

            attachment.getName();
            result = "businessContentAttachment";

            payload.getName();
            result = "BusinessContentAttachment";

            payloadProfile.getMaxSize();
            result = 20;
        }};

        payloadProfilesValidator.validatePayloadProfile(payloadProfile, validPayloads);

        new FullVerifications() {{

        }};
    }
}
