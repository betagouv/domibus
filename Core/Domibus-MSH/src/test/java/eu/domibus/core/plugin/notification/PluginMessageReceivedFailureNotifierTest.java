package eu.domibus.core.plugin.notification;

import eu.domibus.common.MessageEvent;
import eu.domibus.common.MessageReceiveFailureEvent;
import eu.domibus.common.NotificationType;
import eu.domibus.core.plugin.delegate.BackendConnectorDelegate;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.BackendConnector;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PluginMessageReceivedFailureNotifierTest {

    @Tested
    PluginMessageReceivedFailureNotifier pluginMessageReceivedFailureNotifier;

    @Injectable
    protected BackendConnectorDelegate backendConnectorDelegate;

    @Test
    public void canHandle() {
        assertTrue(pluginMessageReceivedFailureNotifier.canHandle(NotificationType.MESSAGE_RECEIVED_FAILURE));
    }

    @Test
    public void notifyPlugin(@Injectable BackendConnector backendConnector, @Injectable MessageReceiveFailureEvent messageReceiveFailureEvent) {
        String messageId = "123";
        Map<String, String> properties = new HashMap<>();

        String service = "myservice";
        String endpoint = "myendpoint";
        String action = "myaction";
        String serviceType = "servicetype";
        properties.put(MessageConstants.SERVICE, service);
        properties.put(MessageConstants.SERVICE_TYPE, serviceType);
        properties.put(MessageConstants.ACTION, action);
        properties.put(MessageConstants.ENDPOINT, endpoint);

        pluginMessageReceivedFailureNotifier.notifyPlugin(messageReceiveFailureEvent, backendConnector);

        new Verifications() {{
            MessageReceiveFailureEvent event = null;
            backendConnectorDelegate.messageReceiveFailed(backendConnector, event = withCapture());
//            assertEquals(service, event.getService());
//            assertEquals(serviceType, event.getServiceType());
//            assertEquals(action, event.getAction());
        }};
    }
}
