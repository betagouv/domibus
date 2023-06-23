package eu.domibus.core.earchive.listener;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.archive.client.api.ArchiveWebhookApi;
import eu.domibus.archive.client.model.BatchNotification;
import eu.domibus.core.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchiveBatchUtils;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import javax.jms.Message;
import java.util.UUID;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class EArchiveNotificationListenerTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveNotificationListenerTest.class);

    @Tested
    private EArchiveNotificationListener eArchiveNotificationListener;

    @Injectable
    private DatabaseUtil databaseUtil;
    @Injectable
    private EArchivingDefaultService eArchivingDefaultService;
    @Injectable
    private JmsUtil jmsUtil;
    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;
    @Injectable
    private EArchiveBatchUtils eArchiveBatchUtils;
    @Injectable
    private ArchiveWebhookApi eArchivingClientApi;
    @Injectable
    ObjectProvider<ArchiveWebhookApi> objectProvider;

    private final long entityId = 1L;

    private final String batchId = UUID.randomUUID().toString();

    @Test
    public void onMessageExported_ok(@Injectable Message message,
                                     @Injectable EArchiveBatchEntity eArchiveBatch,
                                     @Injectable BatchNotification batchNotification) {

        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");

        new Expectations(eArchiveNotificationListener) {{
            databaseUtil.getDatabaseUserName();
            result = "test";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE);
            result = "EXPORTED";

            eArchivingDefaultService.getEArchiveBatch(entityId, true);
            result = eArchiveBatch;

            eArchiveNotificationListener.buildBatchNotification(eArchiveBatch);
            result = batchNotification;
        }};

        eArchiveNotificationListener.onMessage(message);

    }

    @Test
    public void onMessageExported_ok_basicAuth(@Injectable Message message,
                                               @Injectable EArchiveBatchEntity eArchiveBatch,
                                               @Injectable BatchNotification batchNotification,
                                               @Injectable ArchiveWebhookApi apiClient) {

        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");

        new Expectations(eArchiveNotificationListener) {{
            databaseUtil.getDatabaseUserName();
            result = "test";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE);
            result = "EXPORTED";

            eArchivingDefaultService.getEArchiveBatch(entityId, true);
            result = eArchiveBatch;

            eArchiveNotificationListener.buildBatchNotification(eArchiveBatch);
            result = batchNotification;

        }};

        eArchiveNotificationListener.onMessage(message);
    }

    @Test
    void onMessageExported_NotificationTypeUnknown(@Injectable Message message,
                                                   @Injectable EArchiveBatchEntity eArchiveBatch) {

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

        }};

        Assertions.assertThrows(IllegalArgumentException.class, () -> eArchiveNotificationListener.onMessage(message));

    }
}
