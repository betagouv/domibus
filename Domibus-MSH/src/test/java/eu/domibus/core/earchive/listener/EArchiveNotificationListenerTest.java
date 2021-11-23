package eu.domibus.core.earchive.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchiveBatchUtils;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.core.proxy.DomibusProxyService;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import gen.eu.domibus.archive.client.api.ArchiveWebhookApi;
import gen.eu.domibus.archive.client.model.BatchNotification;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.Message;
import java.util.UUID;

/**
 * @author François Gautier
 * @since 5.0
 */
@RunWith(JMockit.class)
public class EArchiveNotificationListenerTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveNotificationListenerTest.class);

    @Tested
    private EArchiveNotificationListener eArchiveNotificationListener;

    @Injectable
    private EArchivingDefaultService eArchivingDefaultService;

    @Injectable
    private EArchiveBatchUtils eArchiveBatchUtils;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Injectable
    private JmsUtil jmsUtil;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomibusProxyService domibusProxyService;

    @Injectable
    private ObjectMapper objectMapper;

    private final long entityId = 1L;

    private final String batchId = UUID.randomUUID().toString();

    @Test
    public void onMessageExported_ok(final @Mocked Message message,
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

            eArchiveNotificationListener.initializeEarchivingClientApi();
            result = apiClient;
        }};

        eArchiveNotificationListener.onMessage(message);
    }

    @Test(expected = IllegalArgumentException.class)
    public void onMessageExported_NotificationTypeUnknown(final @Mocked Message message,
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

        eArchiveNotificationListener.onMessage(message);

        new FullVerifications() {};

    }
}