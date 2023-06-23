package eu.domibus.core.plugin.delegate;

import eu.domibus.api.util.ClassUtil;
import eu.domibus.common.MessageDeletedBatchEvent;
import eu.domibus.common.MessageReceiveFailureEvent;
import eu.domibus.core.plugin.BackendConnectorHelper;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.core.plugin.notification.AsyncNotificationConfigurationService;
import eu.domibus.core.plugin.routing.RoutingService;
import eu.domibus.plugin.BackendConnector;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 3.2.2
 */
@ExtendWith(JMockitExtension.class)
public class DefaultBackendConnectorDelegateTest {

    @Injectable
    ClassUtil classUtil;

    @Injectable
    protected RoutingService routingService;

    @Injectable
    protected BackendConnectorProvider backendConnectorProvider;

    @Injectable
    protected BackendConnectorHelper backendConnectorHelper;

    @Injectable
    protected AsyncNotificationConfigurationService asyncNotificationConfigurationService;

    @Tested
    DefaultBackendConnectorDelegate defaultBackendConnectorDelegate;

    @Test
    public void testMessageReceive(@Injectable final BackendConnector backendConnector,
                                   @Injectable final MessageReceiveFailureEvent event) throws Exception {
        defaultBackendConnectorDelegate.messageReceiveFailed(backendConnector, event);

        new Verifications() {{
            backendConnector.messageReceiveFailed(event);
        }};
    }

    @Test
    public void messageDeletedBatchEvent(@Injectable MessageDeletedBatchEvent event,
                                         @Injectable BackendConnector backendConnector) {
        String backend = "mybackend";

        new Expectations(defaultBackendConnectorDelegate) {{
            backendConnectorProvider.getBackendConnector(backend);
            result = backendConnector;
        }};

        defaultBackendConnectorDelegate.messageDeletedBatchEvent(backend, event);

        new Verifications() {{
            backendConnector.messageDeletedBatchEvent(event);
        }};
    }
}
