package eu.domibus.plugin.fs.worker;


import eu.domibus.ext.services.AuthenticationExtService;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.JMSExtService;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.fs.*;
import eu.domibus.plugin.fs.ebms3.UserMessage;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.jms.Queue;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static eu.domibus.plugin.fs.worker.FSSendMessagesService.METADATA_FILE_NAME;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno, Catalin Enache
 */
@ExtendWith(JMockitExtension.class)
public class FSSendMessagesServiceTest {

    @Tested
    private FSSendMessagesService instance;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    private FSPluginImpl backendFSPlugin;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private AuthenticationExtService authenticationExtService;

    @Injectable
    private DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    private FSDomainService fsDomainService;

    @Injectable
    private JMSExtService jmsExtService;

    @Injectable
    private DomainContextExtService domainContextExtService;

    @Injectable
    @Qualifier("fsPluginSendQueue")
    private Queue fsPluginSendQueue;

    @Injectable
    protected FSXMLHelper fsxmlHelper;

    @Injectable
    protected FSFileNameHelper fsFileNameHelper;

    @Injectable
    private FSProcessFileService fsProcessFileService;

    @Injectable
    FSErrorMessageHelper fsErrorMessageHelper;

    private FileObject rootDir;
    private FileObject outgoingFolder;
    private FileObject contentFile;
    private FileObject metadataFile;

    private UserMessage metadata;

