package eu.domibus.plugin.fs.worker;

import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.plugin.fs.FSFilesManager;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@ExtendWith(JMockitExtension.class)
public class FSPurgeFailedServiceTest {

    @Tested
    private FSPurgeFailedService instance;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private FSDomainService fsMultiTenancyService;

    @Injectable
    private DomibusConfigurationExtService domibusConfigurationExtService;

    private FileObject rootDir;
    private FileObject failedFolder;
    private FileObject oldFile;
    private FileObject recentFile;

    @BeforeEach
    public void setUp() throws IOException {
        String location = "ram:///FSPurgeFailedServiceTest";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        failedFolder = rootDir.resolveFile(FSFilesManager.FAILED_FOLDER);
        failedFolder.createFolder();

        oldFile = failedFolder.resolveFile("old_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.xml");
        oldFile.createFile();
        // set modified time to 30s ago
        oldFile.getContent().setLastModifiedTime(System.currentTimeMillis() - 30000);

        recentFile = failedFolder.resolveFile("recent_3c5558e4-7b6d-11e7-bb31-be2e44b06b36@domibus.eu.xml");
        recentFile.createFile();
    }

    @AfterEach
public void tearDown() throws FileSystemException {
        rootDir.close();
        failedFolder.close();
    }

    @Test
    public void testPurgeMessages() throws FileSystemException, FSSetUpException {
        final String domain = FSSendMessagesService.DEFAULT_DOMAIN;

        new Expectations( instance) {{
            fsPluginProperties.getDomainEnabled(domain);
            result = true;

            fsMultiTenancyService.getFSPluginDomain();
            result = domain;

            fsFilesManager.setUpFileSystem(FSSendMessagesService.DEFAULT_DOMAIN);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.FAILED_FOLDER);
            result = failedFolder;

            fsFilesManager.findAllDescendantFiles(failedFolder);
            result = new FileObject[]{recentFile, oldFile};

            fsPluginProperties.getFailedPurgeExpired(FSSendMessagesService.DEFAULT_DOMAIN);
            result = 20;

            fsFilesManager.isFileOlderThan(recentFile, 20);
            result = false;

            fsFilesManager.isFileOlderThan(oldFile, 20);
            result = true;
        }};

        instance.purgeMessages();

        new VerificationsInOrder() {{
            fsFilesManager.deleteFile(oldFile);
        }};
    }

    @Test
    public void testPurgeMessages_Domain1_BadConfiguration() throws FileSystemException, FSSetUpException {
        new Expectations( instance) {{
            fsPluginProperties.getDomainEnabled("DOMAIN1");
            result = true;

            fsMultiTenancyService.getFSPluginDomain();
            result = "DOMAIN1";

            fsFilesManager.setUpFileSystem("DOMAIN1");
            result = new FSSetUpException("Test-forced exception");
        }};

        instance.purgeMessages();

        new Verifications() {{
            fsFilesManager.deleteFile(withAny(oldFile));
            maxTimes = 0;
        }};
    }

}
