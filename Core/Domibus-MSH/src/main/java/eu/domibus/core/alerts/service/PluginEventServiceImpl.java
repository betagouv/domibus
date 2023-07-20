package eu.domibus.core.alerts.service;

import eu.domibus.api.alerts.AlertEvent;
import eu.domibus.api.alerts.PluginEventService;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jms.Queue;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ALERT_ACTIVE;
import static eu.domibus.jms.spi.InternalJMSConstants.ALERT_MESSAGE_QUEUE;
import static eu.domibus.core.alerts.service.EventServiceImpl.MAX_DESCRIPTION_LENGTH;

/**
 * {@inheritDoc}
 *
 * @author François Gautier
 * @since 5.0
 */
@Service
public class PluginEventServiceImpl implements PluginEventService {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PluginEventServiceImpl.class);
    private static final String PLUGIN_EVENT_ADDED_TO_THE_QUEUE = "Plugin Event:[{}] added to the queue";

    private final JMSManager jmsManager;

    private final Queue alertMessageQueue;

    private final DomibusPropertyProvider domibusPropertyProvider;


    public PluginEventServiceImpl(JMSManager jmsManager, @Qualifier(ALERT_MESSAGE_QUEUE) Queue alertMessageQueue, DomibusPropertyProvider domibusPropertyProvider) {
        this.jmsManager = jmsManager;
        this.alertMessageQueue = alertMessageQueue;
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    public void enqueueMessageEvent(AlertEvent alertEvent) {
        boolean domibusAlertsActive = BooleanUtils.isTrue(domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_ACTIVE));
        if (!domibusAlertsActive) {
            LOG.debug("Domibus alerts inactive, exiting");
            return;
        }

        Event event = new Event(EventType.PLUGIN);
        for (Map.Entry<String, String> stringStringEntry : alertEvent.getProperties().entrySet()) {
            event.addStringKeyValue(stringStringEntry.getKey(), stringStringEntry.getValue());
        }
        if (alertEvent.getAlertLevel() != null) {
            event.addStringKeyValue(AlertServiceImpl.ALERT_LEVEL, alertEvent.getAlertLevel().name());
        }
        event.addStringKeyValue(AlertServiceImpl.ALERT_ACTIVE, BooleanUtils.toStringTrueFalse(true));
        if (StringUtils.isNotBlank(alertEvent.getName())) {
            event.addStringKeyValue(AlertServiceImpl.ALERT_NAME, alertEvent.getName());
        }
        if (StringUtils.isNotBlank(alertEvent.getEmailSubject())) {
            event.addStringKeyValue(AlertServiceImpl.ALERT_SUBJECT, alertEvent.getEmailSubject());
        }
        if (StringUtils.isNotBlank(alertEvent.getEmailBody())) {
            addDescription(alertEvent, event);
        }

        jmsManager.convertAndSendToQueue(event, alertMessageQueue, EventType.PLUGIN.getQueueSelector());
        LOG.debug(PLUGIN_EVENT_ADDED_TO_THE_QUEUE, event);
    }

    private void addDescription(AlertEvent alertEvent, Event event) {
        event.addStringKeyValue(AlertServiceImpl.ALERT_DESCRIPTION, StringUtils.truncate(alertEvent.getEmailBody(), MAX_DESCRIPTION_LENGTH));
        if (alertEvent.getEmailBody().length() > MAX_DESCRIPTION_LENGTH) {
            String description = alertEvent.getEmailBody();
            for (int increment = 1; increment * MAX_DESCRIPTION_LENGTH < description.length(); increment++) {
                int start = increment * MAX_DESCRIPTION_LENGTH;
                int end = start + MAX_DESCRIPTION_LENGTH;
                event.addStringKeyValue(AlertServiceImpl.ALERT_DESCRIPTION + "_" + increment, description.substring(start, Math.min(description.length(), end)));
            }
        }
    }
}
