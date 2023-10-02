package eu.domibus.core.alert;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.user.UserEntityBase;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.AbstractPropertyValue;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.model.service.EventProperties;
import eu.domibus.core.alerts.service.AlertDispatcherService;
import eu.domibus.core.alerts.service.EventServiceImpl;
import eu.domibus.core.user.ui.UserDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@SuppressWarnings("rawtypes")
class AlertEventsTestIT extends AbstractIT {

    @Autowired
    private EventServiceImpl eventService;

    @Autowired
    UserDao userDao;

    @Autowired
    MessageDaoTestUtil messageDaoTestUtil;

    static List<Alert> dispatchedAlerts = new ArrayList<>();

    @Configuration
    static class ContextConfiguration {

        @Primary
        @Bean
        public AlertDispatcherService alertDispatcherService() {
            return alert -> dispatchedAlerts.add(alert);
        }

    }

    @BeforeEach
    public void setUp() throws XmlProcessingException, IOException {
        dispatchedAlerts.clear();
        uploadPmode(SERVICE_PORT);
    }

    @Test
    void sendEventStartDateStopped() {
        EventType eventType = EventType.ARCHIVING_START_DATE_STOPPED;
        eventService.enqueueEvent(eventType, eventType.name(), new EventProperties());

        Assertions.assertEquals(1, dispatchedAlerts.size());
        Alert alert = dispatchedAlerts.get(0);
        Assertions.assertEquals(AlertType.ARCHIVING_START_DATE_STOPPED, alert.getAlertType());
        Assertions.assertEquals(1, alert.getEvents().size());
        Assertions.assertEquals(alert.getEvents().toArray(new Event[0])[0].getType(), eventType);
    }

    @Test
    void sendEventMessageNotFinal() {
        String messageId = "messageId";
        MessageStatus messageStatus = MessageStatus.RECEIVED;

        eventService.enqueueEvent(EventType.ARCHIVING_MESSAGES_NON_FINAL, messageId, new EventProperties(messageId, messageStatus.name()));

        Assertions.assertEquals(1, dispatchedAlerts.size());
        Alert alert = dispatchedAlerts.get(0);
        Assertions.assertEquals(AlertType.ARCHIVING_MESSAGES_NON_FINAL, alert.getAlertType());
        Assertions.assertEquals(1, alert.getEvents().size());
        Event event = alert.getEvents().toArray(new Event[0])[0];
        Assertions.assertEquals(EventType.ARCHIVING_MESSAGES_NON_FINAL, event.getType());
        Map<String, AbstractPropertyValue> properties = event.getProperties();
        Assertions.assertEquals(3, properties.size());
        Assertions.assertEquals(properties.get("OLD_STATUS").getValue(), messageStatus.name());
        Assertions.assertEquals(properties.get("MESSAGE_ID").getValue(), messageId);
        Assertions.assertEquals(properties.get("EVENT_IDENTIFIER").getValue(), messageId);
    }

    @Test
    void sendMessagingEvent() {
        String messageId = this.getClass().getName() + "msg-test-1";
        UserMessageLog uml = messageDaoTestUtil.createTestMessage(messageId);
        eventService.enqueueMessageStatusChangedEvent(messageId, MessageStatus.SEND_ENQUEUED, MessageStatus.SEND_FAILURE, MSHRole.SENDING);

        Assertions.assertEquals(1, dispatchedAlerts.size());
        Alert alert = dispatchedAlerts.get(0);
        Assertions.assertEquals(AlertType.MSG_STATUS_CHANGED, alert.getAlertType());
        Assertions.assertEquals(1, alert.getEvents().size());
        Event event = alert.getEvents().toArray(new Event[0])[0];
        Assertions.assertEquals(EventType.MSG_STATUS_CHANGED, event.getType());
        Map<String, AbstractPropertyValue> properties = event.getProperties();
        Assertions.assertEquals(4, properties.size());
        Assertions.assertEquals(properties.get("OLD_STATUS").getValue(), MessageStatus.SEND_ENQUEUED.name());
        Assertions.assertEquals(properties.get("NEW_STATUS").getValue(), MessageStatus.SEND_FAILURE.name());
        Assertions.assertEquals(properties.get("MESSAGE_ID").getValue(), messageId);

        messageDaoTestUtil.deleteMessages(Collections.singletonList(uml.getEntityId()));
    }

