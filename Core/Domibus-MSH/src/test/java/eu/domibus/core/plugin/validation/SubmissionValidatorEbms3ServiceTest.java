package eu.domibus.core.plugin.validation;

import eu.domibus.api.model.UserMessage;
import eu.domibus.core.plugin.transformer.SubmissionAS4Transformer;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.validation.SubmissionValidationException;
import eu.domibus.plugin.validation.SubmissionValidator;
import eu.domibus.plugin.validation.SubmissionValidatorList;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
class SubmissionValidatorEbms3ServiceTest {

    @Tested
    SubmissionValidatorService submissionValidatorService;

    @Injectable
    SubmissionValidatorListProvider submissionValidatorListProvider;

    @Injectable
    SubmissionAS4Transformer submissionAS4Transformer;

    @Test
    void testValidateSubmissionWhenFirstValidatorThrowsException(@Injectable final Submission submission,
                                                                 @Injectable final UserMessage userMessage,
                                                                 @Injectable final SubmissionValidatorList submissionValidatorList,
                                                                 @Injectable final SubmissionValidator validator1,
                                                                 @Injectable final SubmissionValidator validator2) {
        final String backendName = "customPlugin";
        new Expectations() {{
            submissionAS4Transformer.transformFromMessaging(userMessage, null);
            result = submission;
            submissionValidatorListProvider.getSubmissionValidatorList(backendName);
            result = submissionValidatorList;
            submissionValidatorList.getSubmissionValidators();
            result = new SubmissionValidator[]{validator1, validator2};
            validator1.validate(submission);
            result = new SubmissionValidationException("Exception in the validator1");
        }};

        Assertions.assertThrows(SubmissionValidationException.class,
                () -> submissionValidatorService.validateSubmission(userMessage, null, backendName));

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateSubmissionWithAllValidatorsCalled(@Injectable final Submission submission,
                                                              @Injectable final UserMessage userMessage,
                                                              @Injectable final SubmissionValidatorList submissionValidatorList,
                                                              @Injectable final SubmissionValidator validator1,
                                                              @Injectable final SubmissionValidator validator2) {
        final String backendName = "customPlugin";
        new Expectations() {{
            submissionAS4Transformer.transformFromMessaging(userMessage, null);
            result = submission;
            submissionValidatorListProvider.getSubmissionValidatorList(backendName);
            result = submissionValidatorList;
            submissionValidatorList.getSubmissionValidators();
            result = new SubmissionValidator[]{validator1, validator2};
        }};

        submissionValidatorService.validateSubmission(userMessage, null, backendName);

        new FullVerifications() {{
            validator1.validate(submission);
            times = 1;
            validator2.validate(submission);
            times = 1;
        }};
    }

    @Test
    void testValidateSubmission_noValidator(@Injectable final Submission submission,
                                            @Injectable final UserMessage userMessage,
                                            @Injectable final SubmissionValidatorList submissionValidatorList,
                                            @Injectable final SubmissionValidator validator1,
                                            @Injectable final SubmissionValidator validator2) {
        final String backendName = "customPlugin";
        new Expectations() {{
            submissionValidatorListProvider.getSubmissionValidatorList(backendName);
            result = null;
        }};

        submissionValidatorService.validateSubmission(userMessage, null, backendName);

        new FullVerifications() {
        };
    }
}
