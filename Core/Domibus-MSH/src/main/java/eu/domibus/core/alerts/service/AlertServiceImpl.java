package eu.domibus.core.alerts.service;

import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.api.jms.JMSManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.server.ServerInfoService;
import eu.domibus.api.util.DateUtil;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.configuration.common.AlertModuleConfiguration;
import eu.domibus.core.alerts.configuration.global.CommonConfigurationManager;
import eu.domibus.core.alerts.dao.AlertDao;
import eu.domibus.core.alerts.dao.EventDao;
import eu.domibus.core.alerts.model.common.AlertCriteria;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.persist.AbstractEventProperty;
import eu.domibus.core.alerts.model.persist.Alert;
import eu.domibus.core.alerts.model.persist.Event;
import eu.domibus.core.alerts.model.service.DefaultMailModel;
import eu.domibus.core.alerts.model.service.MailModel;
import eu.domibus.core.converter.AlertCoreMapper;
import eu.domibus.core.scheduler.ReprogrammableService;
import eu.domibus.jms.spi.InternalJMSConstants;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Queue;
import java.time.ZoneOffset;
import java.util.*;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ALERT_RETRY_TIME;
import static eu.domibus.core.alerts.model.common.AlertStatus.*;
import static eu.domibus.core.alerts.configuration.common.AlertConfigurationServiceImpl.DOMIBUS_ALERT_SUPER_INSTANCE_NAME_SUBJECT;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(AlertServiceImpl.class);

    static final String ALERT_LEVEL = "ALERT_LEVEL";
    static final String ALERT_SUBJECT = "ALERT_SUBJECT";
    static final String ALERT_ACTIVE = "ALERT_ACTIVE";
    static final String ALERT_NAME = "ALERT_NAME";

    static final String REPORTING_TIME = "REPORTING_TIME";

    /**
     * server name on which Domibus is running
     */
    static final String SERVER_NAME = "SERVER_NAME";

    public static final String DESCRIPTION = "DESCRIPTION";

    public static final String ALERT_SELECTOR = "alert";

    public static final String ALERT_DESCRIPTION = "ALERT_DESCRIPTION";

    private final EventDao eventDao;

    private final AlertDao alertDao;

    private final DomibusPropertyProvider domibusPropertyProvider;

    private final AlertCoreMapper alertCoreMapper;

    private final JMSManager jmsManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final Queue alertMessageQueue;

    private final AlertConfigurationService alertConfigurationService;

    private final ServerInfoService serverInfoService;

    private final CommonConfigurationManager alertConfigurationManager;

    private final ReprogrammableService reprogrammableService;

    public AlertServiceImpl(EventDao eventDao,
                            AlertDao alertDao,
                            DomibusPropertyProvider domibusPropertyProvider,
                            AlertCoreMapper alertCoreMapper,
                            JMSManager jmsManager,
                            @Qualifier(InternalJMSConstants.ALERT_MESSAGE_QUEUE) Queue alertMessageQueue,
                            AlertConfigurationService alertConfigurationService,
                            ServerInfoService serverInfoService,
                            CommonConfigurationManager alertConfigurationManager,
                            ReprogrammableService reprogrammableService) {
        this.eventDao = eventDao;
        this.alertDao = alertDao;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.alertCoreMapper = alertCoreMapper;
        this.jmsManager = jmsManager;
        this.alertMessageQueue = alertMessageQueue;
        this.alertConfigurationService = alertConfigurationService;
        this.serverInfoService = serverInfoService;
        this.alertConfigurationManager = alertConfigurationManager;
        this.reprogrammableService = reprogrammableService;
    }

    protected eu.domibus.core.alerts.model.service.Alert createAlertOnEvent(eu.domibus.core.alerts.model.service.Event event) {
        AlertModuleConfiguration moduleConfiguration = alertConfigurationService.getConfiguration(AlertType.getByEventType(event.getType()));
        return createAlert(event, moduleConfiguration.getAlertLevel(event), moduleConfiguration.isActive());
    }

    private eu.domibus.core.alerts.model.service.Alert createAlert(eu.domibus.core.alerts.model.service.Event event, AlertLevel alertLevel, boolean active) {
        final Event eventEntity = readEvent(event);
        AlertType alertType = AlertType.getByEventType(event.getType());
        if (!active) {
            LOG.debug("Alerts of type [{}] are currently disabled", alertType);
            return null;
        }
        if (alertLevel == null) {
            LOG.debug("Alert of type [{}] currently disabled for this event: [{}]", alertType, event);
            return null;
        }

        Alert alert = new Alert();
        alert.addEvent(eventEntity);
        alert.setAlertType(alertType);
        alert.setAttempts(0);
        alert.setMaxAttempts(domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_RETRY_MAX_ATTEMPTS));
        alert.setAlertStatus(SEND_ENQUEUED);
        alert.setCreationTime(new Date());
        alert.setAlertLevel(alertLevel);
        alertDao.create(alert);
        LOG.info("New alert saved: [{}]", alert);
        return alertCoreMapper.alertPersistToAlertService(alert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void createAndEnqueueAlertOnPluginEvent(eu.domibus.core.alerts.model.service.Event event) {

        AlertLevel alertLevel = event.findOptionalProperty(ALERT_LEVEL)
                .map(AlertLevel::valueOf)
                .orElse(null);
        String alertName = event.findOptionalProperty(ALERT_NAME)
                .orElse(null);
        boolean active = BooleanUtils.toBoolean(event.findOptionalProperty(ALERT_ACTIVE).orElse(null));

        eu.domibus.core.alerts.model.service.Alert alert = createAlert(event, alertLevel, active);
        LOG.debug("Alert [{}] created and queued", alertName);
        enqueueAlert(alert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enqueueAlert(eu.domibus.core.alerts.model.service.Alert alert) {
        if (alert == null) {
            LOG.info("No alert enqueued because of a missing alert parameter");
            return;
        }
        jmsManager.convertAndSendToQueue(alert, alertMessageQueue, ALERT_SELECTOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MailModel<Map<String, String>> getMailModelForAlert(eu.domibus.core.alerts.model.service.Alert alert) {
        final Alert alertEntity = readAlert(alert);
        alertEntity.setReportingTime(new Date());
        Map<String, String> mailModel = new HashMap<>();
        final Event next = alertEntity.getEvents().iterator().next();
        next.getProperties().forEach((key, value) -> mailModel.put(key, StringEscapeUtils.escapeHtml4(value.getValue().toString())));
        mailModel.put(ALERT_LEVEL, alertEntity.getAlertLevel().name());
        mailModel.put(REPORTING_TIME, DateUtil.DEFAULT_FORMATTER.withZone(ZoneOffset.UTC).format(alertEntity.getReportingTime().toInstant()));
        mailModel.put(DESCRIPTION, getDescription(alertEntity, next));
        mailModel.put(SERVER_NAME, serverInfoService.getServerName());

        if (LOG.isDebugEnabled()) {
            mailModel.forEach((key, value) -> LOG.debug("Mail template key[{}] value[{}]", key, value));
        }
        final AlertType alertType = alertEntity.getAlertType();
        String subject = getSubject(alertType, next);
        final String template = alertType.getTemplate();
        return new DefaultMailModel<>(mailModel, template, subject);
    }

    protected String getDescription(Alert alertEntity, Event event) {
        StringBuilder result = new StringBuilder();
        result.append("[").append(alertEntity.getAlertType().getTitle()).append("] ");
        result.append(getSafeString(event, ALERT_DESCRIPTION));
        long descriptionNumber = event.getProperties().keySet().stream()
                .filter(key -> startsWithIgnoreCase(key, ALERT_DESCRIPTION))
                .count();
        for (int i = 1; i < descriptionNumber; i++) {
            result.append(getSafeString(event, ALERT_DESCRIPTION + "_" + i));
        }
        return result.toString();
    }

    protected String getSafeString(Event event, String key) {
        AbstractEventProperty<?> abstractEventProperty = event.getProperties().get(key);
        if (abstractEventProperty == null) {
            return EMPTY;
        }
        return abstractEventProperty.getValue().toString();
    }

    protected String getSubject(AlertType alertType, Event next) {
        String subject = null;
        AbstractEventProperty<?> alertSubject = next.getProperties().get(ALERT_SUBJECT);
        if (alertSubject != null) {
            subject = alertSubject.getValue().toString();
        }
        if (StringUtils.isBlank(subject)) {
            subject = alertConfigurationService.getMailSubject(alertType);
        }

        //always set at super level
        final String serverName = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SUPER_INSTANCE_NAME_SUBJECT);
        subject += "[" + serverName + "]";
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void handleAlertStatus(eu.domibus.core.alerts.model.service.Alert alert) {
        final Alert alertEntity = readAlert(alert);
        if (alertEntity == null) {
            LOG.error("Alert[{}]: not found", alert.getEntityId());
            return;
        }
        alertEntity.setAlertStatus(alert.getAlertStatus());
        reprogrammableService.removeRescheduleInfo(alertEntity);
        if (SUCCESS == alertEntity.getAlertStatus()) {
            alertEntity.setReportingTime(new Date());
            alertEntity.setAttempts(alertEntity.getAttempts() + 1);
            LOG.debug("Alert[{}]: send successfully", alert.getEntityId());
            return;
        }
        final Integer attempts = alertEntity.getAttempts() + 1;
        final Integer maxAttempts = alertEntity.getMaxAttempts();
        LOG.debug("Alert[{}]: send unsuccessfully", alert.getEntityId());
        if (attempts < maxAttempts) {
            LOG.debug("Alert[{}]: send attempts[{}], max attempts[{}]", alert.getEntityId(), attempts, maxAttempts);
            final Integer minutesBetweenAttempt = domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_RETRY_TIME);
            final Date nextAttempt = Date.from(java.time.ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(minutesBetweenAttempt).toInstant());
            reprogrammableService.setRescheduleInfo(alertEntity, nextAttempt);

            alertEntity.setAttempts(attempts);
            alertEntity.setAlertStatus(RETRY);
        }

        if (FAILED == alertEntity.getAlertStatus()) {
            alertEntity.setReportingTimeFailure(Date.from(java.time.ZonedDateTime.now(ZoneOffset.UTC).toInstant()));
            alertEntity.setAttempts(alertEntity.getMaxAttempts());
        }
        LOG.debug("Alert[{}]: change status to:[{}]", alert.getEntityId(), alertEntity.getAlertStatus());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retrieveAndResendFailedAlerts() {
        final List<Alert> retryAlerts = alertDao.findRetryAlerts();
        retryAlerts.forEach(this::convertAndEnqueue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<eu.domibus.core.alerts.model.service.Alert> findAlerts(AlertCriteria alertCriteria) {
        final List<Alert> alerts = alertDao.filterAlerts(alertCriteria);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Find alerts:");
            alerts.forEach(alert -> {
                LOG.debug("Alert[{}]", alert);
                alert.getEvents().forEach(event -> {
                    LOG.debug("Event[{}]", event);
                    event.getProperties().
                            forEach((key, value) -> LOG.debug("Event property:[{}]->[{}]", key, value));
                });
            });

        }
        return alertCoreMapper.alertPersistListToAlertServiceList(alerts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Long countAlerts(AlertCriteria alertCriteria) {
        return alertDao.countAlerts(alertCriteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanAlerts() {
        final Integer alertLifeTimeInDays = alertConfigurationManager.getConfiguration().getAlertLifeTimeInDays();
        final Date alertLimitDate = Date.from(java.time.ZonedDateTime.now(ZoneOffset.UTC).minusDays(alertLifeTimeInDays).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant());
        LOG.debug("Cleaning alerts with creation time < [{}]", alertLimitDate);
        alertDao.deleteAlerts(alertLimitDate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateAlertProcessed(List<eu.domibus.core.alerts.model.service.Alert> alerts) {
        alerts.forEach(alert -> {
            final long entityId = alert.getEntityId();
            final boolean processed = alert.isProcessed();
            LOG.debug("Update alert with id[{}] set processed to[{}]", entityId, processed);
            alertDao.updateAlertProcessed(entityId, processed);
        });

    }

    private void convertAndEnqueue(Alert alert) {
        LOG.debug("Preparing alert for retry [{}]", alert);
        final eu.domibus.core.alerts.model.service.Alert convert = alertCoreMapper.alertPersistToAlertService(alert);
        enqueueAlert(convert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAlerts(List<eu.domibus.core.alerts.model.service.Alert> alerts) {
        LOG.info("Deleting alerts: {}", alerts);

        alerts.stream()
                .map(this::readAlert)
                .filter(Objects::nonNull)
                .forEach(this::deleteAlert);
    }

    @Override
    @Transactional
    public void createAndEnqueueAlertOnEvent(eu.domibus.core.alerts.model.service.Event event) {
        final eu.domibus.core.alerts.model.service.Alert alertOnEvent = createAlertOnEvent(event);
        enqueueAlert(alertOnEvent);
    }

    private Event readEvent(eu.domibus.core.alerts.model.service.Event event) {
        return eventDao.read(event.getEntityId());
    }

    private Alert readAlert(eu.domibus.core.alerts.model.service.Alert alert) {
        return alertDao.read(alert.getEntityId());
    }

    protected void deleteAlert(Alert alert) {
        LOG.debug("Deleting alert by first detaching it from its events: [{}]", alert);
        alert.getEvents().forEach(event -> event.removeAlert(alert));
        alertDao.delete(alert);
    }
}
