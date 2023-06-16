package eu.domibus.core.metrics;

import com.codahale.metrics.Metric;
import eu.domibus.api.jms.JMSDestination;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.functions.AuthenticatedFunction;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Catalin Enache *
 * @since 4.1.1
 */
public class JMSQueuesCountSetTest {

    @Injectable
    JMSManager jmsManager;

    @Injectable
    AuthUtils authUtils;

    @Injectable
    DomainTaskExecutor domainTaskExecutor;

    @Test
    public void test_GetMetrics_FromDLQ() {
        //tested class
        JMSQueuesCountSet jmsQueuesCountSet = new JMSQueuesCountSet(jmsManager, authUtils, domainTaskExecutor, 20, true, null, false);

        final Map<String, JMSDestination> jmsDestinationList = new TreeMap<>();
        final long nbMessages = 20;
        final String queueName = "domibus.DLQ";
        final JMSDestination jmsDestination = new JMSDestination();
        jmsDestination.setName(queueName);
        jmsDestination.setNumberOfMessages(nbMessages);
        jmsDestination.setType("Queue");
        jmsDestination.setInternal(true);
        jmsDestinationList.put(queueName, jmsDestination);

        new Expectations(jmsQueuesCountSet) {{
            jmsQueuesCountSet.getQueueNamesDLQ();
            result = Collections.singletonList(jmsDestination);
        }};

        //tested method
        final Map<String, Metric> metrics = jmsQueuesCountSet.getMetrics();

        Assertions.assertNotNull(metrics);
        Assertions.assertTrue(metrics.size() == 1);
        Assertions.assertTrue(metrics.containsKey(queueName));
    }

    @Test
    public void test_GetMetrics_FromQueues() {
        //tested class
        JMSQueuesCountSet jmsQueuesCountSet = new JMSQueuesCountSet(jmsManager, authUtils, domainTaskExecutor, 20, false, null, false);

        final Map<String, JMSDestination> jmsDestinationList = new TreeMap<>();
        final long nbMessages = 20;
        final String queueName = "domibus.my.queue";
        final JMSDestination jmsDestination = new JMSDestination();
        jmsDestination.setName(queueName);
        jmsDestination.setNumberOfMessages(nbMessages);
        jmsDestination.setType("Queue");
        jmsDestination.setInternal(true);
        jmsDestinationList.put(queueName, jmsDestination);

        new Expectations(jmsQueuesCountSet) {{
            jmsQueuesCountSet.getQueuesAuthenticated();
            result = Collections.singletonList(jmsDestination);
        }};

        //tested method
        final Map<String, Metric> metrics = jmsQueuesCountSet.getMetrics();

        Assertions.assertNotNull(metrics);
        Assertions.assertTrue(metrics.size() == 1);
        Assertions.assertTrue(metrics.containsKey(queueName));
    }

    @Test
    public void test_getQueues() {
        //tested class
        JMSQueuesCountSet jmsQueuesCountSet = new JMSQueuesCountSet(jmsManager, authUtils, domainTaskExecutor, 20, false, null, false);

        final Map<String, JMSDestination> jmsDestinationList = new TreeMap<>();
        final long nbMessages = 20;
        final String queueName = "domibus.my.queue";
        final JMSDestination jmsDestination = new JMSDestination();
        jmsDestination.setName(queueName);
        jmsDestination.setNumberOfMessages(nbMessages);
        jmsDestination.setType("Queue");
        jmsDestination.setInternal(true);
        jmsDestinationList.put(queueName, jmsDestination);

        new Expectations() {{
            jmsManager.getDestinations();
            result = jmsDestinationList;
        }};

        //tested method
        List<JMSDestination> result = jmsQueuesCountSet.getQueues();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(queueName, result.get(0).getName());
    }

    @Test
    public void test_getQueuesAuthenticated() {
        //tested class
        JMSQueuesCountSet jmsQueuesCountSet = new JMSQueuesCountSet(jmsManager, authUtils, domainTaskExecutor, 20, false, null, false);

        new Expectations() {{
            authUtils.runFunctionWithSecurityContext((AuthenticatedFunction) any, anyString, anyString, (AuthRole) any);
        }};

        jmsQueuesCountSet.getQueuesAuthenticated();

        new Verifications() {{
            AuthenticatedFunction function;
            String username;
            String password;
            AuthRole role;
            authUtils.runFunctionWithSecurityContext(function = withCapture(),
                    username = withCapture(), password = withCapture(), role = withCapture());

            Assertions.assertNotNull(function);
            Assertions.assertEquals("jms_metrics_user", username);
            Assertions.assertEquals("jms_metrics_password", password);
            Assertions.assertEquals(AuthRole.ROLE_AP_ADMIN, role);

        }};
    }
}
