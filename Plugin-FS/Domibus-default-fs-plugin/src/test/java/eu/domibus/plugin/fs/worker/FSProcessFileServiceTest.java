package eu.domibus.plugin.fs.worker;

import eu.domibus.plugin.fs.*;
import eu.domibus.plugin.fs.ebms3.ProcessingType;
import eu.domibus.plugin.fs.ebms3.UserMessage;
import eu.domibus.plugin.fs.exception.FSPluginException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import eu.domibus.plugin.fs.vfs.FileObjectDataSource;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Catalin Enache
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class FSProcessFileServiceTest {

    @Tested
    FSProcessFileService fsProcessFileService;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private FSPluginImpl backendFSPlugin;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    protected FSXMLHelper fsxmlHelper;

    @Injectable
    protected FSFileNameHelper fsFileNameHelper;


    private String domain = null;

    private FileObject rootDir;
    private FileObject outgoingFolder;
    private FileObject contentFile;
    private FileObject metadataFile;
    private FileObject pullMetadataFile;

    private UserMessage metadata;
    private UserMessage pullMetaData;

    @BeforeEach
    public void setUp() throws IOException, JAXBException {
        String location = "ram:///FSProcessFileServiceTest";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        outgoingFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
        outgoingFolder.createFolder();

        metadata = FSTestHelper.getUserMessage(this.getClass(), "testSendMessages_metadata.xml");

        try (InputStream testMetadata = FSTestHelper.getTestResource(this.getClass(), "testSendMessages_metadata.xml")) {
            metadataFile = outgoingFolder.resolveFile("metadata.xml");
            metadataFile.createFile();
            FileContent metadataFileContent = metadataFile.getContent();
            IOUtils.copy(testMetadata, metadataFileContent.getOutputStream());
            metadataFile.close();
        }

        pullMetaData=FSTestHelper.getUserMessage(this.getClass(), "testSetMessageInReadyToPull_metadata.xml");

        try (InputStream testMetadata = FSTestHelper.getTestResource(this.getClass(), "testSetMessageInReadyToPull_metadata.xml")) {
            pullMetadataFile = outgoingFolder.resolveFile("metadata.xml");
            pullMetadataFile.createFile();
            FileContent metadataFileContent = metadataFile.getContent();
            IOUtils.copy(testMetadata, metadataFileContent.getOutputStream());
            pullMetadataFile.close();
        }

        try (InputStream testContent = FSTestHelper.getTestResource(this.getClass(), "testSendMessages_content.xml")) {
            contentFile = outgoingFolder.resolveFile("content.xml");
            contentFile.createFile();
            FileContent contentFileContent = contentFile.getContent();
            IOUtils.copy(testContent, contentFileContent.getOutputStream());
            contentFile.close();
        }
    }

    @AfterEach
public void tearDown() throws FileSystemException {
        rootDir.close();
        outgoingFolder.close();
    }

    @Test
    public void test_processFile_FileExists_Success() throws Exception {
        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";

        new Expectations(fsProcessFileService) {{

            fsPluginProperties.getPayloadId(null);
            result = "cid:message";

            fsFilesManager.resolveSibling(contentFile, "metadata.xml");
            result = metadataFile;

            fsProcessFileService.parseMetadata((FileObject) any);
            result = metadata;

            fsFilesManager.getDataHandler(contentFile);
            result = new DataHandler(new FileObjectDataSource(contentFile));

            backendFSPlugin.submit(with(new Delegate<FSMessage>() {
                void delegate(FSMessage message) throws IOException {
                    Assertions.assertNotNull(message);
                    Assertions.assertNotNull(message.getPayloads());
                    FSPayload fsPayload = message.getPayloads().get("cid:message");
                    Assertions.assertNotNull(fsPayload);
                    Assertions.assertNotNull(fsPayload.getDataHandler());
                    Assertions.assertNotNull(message.getMetadata());

                    DataSource dataSource = fsPayload.getDataHandler().getDataSource();
                    Assertions.assertNotNull(dataSource);
                    Assertions.assertEquals("content.xml", dataSource.getName());
                    Assertions.assertTrue(
                            IOUtils.contentEquals(dataSource.getInputStream(), contentFile.getContent().getInputStream())
                    );

                    Assertions.assertEquals(metadata, message.getMetadata());
                }
            }));
            result = messageId;
        }};

        //tested method
        fsProcessFileService.processFile(contentFile, domain);

        new VerificationsInOrder() {{
            FSMessage message = null;
            backendFSPlugin.submit(message = withCapture());
            Assertions.assertEquals(message.getPayloads().size(), 1);
            Assertions.assertEquals(ProcessingType.PUSH,message.getMetadata().getProcessingType());
        }};
    }

    @Test
    public void test_processFileToPUll_FileExists_Success() throws Exception {
        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";

        new Expectations( fsProcessFileService) {{

            fsPluginProperties.getPayloadId(null);
            result = "cid:message";

            fsFilesManager.resolveSibling(contentFile, "metadata.xml");
            result = pullMetadataFile;

            fsProcessFileService.parseMetadata((FileObject) any);
            result = pullMetaData;

            fsFilesManager.getDataHandler(contentFile);
            result = new DataHandler(new FileObjectDataSource(contentFile));

            backendFSPlugin.submit(with(new Delegate<FSMessage>() {
                void delegate(FSMessage message) throws IOException {
                    Assertions.assertNotNull(message);
                    Assertions.assertNotNull(message.getPayloads());
                    FSPayload fsPayload = message.getPayloads().get("cid:message");
                    Assertions.assertNotNull(fsPayload);
                    Assertions.assertNotNull(fsPayload.getDataHandler());
                    Assertions.assertNotNull(message.getMetadata());

                    DataSource dataSource = fsPayload.getDataHandler().getDataSource();
                    Assertions.assertNotNull(dataSource);
                    Assertions.assertEquals("content.xml", dataSource.getName());
                    Assertions.assertTrue(
                            IOUtils.contentEquals(dataSource.getInputStream(), contentFile.getContent().getInputStream())
                    );

                    Assertions.assertEquals(pullMetaData, message.getMetadata());
                }
            }));
            result = messageId;
        }};

        //tested method
        fsProcessFileService.processFile(contentFile, domain);

        new VerificationsInOrder() {{
            FSMessage message = null;
            backendFSPlugin.submit(message = withCapture());
            Assertions.assertEquals(message.getPayloads().size(), 1);
            Assertions.assertEquals(ProcessingType.PULL,message.getMetadata().getProcessingType());
        }};
    }

    @Test()
    public void test_processFile_MetaDataException(final @Mocked FileObject processableFile, final @Mocked FileObject metadataFile) throws Exception {

        new Expectations(fsProcessFileService) {{
            fsFilesManager.resolveSibling(processableFile, FSSendMessagesService.METADATA_FILE_NAME);
            result = metadataFile;

            metadataFile.exists();
            result = false;

            processableFile.getName().getURI();
            result = "nonexistent_file";
        }};

        fsProcessFileService.processFile(processableFile, domain);

        new Verifications() {{
            backendFSPlugin.submit(withAny(new FSMessage(null, null)));
            maxTimes = 0;
        }};
    }
}
