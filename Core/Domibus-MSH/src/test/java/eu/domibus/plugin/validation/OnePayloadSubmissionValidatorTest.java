package eu.domibus.plugin.validation;

import eu.domibus.plugin.Submission;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by baciuco on 08/08/2016.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class OnePayloadSubmissionValidatorTest {

    @Injectable
    SubmissionValidator payloadsRequiredSubmissionValidator;

    @Tested
    OnePayloadSubmissionValidator onePayloadSubmissionValidator;

    @Test
    public void testValidateWithOnePayload(@Injectable final Submission submission,
                                           @Injectable final Submission.Payload payload1) {
        Set<Submission.Payload> payloads = new HashSet<>();
        payloads.add(payload1);
        new Expectations() {{
            submission.getPayloads();
            result = payloads;
        }};
        onePayloadSubmissionValidator.validate(submission);
    }

    @Test
    void testValidateWithNoPayloads(@Injectable final Submission submission,
                                    @Injectable final Submission.Payload payload1,
                                    @Injectable final Submission.Payload payload2) {
        Set<Submission.Payload> payloads = new HashSet<>();
        payloads.add(payload1);
        payloads.add(payload2);
        new Expectations() {{
            submission.getPayloads();
            result = payloads;
        }};
        Assertions.assertThrows(SubmissionValidationException.class, () -> onePayloadSubmissionValidator.validate(submission));
    }

}
