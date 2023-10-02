package eu.domibus.core.payload.persistence;

import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.payload.encryption.PayloadEncryptionService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.compression.CompressionService;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorage;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "DataFlowIssue"})
@ExtendWith(JMockitExtension.class)
public class FileSystemPayloadPersistenceTest {


    @Injectable
    protected PayloadFileStorageProvider storageProvider;

    @Injectable
    protected BackendNotificationService backendNotificationService;

    @Injectable
    protected CompressionService compressionService;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    protected PayloadPersistenceHelper payloadPersistenceHelper;

    @Injectable
    protected PayloadEncryptionService encryptionService;

    @Tested
    FileSystemPayloadPersistence fileSystemPayloadPersistence;

    @Test
    public void testStoreIncomingPayload(@Injectable PartInfo partInfo,
                                         @Injectable UserMessage userMessage,
                                         @Injectable PayloadFileStorage currentStorage,
                                         @Injectable LegConfiguration legConfiguration) throws IOException {

        new Expectations(fileSystemPayloadPersistence) {{

            partInfo.getFileName();
            result = null;

            partInfo.getLength();
            result = 4;

            storageProvider.getCurrentStorage();
            result = currentStorage;

            payloadPersistenceHelper.isPayloadEncryptionActive(userMessage);
            result = true;

            fileSystemPayloadPersistence.saveIncomingPayloadToDisk(partInfo, currentStorage, true);
        }};

        fileSystemPayloadPersistence.storeIncomingPayload(partInfo, userMessage, legConfiguration);

        new FullVerifications(fileSystemPayloadPersistence) {{
            fileSystemPayloadPersistence.saveIncomingPayloadToDisk(partInfo, currentStorage, true);

            payloadPersistenceHelper.validatePayloadSize(legConfiguration, anyLong);
        }};
    }

    @Test
    public void testSaveIncomingPayloadToDisk(@Injectable PartInfo partInfo,
                                              @Injectable PayloadFileStorage storage,
                                              @Mocked File file,
                                              @Injectable InputStream inputStream) throws IOException {

        String path = "/home/invoice.pdf";
        long fileLength = 10L;
        new Expectations(fileSystemPayloadPersistence) {{
            storage.getStorageDirectory();
            result = file;

            File attachmentStore = new File(file, anyString);
            attachmentStore.getAbsolutePath();
            result = path;

            partInfo.getPayloadDatahandler().getInputStream();
            result = inputStream;

            partInfo.getHref();
            result = "href";

            fileSystemPayloadPersistence.saveIncomingFileToDisk(attachmentStore, inputStream, false);
            result = fileLength;
            times = 1;

            partInfo.toString();
            result = "toStringPartInfo";
        }};

        fileSystemPayloadPersistence.saveIncomingPayloadToDisk(partInfo, storage, false);

        new Verifications() {{
            partInfo.setFileName(path);         times = 1;
            partInfo.setLength(fileLength);     times = 1;
            partInfo.setEncrypted(false);       times = 1;
            partInfo.loadBinary();              times = 1;
        }};
    }

    @Test
    public void testStoreOutgoingPayload(@Injectable PartInfo partInfo,
                                         @Injectable UserMessage userMessage,
                                         @Injectable PayloadFileStorage currentStorage,
                                         @Injectable LegConfiguration legConfiguration,
                                         @Injectable String backendName) throws IOException, EbMS3Exception {

        new Expectations(fileSystemPayloadPersistence) {{
            userMessage.isMessageFragment();
            result = false;

            storageProvider.getCurrentStorage();
            result = currentStorage;

            fileSystemPayloadPersistence.saveOutgoingPayloadToDisk(partInfo, userMessage, legConfiguration, currentStorage, backendName);
        }};

        fileSystemPayloadPersistence.storeOutgoingPayload(partInfo, userMessage, legConfiguration, backendName);

        new Verifications() {{
            fileSystemPayloadPersistence.saveOutgoingPayloadToDisk(partInfo, userMessage, legConfiguration, currentStorage, backendName);
        }};
    }

    @Test
    public void testSaveOutgoingPayloadToDisk(@Injectable PartInfo partInfo,
                                              @Injectable UserMessage userMessage,
                                              @Injectable PayloadFileStorage currentStorage,
                                              @Injectable LegConfiguration legConfiguration,
                                              @Injectable String backendName,
                                              @Injectable InputStream inputStream,
                                              @Mocked File fileBase
    ) throws IOException, EbMS3Exception {

        final String myfile = "myfile";
        final int length = 123;
        final String myFilePath = "myFilePath";

        new Expectations(fileSystemPayloadPersistence) {{
            currentStorage.getStorageDirectory();
            result = fileBase;

            partInfo.getPayloadDatahandler().getInputStream();
            result = inputStream;

            partInfo.getFileName();
            result = myfile;

            File file = new File((File) any, anyString);

            file.getAbsolutePath();
            result = myFilePath;

            payloadPersistenceHelper.isPayloadEncryptionActive(userMessage);
            result = false;

            fileSystemPayloadPersistence.saveOutgoingFileToDisk(file, partInfo, inputStream, userMessage, legConfiguration, Boolean.FALSE);
            result = length;
        }};

        fileSystemPayloadPersistence.saveOutgoingPayloadToDisk(partInfo, userMessage, legConfiguration, currentStorage, backendName);


        new Verifications() {{
            backendNotificationService.notifyPayloadSubmitted(userMessage, myfile, partInfo, backendName);
            backendNotificationService.notifyPayloadProcessed(userMessage, myfile, partInfo, backendName);

            partInfo.setLength(length);
            partInfo.setFileName(myFilePath);
            partInfo.setEncrypted(false);
        }};
    }
}
