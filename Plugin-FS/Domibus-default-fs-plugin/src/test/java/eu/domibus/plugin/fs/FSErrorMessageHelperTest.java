package eu.domibus.plugin.fs;

import mockit.Expectations;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(JMockitExtension.class)
public class FSErrorMessageHelperTest {

    @Tested
    private FSErrorMessageHelper instance;

    @Test
    public void buildErrorMessageWithErrorDetailsTest() {

        final String errorDetail = null;

        new Expectations(instance) {{
            instance.buildErrorMessage(null, null, null, null, null, null);
            result = any;
        }};

        Assertions.assertNull(instance.buildErrorMessage(errorDetail));

    }

    @Test
    public void testbuildErrorMessage() {
        final String errorCode = "DOM_001";
        final String errorDetail = "Error";
        final String messageId = "messageId";
        final String mshRole = "mshRole";
        final String notified = "notified";
        final String timestamp = null;

        Assertions.assertNotNull(instance.buildErrorMessage(errorCode, errorDetail, messageId, mshRole, notified, timestamp));
    }

}