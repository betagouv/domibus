package eu.domibus.core.plugin.notification;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.plugin.notification.AsyncNotificationConfiguration;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PluginAsyncNotificationJMSConfigurerTest {

    PluginAsyncNotificationJMSConfigurer pluginAsyncNotificationJMSConfigurer;

    @Injectable
    ObjectProvider<PluginAsyncNotificationListener> asyncNotificationListenerProvider;

    @Injectable
    protected JmsListenerContainerFactory internalJmsListenerContainerFactory;


    @BeforeEach
    void setUp() {
        pluginAsyncNotificationJMSConfigurer = new PluginAsyncNotificationJMSConfigurer(internalJmsListenerContainerFactory, asyncNotificationListenerProvider, null);
    }

    @Test
    public void configureJmsListenersWithNoAsyncPlugins(@Injectable JmsListenerEndpointRegistrar registrar)  {
        pluginAsyncNotificationJMSConfigurer.asyncNotificationConfigurations = null;

        new Expectations(pluginAsyncNotificationJMSConfigurer) {{
            pluginAsyncNotificationJMSConfigurer.initializeAsyncNotificationLister(registrar, (AsyncNotificationConfiguration) any);
            times = 0;
        }};

        pluginAsyncNotificationJMSConfigurer.configureJmsListeners(registrar);

        new FullVerifications() {{
        }};
    }
    
    @Test
    public void initializeAsyncNotificationLister(@Injectable JmsListenerEndpointRegistrar registrar,
                                                  @Injectable AsyncNotificationConfiguration asyncNotificationConfiguration,
                                                  @Injectable BackendConnector backendConnector,
                                                  @Injectable Queue queue,
                                                  @Injectable SimpleJmsListenerEndpoint endpoint) {
        new Expectations(pluginAsyncNotificationJMSConfigurer) {{
            asyncNotificationConfiguration.getBackendConnector();
            result = backendConnector;

            asyncNotificationConfiguration.getBackendNotificationQueue();
            result = queue;

            pluginAsyncNotificationJMSConfigurer.createJMSListener(asyncNotificationConfiguration);
            result = endpoint;
        }};

        pluginAsyncNotificationJMSConfigurer.initializeAsyncNotificationLister(registrar, asyncNotificationConfiguration);

        new Verifications() {{
            registrar.registerEndpoint(endpoint, internalJmsListenerContainerFactory);
        }};
    }

    @Test
    public void createJMSListener(@Injectable AsyncNotificationConfiguration asyncNotificationListener,
                                  @Injectable BackendConnector backendConnector,
                                  @Injectable PluginAsyncNotificationListener pluginAsyncNotificationListener) throws JMSException {
        String queueName = "myqueue";

        new Expectations() {{
            asyncNotificationListener.getBackendConnector();
            result = backendConnector;

            asyncNotificationListenerProvider.getObject(asyncNotificationListener);
            result = pluginAsyncNotificationListener;

            asyncNotificationListener.getQueueName();
            result = queueName;
        }};

        SimpleJmsListenerEndpoint jmsListener = pluginAsyncNotificationJMSConfigurer.createJMSListener(asyncNotificationListener);
        Assertions.assertEquals(jmsListener.getMessageListener(), pluginAsyncNotificationListener);
        Assertions.assertEquals(jmsListener.getDestination(), queueName);
    }
}
