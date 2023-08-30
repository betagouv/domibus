package eu.domibus.jms.activemq;

import eu.domibus.jms.spi.helper.PriorityJmsTemplate;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jmx.access.MBeanProxyFactoryBean;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.management.MBeanServerConnection;

import static eu.domibus.jms.activemq.DomibusJMSActiveMQConfiguration.MQ_BROKER_NAME;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DomibusJMSActiveMQConfigurationTest {

    @Tested
    private DomibusJMSActiveMQConfiguration domibusJMSActiveMQConfiguration;

    @Test
    public void mBeanServerConnections(@Injectable MBeanServerConnection mBeanServerConnection,
                                       @Mocked MBeanServerConnectionFactoryBean mBeanServerConnectionFactoryBean) throws Exception {
        // GIVEN
        String serviceUrl = "service:jmx:rmi:///jndi/rmi://localhost:123/jmxrmi";

        new Expectations() {{
            mBeanServerConnectionFactoryBean.getObject();
            result = mBeanServerConnection;
        }};

        // WHEN
        MBeanServerConnection result = domibusJMSActiveMQConfiguration.mBeanServerConnections(serviceUrl);

        // THEN
        new Verifications() {{
            mBeanServerConnectionFactoryBean.setServiceUrl(serviceUrl);
            mBeanServerConnectionFactoryBean.setConnectOnStartup(false);
            mBeanServerConnectionFactoryBean.afterPropertiesSet();
            Assertions.assertEquals(mBeanServerConnection, result);
        }};
    }

    @Test
    public void mBeanProxyFactoryBean(@Injectable MBeanServerConnection server,
                                      @Injectable BrokerViewMBean brokerViewMBean,
                                      @Mocked MBeanProxyFactoryBean mBeanProxyFactoryBean) throws Exception {
        // GIVEN
        final String brokerName = "localhost";
        final String serviceUrl = "service:jmx:rmi:///jndi/rmi://localhost:123/jmxrmi";

        new Expectations() {{
            mBeanProxyFactoryBean.getObject();
            result = brokerViewMBean;
        }};

        // WHEN
        BrokerViewMBean result = domibusJMSActiveMQConfiguration.mBeanProxyFactoryBeans(brokerName, serviceUrl);

        // THEN
        new Verifications() {{
            mBeanProxyFactoryBean.setObjectName(MQ_BROKER_NAME + brokerName);
            mBeanProxyFactoryBean.setProxyInterface(BrokerViewMBean.class);
            mBeanProxyFactoryBean.setServiceUrl(serviceUrl);
            mBeanProxyFactoryBean.afterPropertiesSet();
            Assertions.assertEquals(brokerViewMBean, result);
        }};
    }

    @Test
    public void jmsSender(@Injectable ConnectionFactory connectionFactory,
                          @Mocked PriorityJmsTemplate jmsTemplate) {
        domibusJMSActiveMQConfiguration.jmsSender(connectionFactory);

        new Verifications() {{
            jmsTemplate.setSessionTransacted(true);
            jmsTemplate.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
            jmsTemplate.setConnectionFactory(connectionFactory);
        }};
    }
}
