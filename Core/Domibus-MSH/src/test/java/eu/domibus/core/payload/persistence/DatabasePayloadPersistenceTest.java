package eu.domibus.core.payload.persistence;

import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.payload.encryption.PayloadEncryptionService;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.compression.CompressionService;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class DatabasePayloadPersistenceTest {

    @Injectable
    protected BackendNotificationService backendNotificationService;

    @Injectable
    protected CompressionService compressionService;

    @Injectable
    PayloadPersistenceHelper payloadPersistenceHelper;

    @Injectable
    protected PayloadEncryptionService encryptionService;

    @Tested
    DatabasePayloadPersistence databasePayloadPersistence;


    @Test
    @Disabled("EDELIVERY-6896")
    public void testStoreIncomingPayload(@Injectable PartInfo partInfo,
                                         @Injectable UserMessage userMessage,
                                         @Injectable LegConfiguration legConfiguration,
                                         @Injectable String backendName,
                                         @Injectable InputStream inputStream,
                                         @Mocked ByteArrayOutputStream byteArrayOutputStream,
                                         @Injectable Cipher encryptCipherForPayload,
                                         @Mocked CipherOutputStream cipherOutputStream) throws IOException {
        final byte[] binaryData = "test".getBytes();

        new Expectations() {{
            new ByteArrayOutputStream(PayloadPersistence.DEFAULT_BUFFER_SIZE);
            result = byteArrayOutputStream;

            byteArrayOutputStream.toByteArray();
            result = binaryData;

            payloadPersistenceHelper.isPayloadEncryptionActive(userMessage);
            result = true;

            encryptionService.getEncryptCipherForPayload();
            result = encryptCipherForPayload;

            new CipherOutputStream(byteArrayOutputStream, encryptCipherForPayload);
            result = cipherOutputStream;

            partInfo.getPayloadDatahandler().getInputStream();
            result = inputStream;

            IOUtils.copy(inputStream, cipherOutputStream, PayloadPersistence.DEFAULT_BUFFER_SIZE);
        }};

        databasePayloadPersistence.storeIncomingPayload(partInfo, userMessage, legConfiguration);

        new Verifications() {{
            partInfo.setBinaryData(binaryData);
            partInfo.setLength(binaryData.length);
            partInfo.setFileName(null);
            partInfo.setEncrypted(true);

            payloadPersistenceHelper.validatePayloadSize(legConfiguration, binaryData.length);
            partInfo.loadBinary();
        }};
    }

    @Test
    public void testStoreOutgoingPayload(@Injectable PartInfo partInfo,
                                         @Injectable UserMessage userMessage,
                                         @Injectable LegConfiguration legConfiguration,
                                         @Injectable String backendName,
                                         @Injectable InputStream inputStream) throws IOException, EbMS3Exception {

        final String myfile = "myfile";
        byte[] binaryData = "fileContent".getBytes();

        new Expectations(databasePayloadPersistence) {{

            partInfo.getPayloadDatahandler().getInputStream();
            result = inputStream;

            partInfo.getFileName();
            result = myfile;

            payloadPersistenceHelper.isPayloadEncryptionActive(userMessage);
            result = false;

            databasePayloadPersistence.getOutgoingBinaryData(partInfo, inputStream, userMessage, legConfiguration, false);
            result = binaryData;
        }};

        databasePayloadPersistence.storeOutgoingPayload(partInfo, userMessage, legConfiguration, backendName);

        new Verifications() {{
            backendNotificationService.notifyPayloadSubmitted(userMessage, myfile, partInfo, backendName);
            backendNotificationService.notifyPayloadProcessed(userMessage, myfile, partInfo, backendName);

            partInfo.setBinaryData(binaryData);
            partInfo.setLength(binaryData.length);
            partInfo.setFileName(null);
            partInfo.setEncrypted(false);

            payloadPersistenceHelper.validatePayloadSize(legConfiguration, binaryData.length);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testGetOutgoingBinaryData(@Injectable PartInfo partInfo,
                                          @Injectable UserMessage userMessage,
                                          @Injectable LegConfiguration legConfiguration,
                                          @Injectable String backendName,
                                          @Injectable InputStream inputStream,
                                          @Mocked ByteArrayOutputStream byteArrayOutputStream,
                                          @Injectable Cipher encryptCipherForPayload,
                                          @Mocked CipherOutputStream cipherOutputStream,
                                          @Mocked GZIPOutputStream gzipOutputStream) throws IOException, EbMS3Exception {
        new Expectations() {{
            new ByteArrayOutputStream(PayloadPersistence.DEFAULT_BUFFER_SIZE);
            result = byteArrayOutputStream;

            compressionService.handleCompression(userMessage.getMessageId(), partInfo, legConfiguration);
            result = true;

            encryptionService.getEncryptCipherForPayload();
            result = encryptCipherForPayload;

            new CipherOutputStream(byteArrayOutputStream, encryptCipherForPayload);
            result = cipherOutputStream;

            new GZIPOutputStream(cipherOutputStream);
            result = gzipOutputStream;

            IOUtils.copy(inputStream, gzipOutputStream, PayloadPersistence.DEFAULT_BUFFER_SIZE);
        }};


        databasePayloadPersistence.getOutgoingBinaryData(partInfo, inputStream, userMessage, legConfiguration, Boolean.TRUE);
    }
}
