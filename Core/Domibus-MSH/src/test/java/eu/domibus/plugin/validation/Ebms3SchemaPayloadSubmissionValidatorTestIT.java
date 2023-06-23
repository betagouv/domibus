package eu.domibus.plugin.validation;

import eu.domibus.plugin.Submission;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Created by baciuco on 08/08/2016.
 */
@ExtendWith(JMockitExtension.class)
public class Ebms3SchemaPayloadSubmissionValidatorTestIT {

    SchemaPayloadSubmissionValidator schemaPayloadSubmissionValidator;

    @BeforeEach
    public void init() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance("eu.domibus.core.plugin.validation");
        schemaPayloadSubmissionValidator = new SchemaPayloadSubmissionValidator();
        schemaPayloadSubmissionValidator.setJaxbContext(jaxbContext);
        schemaPayloadSubmissionValidator.setSchema(new ClassPathResource("eu/domibus/core/plugin/validation/payload.xsd"));
    }

    @Test
    public void testValidatePayloadWithValidPayload(@Injectable final Submission.Payload payload) throws Exception {
        new Expectations() {{
            payload.getPayloadDatahandler().getInputStream();
            result = new ClassPathResource("eu/domibus/core/plugin/validation/validPayload.xml").getInputStream();
        }};

        schemaPayloadSubmissionValidator.validatePayload(payload);
    }

    @Test
    void testValidatePayloadWithValidInvalidPayload(@Injectable final Submission.Payload payload) throws Exception {
        new Expectations() {{
            payload.getPayloadDatahandler().getInputStream();
            result = new ClassPathResource("eu/domibus/core/plugin/validation/invalidPayload.xml").getInputStream();
        }};

        Assertions.assertThrows(SubmissionValidationException. class,() -> schemaPayloadSubmissionValidator.validatePayload(payload));
    }
}
