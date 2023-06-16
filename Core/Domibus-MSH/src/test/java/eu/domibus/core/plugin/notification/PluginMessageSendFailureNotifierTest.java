package eu.domibus.core.plugin.notification;

import eu.domibus.common.MessageSendFailedEvent;
import eu.domibus.common.NotificationType;
import eu.domibus.core.plugin.delegate.BackendConnectorDelegate;
import eu.domibus.plugin.BackendConnector;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
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
public class PluginMessageSendFailureNotifierTest {

    @Tested
    PluginMessageSendFailureNotifier pluginMessageSendFailureNotifier;

    @Injectable
    protected BackendConnectorDelegate backendConnectorDelegate;

    @Test
    public void canHandle() {
        assertTrue(pluginMessageSendFailureNotifier.canHandle(NotificationType.MESSAGE_SEND_FAILURE));
    }

    @Test
    public void notifyPlugin(@Injectable BackendConnector backendConnector, @Injectable MessageSendFailedEvent messageSendFailedEvent) {
        String messageId = "123";
        Map<String, String> properties = new HashMap<>();


        pluginMessageSendFailureNotifier.notifyPlugin(messageSendFailedEvent, backendConnector);

        new Verifications() {{
            MessageSendFailedEvent event = null;
            backendConnectorDelegate.messageSendFailed(backendConnector, event = withCapture());
//            assertEquals(messageId, event.getMessageId());
        }};
    }
}
