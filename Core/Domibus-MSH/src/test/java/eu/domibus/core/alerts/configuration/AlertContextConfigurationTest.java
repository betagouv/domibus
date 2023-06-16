package eu.domibus.core.alerts.configuration;

import eu.domibus.api.property.DomibusPropertyChangeNotifier;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.configuration.common.AlertContextConfiguration;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.JndiDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import java.util.Optional;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class AlertContextConfigurationTest {


    @Tested
    AlertContextConfiguration alertContextConfiguration;

    @Mocked
    DefaultJmsListenerContainerFactory defaultJmsListenerContainerFactory;

    @Injectable
    DomibusPropertyChangeNotifier domibusPropertyChangeNotifier;

    @Injectable
    AlertConfigurationService alertConfigurationService;

    @Test
    public void alertJmsListenerContainerFactory(@Injectable ConnectionFactory connectionFactory,
                                                 @Injectable DomibusPropertyProvider domibusPropertyProvider,
                                                 @Injectable MappingJackson2MessageConverter jackson2MessageConverter,
                                                 @Injectable Optional<JndiDestinationResolver> internalDestinationResolver
                                                 ) {
        String concurrency = "2-3";

        new Expectations() {{
            domibusPropertyProvider.getProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_ALERT_QUEUE_CONCURRENCY);
            this.result = concurrency;
        }};


        alertContextConfiguration.alertJmsListenerContainerFactory(connectionFactory, domibusPropertyProvider, jackson2MessageConverter, internalDestinationResolver,null);

        new Verifications() {{
            MessageConverter messageConverter = null;
            defaultJmsListenerContainerFactory.setMessageConverter(messageConverter = withCapture());
            Assertions.assertEquals(messageConverter, jackson2MessageConverter);

            ConnectionFactory cf = null;
            defaultJmsListenerContainerFactory.setConnectionFactory(cf = withCapture());
            Assertions.assertEquals(connectionFactory, cf);

            String factoryConcurrency = null;
            defaultJmsListenerContainerFactory.setConcurrency(factoryConcurrency = withCapture());
            Assertions.assertEquals(factoryConcurrency, concurrency);

            defaultJmsListenerContainerFactory.setSessionTransacted(true);

            Integer sessionAckMode = null;
            defaultJmsListenerContainerFactory.setSessionAcknowledgeMode(sessionAckMode = withCapture());
            Assertions.assertEquals(Session.SESSION_TRANSACTED, sessionAckMode.intValue());
        }};
    }
}
