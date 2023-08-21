package eu.domibus.jms.activemq;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sebastian-Ion TINCU
 * @since 5.0.1
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class DomibusJMSActiveMQBrokerTest {

    @Tested
    private DomibusJMSActiveMQBroker domibusJMSActiveMQBroker;

    @Injectable
    private ObjectProvider<BrokerViewMBean> brokerViewMBeans;

    @Injectable
    private ObjectProvider<MBeanServerConnection> mBeanServerConnections;

    @Injectable
    private BrokerViewMBean brokerViewMBean;

    @Injectable
    private MBeanServerConnection mBeanServerConnection;

    @Injectable
    private String brokerName = "broker";

    @Injectable
    private String serviceUrl = "service:jmx:rmi:///jndi/rmi://localhost:123/jmxrmi";

    @BeforeEach
    public void ignorePostConstructInvocation() {
        new MockUp<DomibusJMSActiveMQBroker>() {
            @Mock
            void init(Invocation invocation) {
                // ignore the first invocation due to @PostConstruct
                if (invocation.getInvocationCount() > 1) {
                    invocation.proceed();
                }
            }
        };
    }

    @Test
    public void isMaster_returnsTrueWhenBrokerViewMBeanReturnsTheSlaveFlagAsFalse() {
        // GIVEN
        new Expectations() {{
            brokerViewMBean.isSlave();
            result = false;
        }};

        // WHEN
        boolean master = domibusJMSActiveMQBroker.isMaster();

        // THEN
        Assertions.assertTrue(master, "Should have seen this broker as true when the broker view MBean doesn't see it as a slave");
    }

    @Test
    public void isMaster_returnsFalseWhenBrokerViewMBeanReturnsTheSlaveFlagAsTrue() {
        // GIVEN
        new Expectations() {{
            brokerViewMBean.isSlave();
            result = true;
        }};

        // WHEN
        boolean master = domibusJMSActiveMQBroker.isMaster();

        // THEN
        Assertions.assertFalse(master, "Should have seen this broker as false when the broker view MBean sees it as a slave");
    }

    @Test
    public void getQueueViewMBean_retrieveByQueueName(@Injectable QueueViewMBean queueViewMBean, @Injectable ObjectName objectName) {
        // GIVEN
        final String queueName = "queueName";
        HashMap<String, ObjectName> queueMap = new HashMap<>();
        queueMap.put(queueName, objectName);
        new MockUp<DomibusJMSActiveMQBroker>() {
            @Mock
            QueueViewMBean getQueueViewMBean(ObjectName objectName) {
                return queueViewMBean;
            }

            @Mock
            Map<String, ObjectName> getQueueMap() {
                return queueMap;
            }
        };

        // WHEN
        QueueViewMBean result = domibusJMSActiveMQBroker.getQueueViewMBean(queueName);

        // THEN
        Assertions.assertSame(queueViewMBean, result, "Should have returned the correct MBean from the queue map when retrieving it by its queue name");
    }

    @Test
    public void getQueueViewMBean_retrieveByObjectName(@Mocked MBeanServerInvocationHandler mBeanServerInvocationHandler, @Injectable QueueViewMBean queueViewMBean, @Injectable ObjectName objectName) {
        // GIVEN
        new Expectations() {{
            MBeanServerInvocationHandler.newProxyInstance(mBeanServerConnection, objectName, QueueViewMBean.class, true);
            result = queueViewMBean;
        }};

        // WHEN
        QueueViewMBean result = domibusJMSActiveMQBroker.getQueueViewMBean(objectName);

        // THEN
        Assertions.assertSame(queueViewMBean, result, "Should have returned the correct MBean from the queue map when retrieving it by its object name");
    }

    @Test
    public void getQueueMap_returnsExistingQueueMapWhenAlreadyPopulated(@Injectable ObjectName objectName) throws Exception {
        // GIVEN
        Map<String, ObjectName> queueMap = getQueueMap();
        queueMap.put("queueName", objectName);

        // WHEN
        domibusJMSActiveMQBroker.getQueueMap();

        // THEN
        new Verifications() {{
            brokerViewMBean.getQueues();
            times = 0;
        }};
    }

    @Test
    public void getQueueMap_lazyInitializesQueueMap(@Injectable ObjectName objectName,
                                                    @Injectable QueueViewMBean queueViewMBean) throws Exception {
        // GIVEN
        String queueName = "queueName";
        Map<String, ObjectName> queueMap = getQueueMap();
        Assertions.assertTrue(queueMap.isEmpty(), "Should have had the queue map not already populated");
        new MockUp<DomibusJMSActiveMQBroker>() {
            @Mock
            QueueViewMBean getQueueViewMBean(ObjectName objectName) {
                return queueViewMBean;
            }
        };
        new Expectations() {{
            brokerViewMBean.getQueues();
            result = new ObjectName[]{objectName};

            queueViewMBean.getName();
            result = queueName;
        }};

        // WHEN
        Map<String, ObjectName> result = domibusJMSActiveMQBroker.getQueueMap();

        // THEN
        new FullVerifications() {{
            Assertions.assertSame(queueMap, result, "Should have returned the same queue map");
            Assertions.assertSame(objectName, result.get(queueName), "Should have populated the queue map");
        }};
    }

    @Test
    public void refresh(@Injectable ObjectName objectName) throws Exception {
        // GIVEN
        Map<String, ObjectName> queueMap = getQueueMap();
        queueMap.put("queueViewMBeanName", objectName);
        new Expectations() {{
            mBeanServerConnections.getObject(serviceUrl);
            result = mBeanServerConnection;

            brokerViewMBeans.getObject(brokerName, serviceUrl);
            result = brokerViewMBean;
        }};

        // WHEN
        domibusJMSActiveMQBroker.refresh();

        // THEN
        Assertions.assertSame(brokerViewMBean, getBrokerViewMBean(), "Should have refreshed the broker view MBean");
        Assertions.assertSame(mBeanServerConnection, getMBeanServerConnection(), "Should have refreshed the server connection MBean");
        Assertions.assertTrue(queueMap.isEmpty(), "Should have had the queue map cleared");
    }

    private Map<String, ObjectName> getQueueMap() throws IllegalAccessException {
        return (Map<String, ObjectName>) FieldUtils.readField(domibusJMSActiveMQBroker, "queueMap", true);
    }

    private BrokerViewMBean getBrokerViewMBean() throws IllegalAccessException {
        return (BrokerViewMBean) FieldUtils.readField(domibusJMSActiveMQBroker, "brokerViewMBean", true);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IllegalAccessException {
        return (MBeanServerConnection) FieldUtils.readField(domibusJMSActiveMQBroker, "mBeanServerConnection", true);
    }

}