    @BeforeEach
    public void setUp() throws IOException, JAXBException {
        String location = "ram:///FSSendMessagesServiceTest";

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
    public void test_SendMessages_Root_Domain1() {
        final String domain0 = FSSendMessagesService.DEFAULT_DOMAIN;
        new Expectations(instance) {{
            domibusConfigurationExtService.isSecuredLoginRequired();
            result = true;

            fsDomainService.getFSPluginDomain();
            result = domain0;

            fsPluginProperties.getDomainEnabled(anyString);
            result = true;

        }};

        //tested method
        instance.sendMessages();

        new FullVerifications(instance) {{
            instance.sendMessages(domain0);
            times = 1;
        }};
    }

    @Test
    public void testSendMessages_RootDomain_NoMultitenancy() throws MessagingProcessingException, FileSystemException, FSSetUpException {
        final String domain = null; //root
        new Expectations( instance) {{
            domibusConfigurationExtService.isSecuredLoginRequired();
            result = false;

            fsFilesManager.setUpFileSystem(domain);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{metadataFile, contentFile};

            instance.canReadFileSafely((FileObject) any, anyString);
            result = true;

            fsPluginProperties.getDomainEnabled(anyString);
            result = true;
        }};

        //tested method
        instance.sendMessages(domain);

        new VerificationsInOrder() {{
            FileObject fileActual;
            instance.enqueueProcessableFile(fileActual = withCapture());
            Assertions.assertEquals(contentFile, fileActual);
        }};
    }

    @Test
    public void test_SendMessages_RootDomain_Multitenancy() throws FileSystemException, FSSetUpException {
        final String domainDefault = FSSendMessagesService.DEFAULT_DOMAIN;
        new Expectations( instance) {{
            domibusConfigurationExtService.isSecuredLoginRequired();
            result = true;

            fsPluginProperties.getAuthenticationUser(domainDefault);
            result = "user1";

            fsPluginProperties.getAuthenticationPassword(domainDefault);
            result = "pass1";

            fsFilesManager.setUpFileSystem(domainDefault);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{metadataFile, contentFile};

            instance.canReadFileSafely((FileObject) any, anyString);
            result = true;

            fsPluginProperties.getDomainEnabled(anyString);
            result = true;
        }};

        //tested method
        instance.sendMessages(domainDefault);

        new VerificationsInOrder() {{
            authenticationExtService.basicAuthenticate(anyString, anyString);

            FileObject fileActual;
            instance.enqueueProcessableFile(fileActual = withCapture());
            Assertions.assertEquals(contentFile, fileActual);
        }};
    }

    @Test
    public void testSendMessages_Domain1() throws MessagingProcessingException, FileSystemException {
        final String domain1 = "DOMAIN1";
        new Expectations( instance) {{
            domibusConfigurationExtService.isSecuredLoginRequired();
            result = true;

            fsFilesManager.setUpFileSystem(domain1);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{metadataFile, contentFile};

            fsPluginProperties.getAuthenticationUser(anyString);
            result = "user1";

            fsPluginProperties.getAuthenticationPassword(anyString);
            result = "pass1";

            instance.canReadFileSafely((FileObject) any, anyString);
            result = true;

            fsPluginProperties.getDomainEnabled(anyString);
            result = true;
        }};

        instance.sendMessages(domain1);

        new Verifications() {{
            authenticationExtService.basicAuthenticate(anyString, anyString);

            FileObject fileActual;
            instance.enqueueProcessableFile(fileActual = withCapture());
            Assertions.assertEquals(contentFile, fileActual);
        }};
    }

    @Test
    public void testSendMessages_Domain1_BadConfiguration() throws MessagingProcessingException, FileSystemException, FSSetUpException {
        final String domain1 = "DOMAIN1";
        new Expectations( instance) {{
            domibusConfigurationExtService.isSecuredLoginRequired();
            result = true;

            fsPluginProperties.getAuthenticationUser(anyString);
            result = "user1";

            fsPluginProperties.getAuthenticationPassword(anyString);
            result = "pass1";

            fsFilesManager.setUpFileSystem("DOMAIN1");
            result = new FSSetUpException("Test-forced exception");

            fsPluginProperties.getDomainEnabled(anyString);
            result = true;
        }};

        instance.sendMessages(domain1);

        new Verifications() {{
            authenticationExtService.basicAuthenticate(anyString, anyString);

            instance.enqueueProcessableFile((FileObject) any);
            maxTimes = 0;
        }};
    }

    @Test
    public void testCanReadFileSafely() {
        String domain = "domain1";
        new Expectations( instance) {{
            instance.checkSizeChangedRecently(contentFile, domain);
            result = false;
            instance.checkTimestampChangedRecently(contentFile, domain);
            result = false;
            instance.checkHasWriteLock(contentFile);
            result = false;
        }};

        //tested method
        boolean actualRes = instance.canReadFileSafely(contentFile, domain);

        Assertions.assertEquals(true, actualRes);
    }

    @Test
    public void testCanReadFileSafelyFalse() {
        String domain = "domain1";
        new Expectations( instance) {{
            instance.checkSizeChangedRecently(contentFile, domain);
            result = false;
            instance.checkTimestampChangedRecently(contentFile, domain);
            result = true;
        }};

        //tested method
        boolean actualRes = instance.canReadFileSafely(contentFile, domain);

        Assertions.assertEquals(false, actualRes);
    }

    @Test
    public void testCheckSizeChangedRecently_RecentFile_SameSize(final @Mocked FileObject contentFile2) throws FileSystemException {
        final String domain = "default";
        final String fileName = "ram:///FSSendMessagesServiceTest/OUT/content.xml2";
        long fileSize = 1234;
        long currentTime = new Date().getTime();
        instance.observedFilesInfo.put(fileName, new FileInfo(fileSize, currentTime, domain));

        new Expectations( instance) {{
            fsPluginProperties.getSendDelay(domain);
            result = 2000;

            contentFile2.getContent().getSize();
            result = fileSize;

            contentFile2.getName().getPath();
            result = fileName;
        }};

        //tested method
        boolean actualRes = instance.checkSizeChangedRecently(contentFile2, domain);
        Assertions.assertEquals(true, actualRes);

    }

    @Test
    public void testCheckSizeChangedRecently_OldFile_SameSize(final @Mocked FileObject contentFile2) throws FileSystemException {
        final String domain = "default";
        final String fileName = "ram:///FSSendMessagesServiceTest/OUT/content.xml2";
        long fileSize = 1234;
        long currentTime = new Date().getTime();
        instance.observedFilesInfo.put(fileName, new FileInfo(fileSize, currentTime - 500, domain));

        new Expectations( instance) {{
            fsPluginProperties.getSendDelay(domain);
            result = 200;

            contentFile2.getContent().getSize();
            result = fileSize;

            contentFile2.getName().getPath();
            result = fileName;
        }};

        //tested method
        boolean actualRes = instance.checkSizeChangedRecently(contentFile2, domain);
        Assertions.assertEquals(false, actualRes);

    }

    @Test
    public void testCheckTimestampChangedRecently_RecentFile(final @Mocked FileObject contentFile2) throws FileSystemException {
        final String domain = "default";
        new Expectations( instance) {{
            fsPluginProperties.getSendDelay(domain);
            result = 200;

            contentFile2.getContent().getLastModifiedTime();
            result = new Date().getTime();
        }};


        //tested method
        boolean actualRes = instance.checkTimestampChangedRecently(contentFile2, domain);
        Assertions.assertEquals(true, actualRes);
    }

    @Test
    public void testCheckTimestampChangedRecently_OldFile(final @Mocked FileObject contentFile2) throws FileSystemException {
        final String domain = "default";
        new Expectations( instance) {{
            fsPluginProperties.getSendDelay(domain);
            result = 200;

            contentFile2.getContent().getLastModifiedTime();
            result = new Date().getTime() - 500;
        }};


        //tested method
        boolean actualRes = instance.checkTimestampChangedRecently(contentFile2, domain);
        Assertions.assertEquals(false, actualRes);
    }

    @Test
    public void testCheckHasWriteLock() throws InterruptedException, FileSystemException {
        final String domain = "default";
        //tested method
        boolean actualRes = instance.checkHasWriteLock(contentFile);
        Assertions.assertEquals(true, actualRes);
    }

    @Test
    public void testClearObservedFiles() {
        final String domain = "default";
        final String fileName = "ram:///FSSendMessagesServiceTest/OUT/content.xml2";
        long fileSize = 1234;
        long currentTime = new Date().getTime();
        instance.observedFilesInfo.put(fileName, new FileInfo(fileSize, currentTime - 800, domain));

        new Expectations( instance) {{
            fsPluginProperties.getSendDelay(domain);
            result = 100;

            fsPluginProperties.getSendWorkerInterval(domain);
            result = 300;
        }};

        //tested method
        instance.clearObservedFiles(domain);
        Assertions.assertEquals(0, instance.observedFilesInfo.size());
    }

    @Test
    public void processFileSafelyWithJAXBExceptionTest(@Injectable FileObject processableFile) throws MessagingProcessingException, FileSystemException, JAXBException, XMLStreamException {
        String domain = "default";

        new Expectations(instance) {{
            fsProcessFileService.processFile(processableFile, domain);
            result = new JAXBException("Invalid metadata file", "DOM_001");
        }};

        instance.processFileSafely(processableFile, domain);

        new Verifications() {{
            fsFilesManager.handleSendFailedMessage(processableFile, domain, withCapture());
        }};
    }

    @Test
    public void processFileSafelyWithMessagingProcessingExceptionTest(@Injectable FileObject processableFile) throws MessagingProcessingException, FileSystemException, JAXBException, XMLStreamException {
        String domain = "default";

        new Expectations(instance) {{
            fsProcessFileService.processFile(processableFile, domain);
            result = new MessagingProcessingException();
        }};

        instance.processFileSafely(processableFile, domain);

        new Verifications() {{
            fsFilesManager.handleSendFailedMessage(processableFile, domain, withCapture());
        }};
    }

    @Test
    public void processFileSafelyWithRuntimeExceptionTest(@Injectable FileObject processableFile) throws MessagingProcessingException, FileSystemException, JAXBException, XMLStreamException {
        String domain = "default";

        new Expectations(instance) {{
            fsProcessFileService.processFile(processableFile, domain);
            result = new RuntimeException();
        }};

        instance.processFileSafely(processableFile, domain);

        new Verifications() {{
            fsFilesManager.handleSendFailedMessage(processableFile, domain, withCapture());
        }};
    }

    @Test
    public void isMetadata() {
        Assertions.assertTrue(instance.isMetadata(METADATA_FILE_NAME));
        Assertions.assertFalse(instance.isMetadata("non_metadata.xml"));
    }

    @Test
    public void isLocked() {
        List<String> lockedFileNames = Arrays.asList("file1.pdf", "file2.pdf");
        Optional<String> existingFileName = Optional.of("file1.pdf");
        Optional<String> nonExistingFileName = Optional.of("file11.pdf");
        Optional<String> emptyFileName = Optional.empty();

        Assertions.assertTrue(instance.isLocked(lockedFileNames, existingFileName));
        Assertions.assertFalse(instance.isLocked(lockedFileNames, nonExistingFileName));
        Assertions.assertFalse(instance.isLocked(lockedFileNames, emptyFileName));
    }

}
