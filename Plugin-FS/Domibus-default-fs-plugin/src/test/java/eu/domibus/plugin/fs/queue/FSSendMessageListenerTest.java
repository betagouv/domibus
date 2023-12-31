package eu.domibus.plugin.fs.queue;

import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.fs.FSFilesManager;
import eu.domibus.plugin.fs.FSTestHelper;
import eu.domibus.plugin.fs.worker.FSSendMessagesService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.Message;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Catalin Enache
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class FSSendMessageListenerTest {

    @Tested
    FSSendMessageListener fsSendMessageListener;

    @Injectable
    private FSSendMessagesService fsSendMessagesService;

    @Injectable
    private DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    FSFilesManager fsFilesManager;

    private FileObject rootDir;
    private FileObject outgoingFolder;
    private FileObject contentFile;

    @BeforeEach
    public void setUp() throws IOException {
        String location = "ram:///FSSendMessageListenerTest";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        outgoingFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
        outgoingFolder.createFolder();

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
    public void test_onMessage_FileExists_Success(final @Mocked Message message, final @Mocked FileObject file) throws Exception {
        final String domain = null;
        final String fileName = "ram:" + contentFile.getURL().getFile();


        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = domain;

            message.getStringProperty(MessageConstants.FILE_NAME);
            result = fileName;
        }};

        //tested method
        fsSendMessageListener.onMessage(message);

        new FullVerifications(fsSendMessagesService) {{
            fsSendMessagesService.authenticateForDomain(domain);

            String domainActual;
            fsSendMessagesService.processFileSafely((FileObject) any, domainActual = withCapture());
            Assertions.assertEquals(domain, domainActual);
        }};
    }

    @Test
    public void test_onMessage_FileDoesntExist_Error(final @Mocked Message message, final @Mocked FileObject file) throws Exception {
        final String domain = null;
        final String fileName = "ram:" + contentFile.getURL().getFile() + "bla";


        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = domain;

            message.getStringProperty(MessageConstants.FILE_NAME);
            result = fileName;
        }};

        //tested method
        fsSendMessageListener.onMessage(message);

        new Verifications() {{
            fsSendMessagesService.processFileSafely((FileObject) any, anyString);
            maxTimes = 0;
        }};
    }

    @Test
    public void test_onMessage_BlankFilename_Error(final @Mocked Message message, final @Mocked FileObject file) throws Exception {
        final String domain = null;
        final String fileName = "";

        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = domain;

            message.getStringProperty(MessageConstants.FILE_NAME);
            result = fileName;
        }};

        //tested method
        fsSendMessageListener.onMessage(message);

        new Verifications() {{
            fsSendMessagesService.processFileSafely((FileObject) any, anyString);
            maxTimes = 0;
        }};
    }
}
