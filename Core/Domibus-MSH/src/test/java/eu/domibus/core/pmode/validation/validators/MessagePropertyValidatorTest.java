package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.model.*;
import eu.domibus.core.ebms3.EbMS3Exception;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.UUID;

/**
 * @author Catalin Enache
 * @since 4.1.5
 */
@ExtendWith(JMockitExtension.class)
public class MessagePropertyValidatorTest {

    @Tested
    MessagePropertyValidator messagePropertyValidator;

    @Test
    public void test_validate_MessageProperty_OK(final @Mocked UserMessage userMessage,
                                                 final @Mocked MessageProperty property) throws Exception {

        new Expectations() {{
            userMessage.getMessageId();
            result = "message " + UUID.randomUUID();

            userMessage.getMessageProperties();
            result = Collections.singleton(property);

            property.getName();
            result = "propName";

            property.getValue();
            result = "test";

            property.getType();
            result = "propType";
        }};

        messagePropertyValidator.validate(userMessage, MSHRole.SENDING);

        new FullVerifications() {{

        }};
    }

    @Test
    public void test_validate_MessageProperty_Exception(final @Mocked UserMessage userMessage,
                                                        final @Mocked MessageProperty property) {

        new Expectations() {{
            userMessage.getMessageId();
            result = "message " + UUID.randomUUID();

            userMessage.getMessageProperties();
            result = Collections.singleton(property);

            property.getName();
            result = "errorMsg";

            property.getValue();
            result = "An error was received from eTranslation service while processing the message id: 5f663af9-39ef-450b-aa3d-b7a33624368c@domibus.eu. The message says: Translation request must be text type, document path type or document base64 type and not several at a time.An error was received from eTranslation service while processing the message id: 5f663af9-39ef-450b-aa3d-b7a33624368c@domibus.eu. The message says: Translation request must be text type, document path type or document base64 type and not several at a time.An error was received from eTranslation service while processing the message id: 5f663af9-39ef-450b-aa3d-b7a33624368c@domibus.eu. The message says: Translation request must be text type, document path type or document base64 type and not several at a time. An error was received from eTranslation service while processing the message id: 5f663af9-39ef-450b-aa3d-b7a33624368c@domibus.eu. The message says: Translation request must be text type, document path type or document base64 type and not several at a time.";

        }};

        try {
            messagePropertyValidator.validate(userMessage, MSHRole.SENDING);
            Assertions.fail("exception expected");
        } catch (EbMS3Exception e) {
            Assertions.assertTrue(e.getMessage().contains("property has a value which exceeds 1024 characters size."));
        }
    }

    @Test
    public void test_validate_MessageProperty_Null(final @Mocked UserMessage userMessage,
                                                 final @Mocked MessageProperty property) throws Exception {

        new Expectations() {{
            userMessage.getMessageId();
            result = "message " + UUID.randomUUID();

            userMessage.getMessageProperties();
            result = Collections.singleton(property);

            property.getName();
            result = "propName";

            property.getValue();
            result = null;

            property.getType();
            result = null;

        }};

        messagePropertyValidator.validate(userMessage, MSHRole.SENDING);

        new FullVerifications() {{

        }};
    }
}
