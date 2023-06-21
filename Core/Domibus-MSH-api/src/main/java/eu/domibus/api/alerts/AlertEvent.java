package eu.domibus.api.alerts;

import eu.domibus.core.alerts.model.common.EventType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static eu.domibus.core.alerts.model.common.EventType.PLUGIN;

/**
 * @author François Gautier
 * @since 5.0
 */
public class AlertEvent {
    private AlertLevel alertLevel;
    private String name;
    private String emailSubject;
    private String emailBody;
    private EventType eventType = PLUGIN;
    private Map<String, String> properties = new HashMap<>(); //NOSONAR

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    void addProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
