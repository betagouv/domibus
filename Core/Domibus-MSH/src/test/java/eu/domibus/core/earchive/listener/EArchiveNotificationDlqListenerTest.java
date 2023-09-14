package eu.domibus.core.earchive.listener;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import eu.domibus.core.alerts.configuration.common.AlertModuleConfiguration;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.Message;
import java.util.UUID;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class EArchiveNotificationDlqListenerTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveNotificationDlqListenerTest.class);

    @Tested
    private EArchiveNotificationDlqListener eArchiveNotificationDlqListener;

    @Injectable
    private EArchivingDefaultService eArchivingDefaultService;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Injectable
    private JmsUtil jmsUtil;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private EventService eventService;

    @Injectable
    AlertConfigurationService alertConfigurationService;

    private final long entityId = 1L;

    private final String batchId = UUID.randomUUID().toString();

    @Test
    public void onMessageExported_ok(final @Mocked Message message,
                                     @Injectable EArchiveBatchEntity eArchiveBatch,
                                     @Injectable AlertModuleConfiguration alertConfiguration
    ) {

        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");

        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            result = "test";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE);
            result = "EXPORTED";

            eArchivingDefaultService.getEArchiveBatch(entityId, false);
            result = eArchiveBatch;

            alertConfigurationService.getConfiguration(AlertType.ARCHIVING_NOTIFICATION_FAILED);
            result = alertConfiguration;

            alertConfiguration.isActive();
            result = true;
        }};

        eArchiveNotificationDlqListener.onMessage(message);
    }

    @Test
    void onMessageExported_NotificationTypeUnknown(final @Mocked Message message,
                                                   @Injectable EArchiveBatchEntity eArchiveBatch,
                                                   @Injectable AlertModuleConfiguration alertConfiguration
    ) {

        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");

        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            result = "test";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE);
            result = "UNKNOWN";

            alertConfigurationService.getConfiguration(AlertType.ARCHIVING_NOTIFICATION_FAILED);
            result = alertConfiguration;

            alertConfiguration.isActive();
            result = true;

        }};

        Assertions.assertThrows(IllegalArgumentException.class, () -> eArchiveNotificationDlqListener.onMessage(message));

        new FullVerifications() {{
            jmsUtil.setCurrentDomainFromMessage(message);
        }};

    }
}
