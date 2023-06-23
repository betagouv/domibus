package eu.domibus.core.earchive.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.earchive.DomibusEArchiveException;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.earchive.*;
import eu.domibus.core.earchive.eark.DomibusEARKSIPResult;
import eu.domibus.core.earchive.eark.FileSystemEArchivePersistence;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.messaging.MessageConstants;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.Message;
import java.util.*;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_EARCHIVING_NOTIFICATION_DETAILS_ENABLED;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked"})
@ExtendWith(JMockitExtension.class)
public class EArchiveListenerTest {

    @Tested
    private EArchiveListener eArchiveListener;

    @Injectable
    private FileSystemEArchivePersistence fileSystemEArchivePersistence;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Injectable
    private EArchivingDefaultService eArchivingDefaultService;

    @Injectable
    private JmsUtil jmsUtil;

    @Injectable
    private ObjectMapper jsonMapper;

    @Injectable
    private EArchiveBatchUtils eArchiveBatchUtils;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    private String batchId;

    private Long entityId;

    private List<EArchiveBatchUserMessage> batchUserMessages;

    @BeforeEach
    public void setUp() {
        batchId = UUID.randomUUID().toString();
        entityId = new Random().nextLong();

        batchUserMessages = Arrays.asList(
                new EArchiveBatchUserMessage(new Random().nextLong(), UUID.randomUUID().toString()),
                new EArchiveBatchUserMessage(new Random().nextLong(), UUID.randomUUID().toString()));
    }

