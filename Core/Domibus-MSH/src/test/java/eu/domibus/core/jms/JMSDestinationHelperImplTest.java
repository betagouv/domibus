package eu.domibus.core.jms;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.RegexUtil;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@ExtendWith(JMockitExtension.class)
public class JMSDestinationHelperImplTest {

    @Injectable
    RegexUtil regexUtil;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Tested
    JMSDestinationHelperImpl jmsDestinationHelper;

    @Test
    public void testIsInternalWithNoInternalExpressionDefined() throws Exception {
        String queueName = "myQueue";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = null;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assertions.assertFalse(internal);
    }

    @Test
    public void testIsInternal() throws Exception {
        final String queueName = "myQueue";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = "myexpression";

            regexUtil.matches("myexpression", queueName);
            result = true;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assertions.assertTrue(internal);

    }

    @Test
    public void testWildflyQueueNameIsInternal() {
        final String queueName = "jms.queue.DomibusAlertMessageQueue";
        final String INTERNALQUEUE_EXPRESSION = ".*jms.queue.(Domibus[a-zA-Z]|DLQ|ExpiryQueue|internal|backend.jms|notification.jms|notification.webservice|notification.kerkovi|notification.filesystem).*";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = INTERNALQUEUE_EXPRESSION;

            regexUtil.matches(INTERNALQUEUE_EXPRESSION, queueName);
            result = true;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assertions.assertTrue(internal);
    }

    @Test
    public void testWeblogicQueueNameIsInternal() {
        final String queueName = "domibus.backend.jms.inQueue ";
        final String INTERNALQUEUE_EXPRESSION = ".*domibus.(internal|DLQ|backend|.jms|notification|.jms|notification|.webservice|notification|.kerkovi|notification|.filesystem).*";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = INTERNALQUEUE_EXPRESSION;

            regexUtil.matches(INTERNALQUEUE_EXPRESSION, queueName);
            result = true;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assertions.assertTrue(internal);
    }

    @Test
    public void testTomcatQueueNameIsInternal() {
        final String queueName = "domibus.backend.jms.outQueue";
        final String INTERNALQUEUE_EXPRESSION = ".*domibus.(internal|DLQ|backend|.jms|notification|.jms|notification|.webservice|notification|.kerkovi|notification|.filesystem).*";
        new Expectations() {{
            domibusPropertyProvider.getProperty(anyString);
            result = INTERNALQUEUE_EXPRESSION;

            regexUtil.matches(INTERNALQUEUE_EXPRESSION, queueName);
            result = true;
        }};

        boolean internal = jmsDestinationHelper.isInternal(queueName);
        Assertions.assertTrue(internal);
    }
}
