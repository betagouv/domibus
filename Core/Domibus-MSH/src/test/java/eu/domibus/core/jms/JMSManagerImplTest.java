package eu.domibus.core.jms;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.jms.JMSDestination;
import eu.domibus.api.jms.JmsMessage;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.common.NotificationType;
import eu.domibus.core.audit.AuditService;
import eu.domibus.jms.spi.InternalJMSDestination;
import eu.domibus.jms.spi.InternalJMSManager;
import eu.domibus.jms.spi.InternalJmsMessage;
import eu.domibus.messaging.MessageConstants;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class JMSManagerImplTest {

    @Tested
    JMSManagerImpl jmsManager;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private MetricRegistry metricRegistry; 

    @Injectable
    InternalJMSManager internalJmsManager;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    JMSDestinationMapper jmsDestinationMapper;

    @Injectable
    JMSMessageMapper jmsMessageMapper;

    @Injectable
    AuditService auditService;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    AuthUtils authUtils;

    @Injectable
    private MessageConverter messageConverter;

    @Injectable
    private JmsTemplate jsonJmsTemplate;

    @Injectable
    private DomainService domainService;

    @Test
    public void testGetDestinations() {

        final Map<String, InternalJMSDestination> destinations = new HashMap<>();

        new Expectations() {{
            internalJmsManager.findDestinationsGroupedByFQName();
            result = destinations;
        }};

        jmsManager.getDestinations();

        new Verifications() {{
            jmsDestinationMapper.convert(destinations);
            times = 1;
        }};
    }

    @Test
    public void testGetMessage() {
        final String source = "source";
        final String messageId = "messageId";
        final InternalJmsMessage internalJmsMessage = new InternalJmsMessage();

        new Expectations() {{
            internalJmsManager.getMessage(source, messageId);
            result = internalJmsMessage;
        }};

        jmsManager.getMessage(source, messageId);

        new Verifications() {{
            jmsMessageMapper.convert(internalJmsMessage);
            times = 1;
        }};
    }

    @Test
    public void testSendMessageToQueue() {
        final JmsMessage message = new JmsMessage();
        final InternalJmsMessage messageSPI = new InternalJmsMessage();

        new Expectations() {{
            jmsMessageMapper.convert(message);
            result = messageSPI;
        }};

        jmsManager.sendMessageToQueue(message, "myqueue");

        new Verifications() {{
            jmsMessageMapper.convert(message);
            times = 1;

            internalJmsManager.sendMessage(messageSPI, "myqueue");

            Assertions.assertEquals(message.getProperty(JmsMessage.PROPERTY_ORIGINAL_QUEUE), "myqueue");
        }};
    }

    @Test
    public void testSendMessageToJmsQueue(@Injectable final Queue queue) throws JMSException {
        final JmsMessage message = new JmsMessage();
        final InternalJmsMessage messageSPI = new InternalJmsMessage();


        new Expectations() {{
            jmsMessageMapper.convert(message);
            result = messageSPI;

            queue.getQueueName();
            result = "myqueue";
        }};

        jmsManager.sendMessageToQueue(message, queue);

        new Verifications() {{
            jmsMessageMapper.convert(message);
            times = 1;

            internalJmsManager.sendMessage(messageSPI, queue);

            Assertions.assertEquals(message.getProperty(JmsMessage.PROPERTY_ORIGINAL_QUEUE), "myqueue");
        }};
    }

    @Test
    public void testDeleteMessages()  {
        final String source = "myqueue";
        final String[] messageIds = new String[]{"1", "2"};

        List<JMSMessageDomainDTO> jmsMessageIDDomains = new ArrayList<>();
        jmsMessageIDDomains.add(new JMSMessageDomainDTO("1", "domain1"));
        jmsMessageIDDomains.add(new JMSMessageDomainDTO("2", "domain2"));

        new Expectations(jmsManager) {{
            jmsManager.getJMSMessageDomain(source, messageIds);
            result = jmsMessageIDDomains;

            internalJmsManager.deleteMessages(source, messageIds);
            result = 2;

        }};

        jmsManager.deleteMessages(source, messageIds);

        new FullVerifications(jmsManager) {{
            internalJmsManager.deleteMessages(source, messageIds);
            String actualDomain;
            auditService.addJmsMessageDeletedAudit("1", source, actualDomain = withCapture());
            Assertions.assertEquals("domain1", actualDomain);
            auditService.addJmsMessageDeletedAudit("2", source, actualDomain = withCapture());
            Assertions.assertEquals("domain2", actualDomain);
        }};
    }

    @Test
    public void testMoveMessages_ok() {
        final String source = "myqueue";
        final String destination = "destinationQueue";
        final String[] messageIds = new String[]{"1", "2"};
        List<JMSMessageDomainDTO> jmsMessageIDDomains = new ArrayList<>();
        final String domain1 = "domain1";
        jmsMessageIDDomains.add(new JMSMessageDomainDTO("1", domain1));
        final String domain2 = "domain2";
        jmsMessageIDDomains.add(new JMSMessageDomainDTO("2", domain2));

        new Expectations(jmsManager) {{
            jmsManager.validateMessagesMove(source, jmsManager.getValidJmsDestination(destination), messageIds);

            jmsManager.getJMSMessageDomain(source, messageIds);
            result = jmsMessageIDDomains;

            internalJmsManager.moveMessages(source, destination, messageIds);
            result = 2;

        }};

        jmsManager.moveMessages(source, destination, messageIds);

        new FullVerifications(jmsManager) {{
            internalJmsManager.moveMessages(source, destination, messageIds);
            String actualDomain;
            auditService.addJmsMessageMovedAudit("1", source, destination, actualDomain = withCapture());
            Assertions.assertEquals(domain1, actualDomain);
            auditService.addJmsMessageMovedAudit("2", source, destination, actualDomain = withCapture());
            Assertions.assertEquals(domain2, actualDomain);
        }};
    }

    @Test
    public void test_retrieveDomainFromJMSMessage(final @Injectable JmsMessage jmsMessage) {
        final String sourceQueue = "fromQueue";
        final String jmsMessageID = "jmsMessageID";

        new Expectations(jmsManager) {{
            domibusConfigurationService.isSingleTenantAware();
            result = false;

            jmsManager.getMessage(sourceQueue, jmsMessageID);
            result = jmsMessage;

            jmsMessage.getProperty(MessageConstants.DOMAIN);
            result = "domain1";
        }};

        jmsManager.retrieveDomainFromJMSMessage(sourceQueue, jmsMessageID);

        new FullVerifications(jmsManager) {};
    }

    @Test
    public void testGetDomainSelector_MultiTenant_SuperAdmin() {

        final String selector = "myselector";

        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;

            authUtils.isSuperAdmin();
            result = true;

        }};

        Assertions.assertEquals(selector, jmsManager.getDomainSelector(selector));

        new FullVerifications() {
        };
    }

    @Test
    public void testGetDomainSelector_MultiTenant_Admin() {

        final String selector = "myselector";

        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;

            authUtils.isSuperAdmin();
            result = false;

            domainContextProvider.getCurrentDomain();
            result = new Domain("digit", "digit");

        }};

        Assertions.assertEquals(selector + " AND DOMAIN ='digit'", jmsManager.getDomainSelector(selector));

        new FullVerifications() {
        };
    }

    @Test
    public void testGetDomainSelector_MultiTenant_Admin_EmptySelector() {
        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;

            authUtils.isSuperAdmin();
            result = false;

            domainContextProvider.getCurrentDomain();
            result = new Domain("digit1", "digit1");

        }};

        Assertions.assertEquals("DOMAIN ='digit1'", jmsManager.getDomainSelector(null));

        new FullVerifications() {
        };
    }

    @Test
    public void testJmsQueueInOtherDomain_Domain1Current_QueueDomain2() {
        final String jmsQueueInternalName = "domain2.domibus.backend.jms.outQueue";

        final List<Domain> domains = new ArrayList<>();
        domains.add(DomainService.DEFAULT_DOMAIN);
        Domain domain1 = new Domain();
        domain1.setCode("domain1");
        domain1.setName("Domain1");
        domains.add(domain1);

        Domain domain2 = new Domain();
        domain2.setCode("domain2");
        domain2.setName("Domain2");
        domains.add(domain2);

        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;

            authUtils.isSuperAdmin();
            result = false;

            domainService.getDomains();
            result = domains;

            domainContextProvider.getCurrentDomainSafely();
            result = domain1;

        }};

        Assertions.assertTrue(jmsManager.jmsQueueInOtherDomain(jmsQueueInternalName));

    }

    @Test
    public void testJmsQueueInOtherDomain_Domain1Current_QueueDomain1() {
        final String jmsQueueInternalName = "domain1.domibus.backend.jms.outQueue";

        final List<Domain> domains = new ArrayList<>();
        domains.add(DomainService.DEFAULT_DOMAIN);
        Domain domain1 = new Domain();
        domain1.setCode("domain1");
        domain1.setName("Domain1");
        domains.add(domain1);

        Domain domain2 = new Domain();
        domain2.setCode("domain2");
        domain2.setName("Domain2");
        domains.add(domain2);

        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;

            authUtils.isSuperAdmin();
            result = false;

            domainService.getDomains();
            result = domains;

            domainContextProvider.getCurrentDomainSafely();
            result = domain1;

        }};

        Assertions.assertFalse(jmsManager.jmsQueueInOtherDomain(jmsQueueInternalName));
    }

    @Test
    public void testJmsQueueInOtherDomain_NonMultitenancy() {
        final String jmsQueueInternalName = "domain1.domibus.backend.jms.outQueue";

        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = false;
        }};

        Assertions.assertFalse(jmsManager.jmsQueueInOtherDomain(jmsQueueInternalName));
    }

    @Test
    public void sortQueuesInClusterTest() {
        Map<String, JMSDestination> queues = new HashMap<>();
        queues.put("cluster2@queueX", new JMSDestination() {{
            setName("cluster2@queueX");
        }});
        queues.put("cluster2@queueY", new JMSDestination() {{
            setName("cluster2@queueY");
        }});
        queues.put("cluster1@queueX", new JMSDestination() {{
            setName("cluster1@queueX");
        }});
        queues.put("cluster1@queueY", new JMSDestination() {{
            setName("cluster1@queueY");
        }});
        queues.put("queueXY", new JMSDestination() {{
            setName("queueXY");
        }});
        JMSDestination[] sortedValues = jmsManager.sortQueues(queues).values().toArray(new JMSDestination[0]);

        Assertions.assertEquals("cluster1@queueX", sortedValues[0].getName());
        Assertions.assertEquals("cluster2@queueX", sortedValues[1].getName());
        Assertions.assertEquals("cluster1@queueY", sortedValues[3].getName());
        Assertions.assertEquals("cluster2@queueY", sortedValues[4].getName());
    }

    @Test
    public void listPendingMessages() {
        String queueName = "mysqueue";
        String originalUser = "C1";
        new Expectations(jmsManager) {{
            authUtils.getOriginalUserWithUnsecureLoginAllowed();
            result = originalUser;

            authUtils.isUnsecureLoginAllowed();
            result = true;

            jmsManager.getQueueElements(queueName, NotificationType.MESSAGE_RECEIVED, originalUser);
        }};

        jmsManager.listPendingMessages(queueName);

        new FullVerifications() {};
    }

    @Test
    public void browseQueue(@Injectable JmsMessage jmsMessage1,
                            @Injectable JmsMessage jmsMessage2) {
        String queueName = "myqueue";
        String originalUser = "C1";
        String selector = "name = value";
        List<JmsMessage> jmsMessages = new ArrayList<>();
        jmsMessages.add(jmsMessage1);
        jmsMessages.add(jmsMessage2);

        String messageId1 = "msg1";
        String messageId2 = "msg2";

        new Expectations(jmsManager) {{
            jmsManager.getDomainSelector(anyString);
            result = selector;

            jmsMessage1.getCustomStringProperty(MessageConstants.MESSAGE_ID);
            result = messageId1;

            jmsMessage2.getCustomStringProperty(MessageConstants.MESSAGE_ID);
            result = messageId2;

            jmsManager.browseClusterMessages(queueName, selector);
            result = jmsMessages;
        }};

        Collection<String> messageList = jmsManager.browseQueue(queueName, NotificationType.MESSAGE_RECEIVED, originalUser);
        assertEquals(2, messageList.size());
    }

    @Test
    public void removeFromPending(@Injectable JmsMessage message) {
        String queueName = "myqueue";
        String messageId = "123";

        new Expectations(jmsManager) {{
            authUtils.isUnsecureLoginAllowed();
            result = true;

            jmsManager.consumeMessage(queueName, messageId);
            result = message;
        }};

        jmsManager.removeFromPending(queueName, messageId);

        new FullVerifications() {};
    }

    @Test
    public void testValidateMessageMove_BlankAndNullIds() {
        String source = "src";
        String destination = "dest1";
        JMSDestination jmsDestination = new JMSDestination();
        jmsDestination.setName(destination);
        String[] messageIds = {"", null};

        // When
        try {
            jmsManager.validateMessagesMove(source, jmsDestination, messageIds);
            Assertions.fail();
        } catch (RequestValidationException e) {
            //do nothing
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateMessageMove_NoIds() {
        String source = "src";
        String destination = "dest1";
        JMSDestination jmsDestination = new JMSDestination();
        jmsDestination.setName(destination);
        String[] messageIds = {};

        // When
        try {
            jmsManager.validateMessagesMove(source, jmsDestination, messageIds);
            Assertions.fail();
        } catch (RequestValidationException e) {
            //do nothing
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateMessageMove_SameDestAsSrc() {
        String source = "src";
        String destination = "src";
        String[] messageIds = {"id1"};
        JMSDestination jmsDestination = new JMSDestination();
        jmsDestination.setName(destination);

        // When
        try {
            jmsManager.validateMessagesMove(source, jmsDestination, messageIds);
            Assertions.fail();
        } catch (RequestValidationException e) {
            //do nothing
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testActionMove_InvalidMsgId(final @Injectable JMSDestination queue1) {
        String source = "src";
        String[] messageIds = {"id1"};
        // Given
        new Expectations() {{
            queue1.getName();
            result = "domibus.queue1";

            internalJmsManager.getMessage(source, "id1");
            result = null;
        }};

        assertThrows(RequestValidationException.class, ()-> jmsManager.validateMessagesMove(source, queue1, messageIds));

        new FullVerifications() {
        };
    }

    @Test
    public void testActionMove_DifferentThanOriginalQueue(final @Injectable JMSDestination queue1, @Injectable InternalJmsMessage msg) {
        String source = "src";
        String[] messageIds = {"id1"};
        Map<String, String> getCustomProperties =new HashMap<>();
        getCustomProperties.put("originalQueue", "some other queue");
        // Given
        new Expectations() {{


            queue1.getName();
            result = "domibus.queue1";

            queue1.getFullyQualifiedName();
            result = "domibus.queue1";

            internalJmsManager.getMessage(source, "id1");
            result = msg;

            msg.getCustomProperties();
            result = getCustomProperties;
        }};


        assertThrows(RequestValidationException.class, () -> jmsManager.validateMessagesMove(source, queue1, messageIds));

        new FullVerifications() {
        };
    }

    @Test
    public void test_matchQueue() {
        List<Pair<String, String>> matchingQueueNames = Arrays.asList(
                // weblogic
                new ImmutablePair<>("eDeliveryModule!DomibusBusinessMessageOutQueue", "DomibusBusinessMessageOutQueue")
                ,
                // weblogic cluster
                new ImmutablePair<>("eDeliveryModule!DomibusBusinessMessageOutQueue", "sv2@DomibusBusinessMessageOutQueue")
        );

        matchingQueueNames.forEach(test -> {
            String originalQueueName = test.getLeft();
            String testQueueName = test.getRight();
            JMSDestination testQueue = new JMSDestination();
            testQueue.setName(testQueueName);

            boolean result = jmsManager.matchQueue(testQueue, originalQueueName);
            Assertions.assertTrue(result, "[" + testQueueName + "] should match [" + originalQueueName + "]");
        });

        List<Pair<String, String>> notMatchingQueueNames = Arrays.asList(
                new ImmutablePair<>("QueueName", "LongQueueName")
                ,
                new ImmutablePair<>("ABC!Name", "Name@QueueName")
        );

        notMatchingQueueNames.forEach(test -> {
            String originalQueueName = test.getLeft();
            String testQueueName = test.getRight();
            JMSDestination testQueue = new JMSDestination();
            testQueue.setName(testQueueName);

            boolean result = jmsManager.matchQueue(testQueue, originalQueueName);
            Assertions.assertFalse(result, "[" + testQueueName + "] should not match [" + originalQueueName + "]");
        });
    }
}