    @Test
    public void onMessage_noBatchInfo(@Injectable Message message) {
        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            result = "unitTest";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = null;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = null;

        }};
        eArchiveListener.onMessage(message);

        new FullVerifications() {
        };
    }

    @Test
    void onMessage_noBatchFound(@Injectable Message message) {
        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            result = "unitTest";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            eArchivingDefaultService.getEArchiveBatch(entityId, true);
            result = new DomibusEArchiveException("eArchive batch not found for batchId: [" + entityId + "]");
        }};

        Assertions.assertThrows(DomibusEArchiveException. class,() -> eArchiveListener.onMessage(message));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void onMessage_noMessages(@Injectable Message message,
                                     @Injectable EArchiveBatchEntity eArchiveBatch,
                                     @Injectable DomibusEARKSIPResult domibusEARKSIPResult) {
        Long firstUserMessageEntityId = 220511070000001204L;

        Long lastUserMessageEntityId = 220511080000001204L;

        new Expectations(eArchiveListener) {{
            databaseUtil.getDatabaseUserName();
            result = "unitTest";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            jmsUtil.getMessageTypeSafely(message);
            result = EArchiveBatchStatus.EXPORTED.name();

            eArchivingDefaultService.getEArchiveBatch(entityId, true);
            result = eArchiveBatch;

            eArchiveBatch.getDateRequested();
            result = new Date();

            eArchiveBatch.geteArchiveBatchUserMessages();
            result = null;

            eArchiveBatch.getBatchId();
            result = "batchId";

            eArchiveBatch.getRequestType();
            result = EArchiveRequestType.CONTINUOUS;

            eArchiveBatchUtils.getMessageStartDate(batchUserMessages, 0);
            result = firstUserMessageEntityId;

            eArchiveBatchUtils.getMessageStartDate(batchUserMessages, eArchiveBatchUtils.getLastIndex(batchUserMessages));
            result = lastUserMessageEntityId;

            userMessageLogDao.findByEntityId(firstUserMessageEntityId).getReceived();
            result = new Date();

            userMessageLogDao.findByEntityId(lastUserMessageEntityId).getReceived();
            result = new Date();

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_EARCHIVING_NOTIFICATION_DETAILS_ENABLED);
            result = true;

            fileSystemEArchivePersistence.createEArkSipStructure((BatchEArchiveDTO) any, (List<EArchiveBatchUserMessage>) any, (Date) any, (Date) any);
            result = domibusEARKSIPResult;

            domibusEARKSIPResult.getManifestChecksum();
            result = "sha256:test";
        }};

        eArchiveListener.onMessage(message);

        new Verifications() {{
            jmsUtil.setCurrentDomainFromMessage(message);
            times = 1;

            eArchivingDefaultService.setStatus(eArchiveBatch, EArchiveBatchStatus.STARTED);
            times = 1;

            eArchiveBatchUtils.getMessageIds((List<EArchiveBatchUserMessage>) any);
            times = 1;

            eArchiveBatch.setManifestChecksum("sha256:test");
            times = 1;

            eArchivingDefaultService.executeBatchIsExported(((EArchiveBatchEntity) any), (List<EArchiveBatchUserMessage>) any);
            times = 1;

            domibusEARKSIPResult.getDirectory().toAbsolutePath().toString();
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void onMessage_ok(@Injectable Message message,
                             @Injectable EArchiveBatchEntity eArchiveBatch,
                             @Injectable DomibusEARKSIPResult domibusEARKSIPResult) {

        Long firstUserMessageEntityId = 220511070000001204L;
        Long lastUserMessageEntityId = 220511080000001204L;

        new Expectations(eArchiveListener) {{
            databaseUtil.getDatabaseUserName();
            result = "unitTest";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getMessageTypeSafely(message);
            result = EArchiveBatchStatus.EXPORTED.name();

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            eArchivingDefaultService.getEArchiveBatch(entityId, true);
            result = eArchiveBatch;

            eArchiveBatch.geteArchiveBatchUserMessages();
            result = batchUserMessages;

            eArchiveBatch.getDateRequested();
            result = new Date();

            eArchiveBatch.getRequestType();
            result = EArchiveRequestType.CONTINUOUS;

            domibusEARKSIPResult.getManifestChecksum();
            result = "sha256:test";

            eArchiveBatchUtils.getMessageStartDate(batchUserMessages, 0);
            result = firstUserMessageEntityId;

            eArchiveBatchUtils.getMessageStartDate(batchUserMessages, eArchiveBatchUtils.getLastIndex(batchUserMessages));
            result = lastUserMessageEntityId;

            userMessageLogDao.findByEntityId(firstUserMessageEntityId).getReceived();
            result = new Date();

            userMessageLogDao.findByEntityId(lastUserMessageEntityId).getReceived();
            result = new Date();

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_EARCHIVING_NOTIFICATION_DETAILS_ENABLED);
            result = true;

            fileSystemEArchivePersistence.createEArkSipStructure((BatchEArchiveDTO) any, (List<EArchiveBatchUserMessage>) any, (Date) any, (Date) any);
            result = domibusEARKSIPResult;

            eArchiveBatch.getBatchId();
            result = batchId;

        }};

        eArchiveListener.onMessage(message);

        new Verifications() {{
            jmsUtil.setCurrentDomainFromMessage(message);
            times = 1;

            eArchivingDefaultService.executeBatchIsExported(((EArchiveBatchEntity) any), (List<EArchiveBatchUserMessage>) any);
            times = 1;

            eArchiveBatchUtils.getMessageIds((List<EArchiveBatchUserMessage>) any);
            times = 1;

            eArchiveBatch.setManifestChecksum("sha256:test");
            times = 1;

            eArchivingDefaultService.setStatus(eArchiveBatch, EArchiveBatchStatus.STARTED);
            times = 1;

            domibusEARKSIPResult.getDirectory().toAbsolutePath().toString();

            databaseUtil.getDatabaseUserName();
        }};
    }

    @Test
    public void onMessage_ArchiveOK(@Injectable Message message,
                                    @Injectable EArchiveBatchEntity eArchiveBatch) {
        new Expectations() {{
            databaseUtil.getDatabaseUserName();
            result = "unitTest";

            jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
            result = batchId;

            jmsUtil.getMessageTypeSafely(message);
            result = EArchiveBatchStatus.ARCHIVED.name();

            jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
            result = entityId;

            eArchivingDefaultService.getEArchiveBatch(entityId, true);
            result = eArchiveBatch;

            eArchiveBatch.geteArchiveBatchUserMessages();
            result = batchUserMessages;

            eArchiveBatch.getBatchId();
            result = "batchId";

        }};

        eArchiveListener.onMessage(message);

        new FullVerifications() {{
            jmsUtil.setCurrentDomainFromMessage(message);
            times = 1;

            eArchivingDefaultService.executeBatchIsArchived(eArchiveBatch, batchUserMessages);
            times = 1;
        }};
    }
}
