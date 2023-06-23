package eu.domibus.core.message;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.PartProperty;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.sender.retry.UpdateRetryLoggingService;
import eu.domibus.core.message.compression.CompressionService;
import eu.domibus.core.message.splitandjoin.SplitAndJoinService;
import eu.domibus.core.payload.persistence.PayloadPersistence;
import eu.domibus.core.payload.persistence.PayloadPersistenceProvider;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorage;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.plugin.transformer.SubmissionAS4Transformer;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static eu.domibus.common.model.configuration.Payload_.MIME_TYPE;
import static eu.domibus.core.message.MessagingServiceImpl.MIME_TYPE_APPLICATION_UNKNOWN;

/**
 * @author Ioana Dragusanu
 * @author Cosmin Baciu
 * @since 3.3
 */

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class MessagingServiceImplTest {

    @Tested
    MessagingServiceImpl messagingService;

    @Injectable
    protected PayloadPersistenceProvider payloadPersistenceProvider;

    @Injectable
    UserMessagePayloadService userMessagePayloadService;

    @Injectable
    PayloadFileStorage storage;

    @Injectable
    PayloadFileStorageProvider storageProvider;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    LegConfiguration legConfiguration;

    @Injectable
    SplitAndJoinService splitAndJoinService;

    @Injectable
    CompressionService compressionService;

    @Injectable
    DomainTaskExecutor domainTaskExecutor;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    BackendNotificationService backendNotificationService;

    @Injectable
    UserMessageLogDao userMessageLogDao;

    @Injectable
    PartInfoServiceImpl partInfoService;

    @Injectable
    UserMessageDao userMessageDao;

    @Injectable
    SubmissionAS4Transformer transformer;

    @Injectable
    UpdateRetryLoggingService updateRetryLoggingService;

    @Test
    public void testStoreOutgoingPayload(@Injectable UserMessage userMessage,
                                         @Injectable PartInfo partInfo,
                                         @Injectable LegConfiguration legConfiguration,
                                         @Injectable String backendName,
                                         @Injectable PayloadPersistence payloadPersistence) throws IOException, EbMS3Exception {
        new Expectations(messagingService) {{
            payloadPersistenceProvider.getPayloadPersistence(partInfo, userMessage);
            result = payloadPersistence;
        }};

        messagingService.storeOutgoingPayload(partInfo, userMessage, legConfiguration, backendName);

        new Verifications() {{
            payloadPersistence.storeOutgoingPayload(partInfo, userMessage, legConfiguration, backendName);
            times = 1;
        }};
    }

    @Test
    public void testStoreIncomingPayload(@Injectable UserMessage userMessage,
                                         @Injectable PartInfo partInfo,
                                         @Injectable PayloadPersistence payloadPersistence) throws IOException {
        new Expectations() {{
            payloadPersistenceProvider.getPayloadPersistence(partInfo, userMessage);
            result = payloadPersistence;
        }};

        messagingService.storeIncomingPayload(partInfo, userMessage, null);

        new Verifications() {{
            payloadPersistence.storeIncomingPayload(partInfo, userMessage, null);
            times = 1;
        }};
    }

    @Test
    public void testStoreSourceMessagePayloads(@Injectable UserMessage userMessage,
                                               @Injectable MSHRole mshRole,
                                               @Injectable LegConfiguration legConfiguration,
                                               @Injectable String backendName) {

        String messageId = "123";
        Long messageEntityId = 1L;
        new Expectations(messagingService) {{
            userMessage.getMessageId();
            result = messageId;

            userMessage.getEntityId();
            result = messageEntityId;

            messagingService.storePayloads(userMessage, null, mshRole, legConfiguration, backendName);
        }};

        messagingService.storeSourceMessagePayloads(userMessage, null, mshRole, legConfiguration, backendName);

        new Verifications() {{
            userMessageService.scheduleSourceMessageSending(messageId, messageEntityId);
        }};
    }

    @Test
    public void testStoreMessageCalls(@Injectable final UserMessage userMessage) {
        messagingService.storeMessagePayloads(userMessage, null, MSHRole.SENDING, legConfiguration, "backend");
    }

    @Test
    public void testStoreOutgoingMessage(@Injectable PayloadPersistence payloadPersistence) throws Exception {
        UserMessage userMessage = new UserMessage();
        List<PartInfo> partInfos = new ArrayList<>();
        PartInfo partInfo = new PartInfo();
        partInfos.add(partInfo);

        new Expectations() {{
            payloadPersistenceProvider.getPayloadPersistence(partInfo, userMessage);
            result = payloadPersistence;

            userMessage.getPartyInfo();
            result = partInfos;
        }};

        final String backend = "backend";
        messagingService.storeMessagePayloads(userMessage, partInfos, MSHRole.SENDING, legConfiguration, backend);

        new Verifications() {{
            payloadPersistence.storeOutgoingPayload(partInfo, userMessage, legConfiguration, backend);
            times = 1;
        }};
    }

    @Test
    public void setContentType_unknown(@Injectable PartInfo partInfo) {
        new Expectations() {{
            partInfo.getPartProperties();
            result = null;

            partInfo.getPayloadDatahandler().getContentType();
            result = null;

            partInfo.getHref();
            result = "href";
        }};

        messagingService.setContentType(partInfo);

        new FullVerifications() {{
            partInfo.setMime(MIME_TYPE_APPLICATION_UNKNOWN);
            times = 1;
        }};
    }

    @Test
    public void setContentType(@Injectable PartInfo partInfo,
                               @Injectable PartProperty partProperty) {

        HashSet<PartProperty> partProperties = new HashSet<>();
        partProperties.add(partProperty);

        new Expectations() {{
            partInfo.getPayloadDatahandler().getContentType();
            result = null;

            partInfo.getPartProperties();
            result = partProperties;

            partProperty.getName();
            result = MIME_TYPE;

            partProperty.getValue();
            result = "application/json";

            partInfo.getHref();
            result = "href";
        }};

        messagingService.setContentType(partInfo);

        new FullVerifications() {{
            partInfo.setMime("application/json");
            times = 1;
        }};
    }
}
