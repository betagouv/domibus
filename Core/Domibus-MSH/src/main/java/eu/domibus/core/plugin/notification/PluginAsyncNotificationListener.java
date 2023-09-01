
package eu.domibus.core.plugin.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.common.MessageEvent;
import eu.domibus.common.NotificationType;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.notification.AsyncNotificationConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * JMS listener responsible for sending async notifications to plugins
 *
 * @author Christian Koch, Stefan Mueller
 * @author Cosmin Baciu
 */
public class PluginAsyncNotificationListener implements MessageListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginAsyncNotificationListener.class);

    protected final AuthUtils authUtils;
    protected final DomainContextProvider domainContextProvider;
    protected final AsyncNotificationConfiguration asyncNotificationConfiguration;
    protected final PluginEventNotifierProvider pluginEventNotifierProvider;
    protected final ObjectMapper objectMapper;

    public PluginAsyncNotificationListener(DomainContextProvider domainContextProvider,
                                           AsyncNotificationConfiguration asyncNotificationConfiguration,
                                           PluginEventNotifierProvider pluginEventNotifierProvider,
                                           AuthUtils authUtils, ObjectMapper objectMapper) {
        this.domainContextProvider = domainContextProvider;
        this.asyncNotificationConfiguration = asyncNotificationConfiguration;
        this.pluginEventNotifierProvider = pluginEventNotifierProvider;
        this.authUtils = authUtils;
        this.objectMapper = objectMapper;
    }

    @MDCKey(value = {DomibusLogger.MDC_MESSAGE_ID, DomibusLogger.MDC_MESSAGE_ROLE, DomibusLogger.MDC_MESSAGE_ENTITY_ID}, cleanOnStart = true)
    @Timer(clazz = PluginAsyncNotificationListener.class,value = "onMessage")
    @Counter(clazz = PluginAsyncNotificationListener.class,value = "onMessage")
    public void onMessage(final Message message) {
        authUtils.runWithSecurityContext(()-> doOnMessage(message),
                "notif", "notif", AuthRole.ROLE_ADMIN);
    }

    public void doOnMessage(final Message message) {
        try {
            final String messageId = message.getStringProperty(MessageConstants.MESSAGE_ID);
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);

            final String role = message.getStringProperty(MessageConstants.MSH_ROLE);
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ROLE, role);

            final String domainCode = message.getStringProperty(MessageConstants.DOMAIN);
            LOG.debug("Processing message ID [{}] for domain [{}]", messageId, domainCode);
            domainContextProvider.setCurrentDomainWithValidation(domainCode);

            final NotificationType notificationType = NotificationType.valueOf(message.getStringProperty(MessageConstants.NOTIFICATION_TYPE));

            LOG.info("Received message with messageId [{}] and notification type [{}]", messageId, notificationType);

            PluginEventNotifier pluginEventNotifier = pluginEventNotifierProvider.getPluginEventNotifier(notificationType);
            if (pluginEventNotifier == null) {
                LOG.warn("Could not get plugin event notifier for notification type [{}]", notificationType);
                return;
            }
            Map<String, String> messageProperties = getMessageProperties(message);


            // deserialize the message body into the correct MessageEvent instance
            String serializedBody = message.getStringProperty(AsyncNotificationConfiguration.BODY);
            String eventClass = message.getStringProperty(AsyncNotificationConfiguration.EVENT_CLASS);
            MessageEvent event = (MessageEvent) objectMapper.readValue(serializedBody, Class.forName(eventClass, true, this.getClass().getClassLoader()));

            LOG.info("Calling the plugin notifier for the event type [{}] with the following content: [{}]", eventClass, serializedBody);
            pluginEventNotifier.notifyPlugin(event, asyncNotificationConfiguration.getBackendConnector());
        } catch (JMSException jmsEx) {
            LOG.error("Error getting the property from JMS message", jmsEx);
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Error getting the property from JMS message", jmsEx.getCause());
        } catch (Exception ex) { //NOSONAR To catch every exceptions thrown by all plugins.
            LOG.error("Error occurred during the plugin notification process of the message", ex);
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Error occurred during the plugin notification process of the message", ex.getCause());
        }
    }

    protected Map<String, String> getMessageProperties(Message message) throws JMSException {
        Map<String, String> properties = new HashMap<>();
        Enumeration propertyNames = message.getPropertyNames();
        while (propertyNames.hasMoreElements()) {
            String propertyName = (String) propertyNames.nextElement();
            properties.put(propertyName, message.getStringProperty(propertyName));
        }
        return properties;
    }


}
