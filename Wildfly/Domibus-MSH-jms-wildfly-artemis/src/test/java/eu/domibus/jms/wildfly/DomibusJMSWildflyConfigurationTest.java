package eu.domibus.jms.wildfly;

import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.jms.spi.helper.PriorityJmsTemplate;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jmx.access.MBeanProxyFactoryBean;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import java.lang.management.ManagementFactory;

import static eu.domibus.jms.wildfly.DomibusJMSWildflyConfiguration.MQ_BROKER_NAME;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class DomibusJMSWildflyConfigurationTest {

    @Tested
    DomibusJMSWildflyConfiguration domibusJMSWildflyConfiguration;

    @Test
    public void mBeanServerConnectionFactoryBean(@Mocked ManagementFactory managementFactory) {
        domibusJMSWildflyConfiguration.mBeanServerConnectionFactoryBean();

        new Verifications() {{
            ManagementFactory.getPlatformMBeanServer();
        }};
    }

    @Test
    public void mBeanProxyFactoryBean(@Injectable MBeanServer mBeanServer,
                                      @Injectable DomibusPropertyProvider domibusPropertyProvider,
                                      @Mocked MBeanProxyFactoryBean mBeanProxyFactoryBean) throws MalformedObjectNameException {
        String artemisBroker = "localhost";
        String objectName = String.format(MQ_BROKER_NAME, artemisBroker);

        new Expectations() {{
            domibusPropertyProvider.getProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_ARTEMIS_BROKER);
            this.result = artemisBroker;
        }};

        domibusJMSWildflyConfiguration.mBeanProxyFactoryBean(mBeanServer, domibusPropertyProvider);

        new Verifications() {{
            mBeanProxyFactoryBean.setObjectName(objectName);
            mBeanProxyFactoryBean.setProxyInterface(ActiveMQServerControl.class);
            mBeanProxyFactoryBean.setServer(mBeanServer);
        }};
    }

    @Test
    public void jmsSender(@Injectable ConnectionFactory connectionFactory,
                          @Mocked PriorityJmsTemplate jmsTemplate) {
        domibusJMSWildflyConfiguration.jmsSender(connectionFactory);

        new Verifications() {{
            jmsTemplate.setSessionTransacted(true);
            jmsTemplate.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
            jmsTemplate.setConnectionFactory(connectionFactory);
        }};
    }
}
