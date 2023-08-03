package eu.domibus.core.alerts.service;

import eu.domibus.api.alerts.AlertEvent;
import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.Event;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import mockit.integration.junit5.JMockitExtension;
import javax.jms.Queue;
import java.util.HashMap;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ALERT_ACTIVE;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class PluginEventServiceImplTest {


    public static final String ALERT_NAME = "AlertName";
    public static final String EMAIL_SUBJECT = "EmailSubject";
    public static final String EMAIL_BODY_300_LONG = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
            "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    @Tested
    private PluginEventServiceImpl eventService;

    @Injectable
    private JMSManager jmsManager;

    @Injectable
    private Queue alertMessageQueue;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void enqueueMessageEvent_empty(@Injectable AlertEvent alertEvent) {
        new Expectations() {{
            alertEvent.getAlertLevel();
            result = null;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_ACTIVE);
            result = true;
        }};

        eventService.enqueueMessageEvent(alertEvent);

        new Verifications() {{
            Event event;
            jmsManager.convertAndSendToQueue(event = withCapture(), alertMessageQueue, EventType.PLUGIN.getQueueSelector());
            times = 1;
            Assertions.assertEquals(EventType.PLUGIN, event.getType());
        }};
    }

    @Test
    public void enqueueMessageEvent_full(@Injectable AlertEvent alertEvent) {
        Map<String, String> props = new HashMap<>();
        props.put("Test", "Test");
        new Expectations() {{
            alertEvent.getProperties();
            result = props;

            alertEvent.getAlertLevel();
            result = AlertLevel.MEDIUM;

            alertEvent.getEmailBody();
            result = EMAIL_BODY_300_LONG;

            alertEvent.getName();
            result = "AlertName";

            alertEvent.getEmailSubject();
            result = "EmailSubject";
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_ACTIVE);
            result = true;
        }};

        eventService.enqueueMessageEvent(alertEvent);

        new Verifications() {{
            Event event;
            jmsManager.convertAndSendToQueue(event = withCapture(), alertMessageQueue, EventType.PLUGIN.getQueueSelector());
            times = 1;
            Assertions.assertEquals(EventType.PLUGIN, event.getType());
            Assertions.assertEquals(AlertLevel.MEDIUM.name(), event.getProperties().get(AlertServiceImpl.ALERT_LEVEL).toString());
            Assertions.assertEquals(ALERT_NAME, event.getProperties().get(AlertServiceImpl.ALERT_NAME).toString());
            Assertions.assertEquals(Boolean.TRUE.toString(), event.getProperties().get(AlertServiceImpl.ALERT_ACTIVE).toString());
            Assertions.assertEquals(EMAIL_SUBJECT, event.getProperties().get(AlertServiceImpl.ALERT_SUBJECT).toString());
            Assertions.assertEquals(StringUtils.substring(EMAIL_BODY_300_LONG, 0, 255), event.getProperties().get(AlertServiceImpl.ALERT_DESCRIPTION).toString());
            Assertions.assertEquals(StringUtils.substring(EMAIL_BODY_300_LONG, 255), event.getProperties().get(AlertServiceImpl.ALERT_DESCRIPTION + "_1").toString());
        }};
    }
}