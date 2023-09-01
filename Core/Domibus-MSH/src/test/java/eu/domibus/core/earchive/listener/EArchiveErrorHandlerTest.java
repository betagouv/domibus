package eu.domibus.core.earchive.listener;

import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class EArchiveErrorHandlerTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveErrorHandlerTest.class);

    @Tested
    private EArchiveErrorHandler eArchiveErrorHandler;

    @Injectable
    private EArchivingDefaultService eArchivingDefaultService;

    private final long entityId = 1L;

    @Test
    public void handleError_ok(@Injectable EArchiveBatchEntity eArchiveBatch) {

        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");

        new Expectations() {{

            eArchivingDefaultService.getEArchiveBatch(entityId, false);
            result = eArchiveBatch;
        }};
        RuntimeException error = new RuntimeException("ERROR");
        eArchiveErrorHandler.handleError(error);

        new FullVerifications(){{
            eArchivingDefaultService.setStatus(eArchiveBatch, EArchiveBatchStatus.FAILED, error.getMessage(), DomibusMessageCode.BUS_ARCHIVE_BATCH_EXPORT_FAILED.getCode());
            times = 1;
            eArchivingDefaultService.sendToNotificationQueue(eArchiveBatch, EArchiveBatchStatus.FAILED);
            times = 1;
            eArchiveBatch.getBatchId();
            times = 1;
        }};

    }
}
