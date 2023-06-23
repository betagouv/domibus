package eu.domibus.core.clustering;

import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JndiDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import java.util.Optional;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class ControllerListenerConfigurationTest {

    @Tested
    ControllerListenerConfiguration controllerListenerConfiguration;

    @Test
    public void createDefaultMessageListenerContainer(@Injectable ConnectionFactory connectionFactory,
                                                      @Injectable Topic destination,
                                                      @Injectable ControllerListenerService messageListener,
                                                      @Injectable Optional<JndiDestinationResolver> internalDestinationResolver,
                                                      @Injectable DomibusPropertyProvider domibusPropertyProvider) {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_JMS_INTERNAL_COMMAND_CONCURENCY);
            result = "2-3";
        }};

        DefaultMessageListenerContainer defaultMessageListenerContainer = controllerListenerConfiguration.createDefaultMessageListenerContainer(connectionFactory, destination, messageListener, internalDestinationResolver, domibusPropertyProvider);
        Assertions.assertEquals(2, defaultMessageListenerContainer.getConcurrentConsumers());
        Assertions.assertEquals(3, defaultMessageListenerContainer.getMaxConcurrentConsumers());
        Assertions.assertEquals(destination, defaultMessageListenerContainer.getDestination());

    }
}
