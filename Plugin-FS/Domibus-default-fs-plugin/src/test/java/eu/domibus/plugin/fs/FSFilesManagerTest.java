package eu.domibus.plugin.fs;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.fs.exception.FSPluginException;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.activation.DataHandler;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@ExtendWith(JMockitExtension.class)
public class FSFilesManagerTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FSFilesManagerTest.class);

    @Tested
    private FSFilesManager instance;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    private FileObject rootDir;

    private FileObject metadataFile;

    private FileObject contentFile;

    private FileObject outgoingFolder;

    @Injectable
    private FileObject mockedRootDir;

    @Injectable
    protected FSFileNameHelper fsFileNameHelper;

    // TODO [EDELIVERY-11854] Fix merging issue
//    @BeforeEach
//    public void setUp() throws IOException {
//        String location = "ram:///FSFilesManagerTest";
//        String sampleFolderName = "samplefolder";
//
//        FileSystemManager fsManager = VFS.getManager();
//        rootDir = fsManager.resolveFile(location);
//        rootDir.createFolder();
//
//        FileObject sampleFolder = rootDir.resolveFile(sampleFolderName);
//        sampleFolder.createFolder();
//
//        rootDir.resolveFile("file1").createFile();
//        rootDir.resolveFile("file2").createFile();
//        rootDir.resolveFile("file3").createFile();
//        rootDir.resolveFile("toberenamed").createFile();
//        rootDir.resolveFile("toberenamed").getContent().setLastModifiedTime(0);
//        rootDir.resolveFile("tobemoved").createFile();
//        rootDir.resolveFile("toberenamed").getContent().setLastModifiedTime(0);
//        rootDir.resolveFile("tobedeleted").createFile();
//
//        rootDir.resolveFile("targetfolder1/targetfolder2").createFolder();
//
//        outgoingFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
//        outgoingFolder.createFolder();
//        try (InputStream testMetadata = FSTestHelper.getTestResource(this.getClass(), "metadata.xml")) {
//            metadataFile = outgoingFolder.resolveFile("metadata.xml");
//            metadataFile.createFile();
//            FileContent metadataFileContent = metadataFile.getContent();
//            IOUtils.copy(testMetadata, metadataFileContent.getOutputStream());
//            metadataFile.close();
//        }
//
//        try (InputStream testContent = FSTestHelper.getTestResource(this.getClass(), "content.xml")) {
//            contentFile = outgoingFolder.resolveFile("content.xml");
//            contentFile.createFile();
//            FileContent contentFileContent = contentFile.getContent();
//            IOUtils.copy(testContent, contentFileContent.getOutputStream());
//            contentFile.close();
//        }
//    }
//
//    @AfterEach
//    public void tearDown() throws FileSystemException {
//        rootDir.deleteAll();
//        rootDir.close();
//    }
//
//    // This test fails with a temporary filesystem
//    @Test
//    void testGetEnsureRootLocation_Auth() {
//        String location = "ram:///FSFilesManagerTest";
//        String domain = "domain";
//        String user = "user";
//        String password = "password";
//
//        Assertions.assertThrows(FSSetUpException.class, () -> instance.getEnsureRootLocation(location, domain, user, password));
//    }
//
//    @Test
//    public void testGetEnsureRootLocation() throws Exception {
//        String location = "ram:///FSFilesManagerTest";
//
//        FileObject result = instance.getEnsureRootLocation(location);
//        Assertions.assertNotNull(result);
//        Assertions.assertTrue(result.exists());
//    }
//
//    @Test
//    public void testGetEnsureChildFolder() throws Exception {
//        String folderName = "samplefolder";
//
//        FileObject result = instance.getEnsureChildFolder(rootDir, folderName);
//
//        Assertions.assertNotNull(result);
//        Assertions.assertTrue(result.exists());
//        Assertions.assertEquals(FileType.FOLDER, result.getType());
//    }
//
//    @Test
//    void testGetEnsureChildFolder_FileSystemException() throws Exception {
//        final String folderName = "samplefolder";
//
//        new Expectations(instance) {{
//            mockedRootDir.exists();
//            result = true;
//
//            mockedRootDir.resolveFile(folderName);
//            result = new FileSystemException("some unexpected error");
//        }};
//
//        Assertions.assertThrows(FSSetUpException.class, () -> instance.getEnsureChildFolder(mockedRootDir, folderName));
//    }
//
//    @Test
//    public void testFindAllDescendantFiles() throws Exception {
//        FileObject[] files = instance.findAllDescendantFiles(rootDir);
//
//        Assertions.assertNotNull(files);
//        Assertions.assertEquals(8, files.length);
//        Assertions.assertEquals("ram:///FSFilesManagerTest/file1", files[0].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/file2", files[1].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/file3", files[2].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/toberenamed", files[3].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/tobemoved", files[4].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/tobedeleted", files[5].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/OUT/metadata.xml", files[6].getName().getURI());
//        Assertions.assertEquals("ram:///FSFilesManagerTest/OUT/content.xml", files[7].getName().getURI());
//    }
//
//    @Test
//    public void testGetDataHandler() {
//        DataHandler result = instance.getDataHandler(rootDir);
//
//        Assertions.assertNotNull(result);
//        Assertions.assertNotNull(result.getDataSource());
//    }
//
//    @Test
//    public void testResolveSibling() throws Exception {
//        FileObject result = instance.resolveSibling(rootDir, "siblingdir");
//
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("ram:///siblingdir", result.getName().getURI());
//    }
//
//    @Test
//    public void testRenameFile() throws Exception {
//        FileObject file = rootDir.resolveFile("toberenamed");
//
//        long beforeMillis = System.currentTimeMillis();
//        FileObject result = instance.renameFile(file, "renamed");
//        long afterMillis = System.currentTimeMillis();
//
//        Assertions.assertNotNull(result);
//        Assertions.assertEquals("ram:///FSFilesManagerTest/renamed", result.getName().getURI());
//        Assertions.assertTrue(result.exists());
//        Assertions.assertTrue(result.getContent().getLastModifiedTime() >= beforeMillis);
//        Assertions.assertTrue(result.getContent().getLastModifiedTime() <= afterMillis);
//    }
//
//    // This test fails with a temporary filesystem
//    @Test
//    void testSetUpFileSystem_Domain() {
//        new Expectations(instance) {{
//            fsPluginProperties.getLocation("DOMAIN1");
//            result = "ram:///FSFilesManagerTest/samplefolder";
//
//            fsPluginProperties.getUser("DOMAIN1");
//            result = "user";
//
//            fsPluginProperties.getPassword("DOMAIN1");
//            result = "secret";
//        }};
//
//        Assertions.assertThrows(FSSetUpException.class, () -> instance.setUpFileSystem("DOMAIN1"));
//
//    }
//
//    @Test
//    public void testSetUpFileSystem_NoLocation() throws Exception {
//        new Expectations(instance) {{
//            fsPluginProperties.getLocation("DOMAIN1");
//            result = StringUtils.EMPTY;
//        }};
//
//        try {
//            instance.setUpFileSystem("DOMAIN1");
//            Assertions.fail("Exception expected");
//        } catch (FSSetUpException e) {
//            Assertions.assertTrue(e.getMessage().contains("Location folder is not set for domain"));
//        }
//
//        new FullVerifications() {
//        };
//    }
//
//    @Test
//    public void testSetUpFileSystem() throws Exception {
//        new Expectations(instance) {{
//            fsPluginProperties.getLocation(null);
//            result = "ram:///FSFilesManagerTest";
//        }};
//
//        FileObject result = instance.setUpFileSystem(null);
//
//        Assertions.assertNotNull(result);
//        Assertions.assertTrue(result.exists());
//        Assertions.assertEquals("ram:///FSFilesManagerTest", result.getName().getURI());
//    }
//
//    @Test
//    public void testDeleteFile() throws Exception {
//        FileObject file = rootDir.resolveFile("tobedeleted");
//        boolean result = instance.deleteFile(file);
//
//        Assertions.assertTrue(result);
//        Assertions.assertFalse(file.exists());
//    }
//
//    @Test
//    public void testCloseAll(@Mocked final FileObject file1,
//                             @Mocked final FileObject file2,
//                             @Mocked final FileObject file3) throws FileSystemException {
//
//        new Expectations(instance) {{
//            file2.close();
//            result = new FileSystemException("Test-forced exception");
//        }};
//
//        instance.closeAll(new FileObject[]{file1, file2, file3});
//
//        new Verifications() {{
//            file1.close();
//            file2.close();
//            file3.close();
//        }};
//    }
//
//    @Test
//    public void testMoveFile() throws Exception {
//        FileObject file = rootDir.resolveFile("tobemoved");
//        FileObject targetFile = rootDir.resolveFile("targetfolder1/targetfolder2/moved");
//
//        long beforeMillis = System.currentTimeMillis();
//        instance.moveFile(file, targetFile);
//        long afterMillis = System.currentTimeMillis();
//
//        Assertions.assertTrue(targetFile.exists());
//        Assertions.assertTrue(targetFile.getContent().getLastModifiedTime() >= beforeMillis);
//        Assertions.assertTrue(targetFile.getContent().getLastModifiedTime() <= afterMillis);
//    }
//
//    @Test
//    public void testCreateFile() throws Exception {
//        try {
//            instance.createFile(rootDir, "tobecreated", "withcontent");
//        } catch (FileSystemException e) {
//            if ("File closed.".equals(e.getMessage())) {
//                LOG.trace("unit test workaround, file is being closed twice");
//            } else {
//                Assertions.fail();
//            }
//        }
//
//        Assertions.assertTrue(rootDir.resolveFile("tobecreated").exists());
//    }
//
//
//    @Test
//    public void hasLockFile(@Injectable FileObject file,
//                            @Injectable final FileObject lockFile) throws FileSystemException {
//        new Expectations(instance) {{
//            instance.resolveSibling(file, anyString);
//            result = lockFile;
//
//            lockFile.exists();
//            result = true;
//        }};
//
//        final boolean hasLockFile = instance.hasLockFile(file);
//
//        Assertions.assertTrue(hasLockFile);
//    }
//
//    @Test
//    public void createLockFile(@Injectable FileObject file,
//                               @Injectable final FileObject lockFile) throws FileSystemException {
//        new Expectations(instance) {{
//            instance.resolveSibling(file, anyString);
//            result = lockFile;
//        }};
//
//        instance.createLockFile(file);
//
//        new Verifications() {{
//            lockFile.createFile();
//        }};
//    }
//
//    @Test
//    public void deleteLockFile(@Injectable FileObject file,
//                               @Injectable final FileObject lockFile) throws FileSystemException {
//        new Expectations(instance) {{
//            instance.resolveSibling(file, anyString);
//            result = lockFile;
//
//            lockFile.exists();
//            result = true;
//        }};
//
//        instance.deleteLockFile(file);
//
//        new Verifications() {{
//            lockFile.delete();
//        }};
//    }
//
//    @Test
//    public void test_renameProcessedFile_Exception(final @Mocked FileObject processableFile) throws Exception {
//        final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
//        final String newFileName = "content_" + messageId + ".xml";
//
//        new Expectations() {{
//            fsFileNameHelper.deriveFileName("content.xml", messageId);
//            result = newFileName;
//
//            instance.renameFile(contentFile, newFileName);
//            result = new FileSystemException("Unable to rename the file");
//        }};
//
//        try {
//            //tested method
//            instance.renameProcessedFile(contentFile, messageId);
//            Assertions.fail("exception expected");
//        } catch (Exception e) {
//            Assertions.assertEquals(FSPluginException.class, e.getClass());
//        }
//    }
//
//    @Test
//    public void testHandleSendFailedMessage() throws FileSystemException, FSSetUpException, IOException {
//        final String domain = null; //root
//        final String errorMessage = "mock error";
//        final FileObject processableFile = contentFile;
//        new Expectations(instance) {{
//            instance.setUpFileSystem(domain);
//            result = rootDir;
//
//            instance.getEnsureChildFolder(rootDir, anyString);
//            result = rootDir.resolveFile("testfolder");
//
//            fsPluginProperties.isFailedActionDelete(domain);
//            result = true;
//        }};
//
//        instance.handleSendFailedMessage(processableFile, domain, errorMessage);
//
//        new Verifications() {{
//            instance.createFile((FileObject) any, anyString, anyString);
//        }};
//    }

}