    @Test
    void sendMessagingEventStatusWithoutAlert() {
        String messageId = this.getClass().getName() + "msg-test-2";
        UserMessageLog uml = messageDaoTestUtil.createTestMessage(messageId);
        eventService.enqueueMessageStatusChangedEvent(messageId, MessageStatus.SEND_ENQUEUED, MessageStatus.ACKNOWLEDGED, MSHRole.SENDING);


        Assertions.assertEquals(0, dispatchedAlerts.size());

        messageDaoTestUtil.deleteMessages(Collections.singletonList(uml.getEntityId()));
    }

    @Test
    void sendRepetitiveAlert() {
        EventType eventType = EventType.PLUGIN_PASSWORD_EXPIRED;
        UserEntityBase user = userDao.listUsers().get(0);
        int maxPasswordAgeInDays = 10;

        eventService.enqueuePasswordExpirationEvent(eventType, user, maxPasswordAgeInDays);


        Assertions.assertEquals(1, dispatchedAlerts.size());
        Alert alert = dispatchedAlerts.get(0);
        Assertions.assertEquals(AlertType.PLUGIN_PASSWORD_EXPIRED, alert.getAlertType());
        Assertions.assertEquals(1, alert.getEvents().size());
        Assertions.assertEquals(alert.getEvents().toArray(new Event[0])[0].getType(), eventType);

        eventService.enqueuePasswordExpirationEvent(eventType, user, maxPasswordAgeInDays);

        Assertions.assertEquals(1, dispatchedAlerts.size());

        eventService.enqueuePasswordExpirationEvent(eventType, user, maxPasswordAgeInDays);

        Assertions.assertEquals(1, dispatchedAlerts.size());
    }

    @Test
    void sendFrequencyAlert() {
        String partitionName = "partitionName";

        eventService.enqueueEvent(EventType.PARTITION_CHECK, partitionName, new EventProperties(partitionName));

        Assertions.assertEquals(1, dispatchedAlerts.size());
        Alert alert = dispatchedAlerts.get(0);
        Assertions.assertEquals(AlertType.PARTITION_CHECK, alert.getAlertType());
        Assertions.assertEquals(1, alert.getEvents().size());
        Assertions.assertEquals(EventType.PARTITION_CHECK, alert.getEvents().toArray(new Event[0])[0].getType());

        eventService.enqueueEvent(EventType.PARTITION_CHECK, partitionName, new EventProperties(partitionName));

        Assertions.assertEquals(1, dispatchedAlerts.size());

        eventService.enqueueEvent(EventType.PARTITION_CHECK, partitionName, new EventProperties(partitionName));

        Assertions.assertEquals(1, dispatchedAlerts.size());
    }

    @Test
    void sendConnMonitorAlert() {
        MessageStatus oldStatus = MessageStatus.SEND_ENQUEUED;
        MessageStatus newStatus = MessageStatus.SEND_FAILURE;
        String fromParty = "fromParty";
        String toParty = "toParty";
        String messageId = "messageId";

        eventService.enqueueMonitoringEvent(messageId, MSHRole.RECEIVING, oldStatus, newStatus, fromParty, toParty);

        Assertions.assertEquals(1, dispatchedAlerts.size());
        Alert alert = dispatchedAlerts.get(0);
        Assertions.assertEquals(AlertType.CONNECTION_MONITORING_FAILED, alert.getAlertType());
        Assertions.assertEquals(1, alert.getEvents().size());
        Assertions.assertEquals(EventType.CONNECTION_MONITORING_FAILED, alert.getEvents().toArray(new Event[0])[0].getType());
    }

    @Test
    void sendConnMonitorAlertNoAlert() {
        MessageStatus oldStatus = MessageStatus.SEND_ENQUEUED;
        MessageStatus newStatus = MessageStatus.ACKNOWLEDGED;
        String fromParty = "fromParty";
        String toParty = "toParty";
        String messageId = "messageId";

        eventService.enqueueMonitoringEvent(messageId, MSHRole.SENDING, oldStatus, newStatus, fromParty, toParty);

        Assertions.assertEquals(0, dispatchedAlerts.size());
    }

}
