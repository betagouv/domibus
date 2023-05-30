package eu.domibus.plugin.fs.worker;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.fs.FSFilesManager;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
public abstract class FSAbstractPurgeService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FSAbstractPurgeService.class);

    @Autowired
    protected FSPluginProperties fsPluginProperties;

    @Autowired
    protected FSFilesManager fsFilesManager;

    @Autowired
    protected FSDomainService fsDomainService;

    /**
     * Triggering the purge means that the message files from the target directory
     * older than X seconds will be removed
     */
    public void purgeMessages() {
        final String domain = fsDomainService.getFSPluginDomain();

        purgeMessages(domain);
    }

    protected void purgeMessages(String domain) {
        if (!fsPluginProperties.getDomainEnabled(domain)) {
            LOG.debug("Domain [{}] is disabled for FSPlugin", domain);
            return;
        }

        FileObject[] contentFiles = null;
        try (FileObject rootDir = fsFilesManager.setUpFileSystem(domain);
             FileObject targetFolder = fsFilesManager.getEnsureChildFolder(rootDir, getTargetFolderName())) {

            contentFiles = findAllDescendants(targetFolder);
            LOG.debug("Found files [{}]", contentFiles);

            Integer expirationLimit = getExpirationLimit(domain);
            if (expirationLimit == null || expirationLimit <= 0) {
                LOG.debug("Expiration limit is null or not positive [{}]; no purging", expirationLimit);
                return;
            }

            for (FileObject processableFile : contentFiles) {
                checkAndPurge(processableFile, expirationLimit);
            }

        } catch (FileSystemException ex) {
            LOG.error("Error purging messages", ex);
        } catch (FSSetUpException ex) {
            LOG.error("Error setting up folders for domain: " + domain, ex);
        } finally {
            if (contentFiles != null) {
                fsFilesManager.closeAll(contentFiles);
            }
        }
    }

    protected abstract String getTargetFolderName();

    protected void checkAndPurge(FileObject file, Integer expirationLimit) {
        try {
            if (fsFilesManager.isFileOlderThan(file, expirationLimit)) {
                if (file.isFile()) {
                    LOG.debug("File [{}] is too old. Deleting", file.getName());
                    fsFilesManager.deleteFile(file);
                } else {
                    //it's folder
                    LOG.debug("Folder [{}] is too old. Deleting", file.getName());
                    fsFilesManager.deleteFolder(file);
                }
            } else {
                LOG.debug("File/folder [{}] is young enough. Keeping it", file.getName());
            }
        } catch (FileSystemException ex) {
            LOG.error("Error processing file " + file.getName().getURI(), ex);
        }
    }

    protected abstract Integer getExpirationLimit(String domain);

    /**
     * Returns all the files (or folders) to be deleted after a period ot time
     *
     * @param targetFolder folder to read all descendants
     * @return array of {@link FileObject}
     * @throws FileSystemException VFS exception
     */
    public FileObject[] findAllDescendants(final FileObject targetFolder) throws FileSystemException {
        return fsFilesManager.findAllDescendantFiles(targetFolder);
    }

}
