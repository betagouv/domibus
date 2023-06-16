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
public class FSPurgeReceivedServiceTest {

    @Tested
    private FSPurgeReceivedService instance;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private FSDomainService fsMultiTenancyService;

    @Injectable
    private DomibusConfigurationExtService domibusConfigurationExtService;

    private FileObject rootDir;
    private FileObject incomingFolder;
    private FileObject incomingFolderByRecipient;
    private FileObject oldIncomingFolderByMessageId;
    private FileObject recentIncomingFolderByMessageId;

    private FileObject oldFile;
    private FileObject recentFile;

    @BeforeEach
    public void setUp() throws IOException {
        String location = "ram:///FSPurgeReceivedServiceTest";

        FileSystemManager fsManager = VFS.getManager();
        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        incomingFolder = rootDir.resolveFile(FSFilesManager.INCOMING_FOLDER);
        incomingFolder.createFolder();

        incomingFolderByRecipient = incomingFolder.resolveFile("urn_oasis_names_tc_ebcore_partyid-type_unregistered_C4");
        incomingFolderByRecipient.createFolder();

        oldIncomingFolderByMessageId = incomingFolderByRecipient.resolveFile("3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");
        oldIncomingFolderByMessageId.createFolder();

        oldFile = oldIncomingFolderByMessageId.resolveFile("old_message.xml");
        oldFile.createFile();
        // set modified time to 30s ago
        oldFile.getContent().setLastModifiedTime(System.currentTimeMillis() - 30000);

        recentIncomingFolderByMessageId = incomingFolderByRecipient.resolveFile("3c5558e4-7b6d-11e7-bb31-be2e44b06b36@domibus.eu");
        recentIncomingFolderByMessageId.createFolder();
        recentFile = recentIncomingFolderByMessageId.resolveFile("recent_message.xml");
        recentFile.createFile();
    }

    @AfterEach
    public void tearDown() throws FileSystemException {
        rootDir.close();
        incomingFolder.close();

        incomingFolderByRecipient.close();
        oldIncomingFolderByMessageId.close();
        recentIncomingFolderByMessageId.close();
    }

    @Test
    public void testPurgeMessages() throws FileSystemException, FSSetUpException {
        final String domain = FSSendMessagesService.DEFAULT_DOMAIN;

        new Expectations(instance) {{

            fsMultiTenancyService.getFSPluginDomain();
            result = domain;

            fsPluginProperties.getDomainEnabled(domain);
            result = true;

            fsFilesManager.setUpFileSystem(FSSendMessagesService.DEFAULT_DOMAIN);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;

            instance.findAllDescendants(incomingFolder);
            result = new FileObject[]{recentFile, oldFile};

            fsPluginProperties.getReceivedPurgeExpired(FSSendMessagesService.DEFAULT_DOMAIN);
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
        new Expectations() {{

            fsMultiTenancyService.getFSPluginDomain();
            result = "DOMAIN1";

            fsPluginProperties.getDomainEnabled("DOMAIN1");
            result = true;

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
