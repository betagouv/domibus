package eu.domibus.core.plugin.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.functions.AuthenticatedProcedure;
import eu.domibus.common.DeliverMessageEvent;
import eu.domibus.common.NotificationType;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.notification.AsyncNotificationConfiguration;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.config.JmsListenerContainerFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Arun Venugopal
 * @author Federico Martini
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class PluginAsyncNotificationListenerTest {

    @Tested
    PluginAsyncNotificationListener pluginAsyncNotificationListener;

    @Injectable
    ObjectMapper objectMapper;

    @Injectable
    protected JmsListenerContainerFactory internalJmsListenerContainerFactory;

    @Injectable
    protected AuthUtils authUtils;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    protected AsyncNotificationConfiguration notificationListenerService;

    @Injectable
    protected PluginEventNotifierProvider pluginEventNotifierProvider;


    @Test
    public void onMessage(@Injectable Message message,
                          @Injectable PluginEventNotifier pluginEventNotifier,
                          @Injectable DeliverMessageEvent deliverMessageEvent) throws JMSException {
        String messageId = "123";
        NotificationType notificationType = NotificationType.MESSAGE_FRAGMENT_RECEIVED;

        new Expectations(pluginAsyncNotificationListener) {{
            message.getStringProperty(MessageConstants.MESSAGE_ID);
            result = messageId;

            message.getStringProperty(MessageConstants.NOTIFICATION_TYPE);
            result = notificationType.toString();

            message.getStringProperty(AsyncNotificationConfiguration.BODY);
            result = "{}";
            message.getStringProperty(AsyncNotificationConfiguration.EVENT_CLASS);
            result = DeliverMessageEvent.class.getName();

            pluginEventNotifierProvider.getPluginEventNotifier(notificationType);
            result = pluginEventNotifier;

            pluginAsyncNotificationListener.getMessageProperties(message);
            result = new HashMap<>();
        }};

        pluginAsyncNotificationListener.doOnMessage(message);

        new Verifications() {{
            pluginEventNotifier.notifyPlugin(deliverMessageEvent, notificationListenerService.getBackendConnector());
            times = 1;
        }};
    }

    @Test
    public void onMessage_addsAuthentication(@Injectable Message message,
                                             @Injectable PluginEventNotifier pluginEventNotifier){
        // Given
        new Expectations() {{
            authUtils.runWithSecurityContext((AuthenticatedProcedure)any, anyString, anyString, (AuthRole)any);
        }};

        // When
        pluginAsyncNotificationListener.onMessage(message);

        // Then
        new FullVerifications() {{
            AuthenticatedProcedure function;
            String username;
            String password;
            AuthRole role;
            authUtils.runWithSecurityContext(function = withCapture(),
                    username=withCapture(), password=withCapture(), role=withCapture());
            Assertions.assertNotNull(function);
            Assertions.assertEquals("notif",username);
            Assertions.assertEquals("notif",password);
            Assertions.assertEquals(AuthRole.ROLE_ADMIN,role);

        }};
    }
}
