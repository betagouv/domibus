package eu.domibus.plugin.validation;

import eu.domibus.plugin.Submission;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by baciuco on 08/08/2016.
 */
@ExtendWith(JMockitExtension.class)
public class PayloadsRequiredSubmissionValidatorTest {

    @Tested
    PayloadsRequiredSubmissionValidator payloadsRequiredSubmissionValidator;

    @Test
    @Disabled("EDELIVERY-6896")
    public void testValidateWithPayloads(@Injectable final Submission submission,
                                         @Injectable final Submission.Payload payload1,
                                         @Injectable final Submission.Payload payload2) throws Exception {
        new Expectations() {{
            submission.getPayloads();
            Set<Submission.Payload> payloads = new HashSet<>();
            payloads.add(payload1);
            payloads.add(payload2);
            result = payloads;
        }};
        payloadsRequiredSubmissionValidator.validate(submission);
    }

    @Test
    void testValidateWithNoPayloads(@Injectable final Submission submission) {
        new Expectations() {{
            submission.getPayloads();
            result = null;
        }};
        Assertions.assertThrows(SubmissionValidationException.class, () -> payloadsRequiredSubmissionValidator.validate(submission));
    }

}
