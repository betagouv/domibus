package eu.domibus.core.plugin.notification;

import eu.domibus.common.*;
import eu.domibus.core.plugin.delegate.BackendConnectorDelegate;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.BackendConnector;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Service
public class PluginMessageReceivedFailureNotifier implements PluginEventNotifier<MessageReceiveFailureEvent> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginMessageReceivedFailureNotifier.class);

    protected BackendConnectorDelegate backendConnectorDelegate;

    public PluginMessageReceivedFailureNotifier(BackendConnectorDelegate backendConnectorDelegate) {
        this.backendConnectorDelegate = backendConnectorDelegate;
    }

    @Override
    public boolean canHandle(NotificationType notificationType) {
        return NotificationType.MESSAGE_RECEIVED_FAILURE == notificationType;
    }

    @Override
    public void notifyPlugin(MessageReceiveFailureEvent messageEvent, BackendConnector<?, ?> backendConnector) {
        backendConnectorDelegate.messageReceiveFailed(backendConnector, messageEvent);
    }
}
