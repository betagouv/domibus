package eu.domibus.plugin.validation;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.Submission;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.Resource;

import javax.xml.bind.JAXBContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by baciuco on 08/08/2016.
 */
@ExtendWith(JMockitExtension.class)
public class Ebms3SchemaPayloadSubmissionValidatorTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SchemaPayloadSubmissionValidator.class);

    @Injectable
    JAXBContext jaxbContext;

    @Injectable
    Resource schema;

    @Tested
    SchemaPayloadSubmissionValidator schemaPayloadSubmissionValidator;

    @Test
    public void testValidateWithNoPayloads(@Injectable final Submission submission) throws Exception {
        new Expectations() {{
            submission.getPayloads();
            result = null;
        }};
        schemaPayloadSubmissionValidator.validate(submission);

        new Verifications() {{
            schema.getInputStream();
            times = 0;
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    void testValidateWithFirstPayloadInvalid(@Injectable final Submission submission,
                                                    @Injectable final Submission.Payload payload1,
                                                    @Injectable final Submission.Payload payload2) throws Exception {
        new Expectations() {{
            submission.getPayloads();
            Set<Submission.Payload> payloads = new HashSet<>();
            payloads.add(payload1);
            payloads.add(payload2);
            result = payloads;

            schema.getInputStream();
            result = null;
        }};

        Assertions.assertThrows(SubmissionValidationException.class,
                () -> schemaPayloadSubmissionValidator.validate(submission));
        schemaPayloadSubmissionValidator.validate(submission);

        new Verifications() {{
            payload2.getPayloadDatahandler().getInputStream();
            times = 0;
        }};
    }

    @Test
    void testValidateWhenExceptionIsThrown(@Injectable final Submission submission,
                                           @Injectable final Submission.Payload payload1) throws Exception {
        Set<Submission.Payload> payloads = new HashSet<>();
        payloads.add(payload1);
        new Expectations() {{
            submission.getPayloads();
            result = payloads;

            schema.getInputStream();
            result = new NullPointerException();
        }};
        Assertions.assertThrows(SubmissionValidationException.class,
                () -> schemaPayloadSubmissionValidator.validate(submission));

    }

    @Test
    void testValidateWhenExceptionIsThrown2(@Injectable final Submission submission,
                                            @Injectable final Submission.Payload payload1) throws Exception {
        Set<Submission.Payload> payloads = new HashSet<>();
        payloads.add(payload1);
        new Expectations() {{
            submission.getPayloads();
            result = payloads;

            schema.getInputStream();
            result = new SubmissionValidationException();
        }};
        Assertions.assertThrows(SubmissionValidationException.class,
                () -> schemaPayloadSubmissionValidator.validate(submission));

    }
}
