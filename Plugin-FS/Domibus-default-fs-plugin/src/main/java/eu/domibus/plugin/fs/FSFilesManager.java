package eu.domibus.plugin.fs;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.fs.exception.FSPluginException;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import eu.domibus.plugin.fs.vfs.FileObjectDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * This class is responsible for performing complex operations using VFS
 *
 * @author FERNANDES Henrique, GONCALVES Bruno
 * @author Cosmin Baciu
 */
@Component
public class FSFilesManager {
    public static final String ERROR_EXTENSION = ".error";
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FSFilesManager.class);

    private static final String FTP_PREFIX = "ftp:";
    private static final String PARENT_RELATIVE_PATH = "../";
    private static final int TEN_SECONDS = 10000;

    public static final String INCOMING_FOLDER = "IN";
    public static final String OUTGOING_FOLDER = "OUT";
    public static final String SENT_FOLDER = "SENT";
    public static final String FAILED_FOLDER = "FAILED";

    protected final FSPluginProperties fsPluginProperties;

    protected final FSFileNameHelper fsFileNameHelper;

    public FSFilesManager(FSPluginProperties fsPluginProperties,
                          FSFileNameHelper fsFileNameHelper) {
        this.fsPluginProperties = fsPluginProperties;
        this.fsFileNameHelper = fsFileNameHelper;
    }

    public FileObject getEnsureRootLocation(final String location,
                                            final String domain,
                                            final String user,
                                            final String password) throws FileSystemException {
        StaticUserAuthenticator auth = new StaticUserAuthenticator(domain, user, password);
        FileSystemOptions opts = new FileSystemOptions();
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);

        /*
         * This is a workaround for a VFS issue regarding FTP servers on Linux.
         * See https://issues.apache.org/jira/browse/VFS-620
         * Disabling this property forces usage of paths starting at the root
         * of the filesystem which sidesteps the problem.
         * We apply only to FTP URLs since the property applies to SFTP too but
         * that protocol works as intended.
         */
        if (location.startsWith(FTP_PREFIX)) {
            FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
        }
        FtpFileSystemConfigBuilder.getInstance().setConnectTimeout(opts, TEN_SECONDS);
        FtpFileSystemConfigBuilder.getInstance().setDataTimeout(opts, TEN_SECONDS);
        FtpFileSystemConfigBuilder.getInstance().setSoTimeout(opts, TEN_SECONDS);
        SftpFileSystemConfigBuilder.getInstance().setSessionTimeoutMillis(opts, TEN_SECONDS);

        FileSystemManager fsManager = getVFSManager();
        FileObject rootDir = fsManager.resolveFile(location, opts);
        checkRootDirExists(rootDir);

        return rootDir;
    }

    protected void checkRootDirExists(FileObject rootDir) throws FileSystemException {
        if (!rootDir.exists()) {
            throw new FSSetUpException("Root location does not exist: " + rootDir.getName());
        }
    }

    public FileObject getEnsureRootLocation(final String location) throws FileSystemException {
        FileSystemManager fsManager = getVFSManager();
        FileObject rootDir = fsManager.resolveFile(location);
        checkRootDirExists(rootDir);
        return rootDir;
    }

    protected FileSystemManager getVFSManager() throws FileSystemException {
        return VFS.getManager();
    }

    public FileObject getEnsureChildFolder(FileObject rootDir, String folderName) {
        try {
            checkRootDirExists(rootDir);
            FileObject outgoingDir = rootDir.resolveFile(folderName);
            if (!outgoingDir.exists()) {
                outgoingDir.createFolder();
            } else {
                if (outgoingDir.getType() != FileType.FOLDER) {
                    throw new FSSetUpException("Child path exists and is not a folder");
                }
            }
            return outgoingDir;
        } catch (FileSystemException ex) {
            throw new FSSetUpException("IO error setting up folders", ex);
        }
    }

    public FileObject[] findAllDescendantFiles(FileObject folder) throws FileSystemException {
        return folder.findFiles(new FileTypeSelector(FileType.FILE));
    }

    public FileObject[] findAllDescendantFiles(FileObject folder, FileType fileType) throws FileSystemException {
        return folder.findFiles(new FileTypeSelector(fileType));
    }

    public DataHandler getDataHandler(FileObject file) {
        return new DataHandler(new FileObjectDataSource(file));
    }

    public FileObject resolveSibling(FileObject file, String siblingName) throws FileSystemException {
        return file.resolveFile(PARENT_RELATIVE_PATH + siblingName);
    }

    /**
     * Checks if a lock file exists for a given file
     *
     * @param file The original file for which the lock file is checked
     * @return true if a lock file exists
     * @throws FileSystemException On error parsing the path, or on error finding the file.
     */
    public boolean hasLockFile(FileObject file) throws FileSystemException {
        try (final FileObject lockFile = resolveSibling(file, fsFileNameHelper.getLockFilename(file))) {
            LOG.debug("Checking if lock file exists [{}]", file.getName().getURI());
            final boolean exists = lockFile.exists();
            LOG.debug("Lock file [{}] exists? [{}]", file.getName().getURI(), exists);
            return exists;
        }
    }

    /**
     * Creates a lock file associated to a given file. For instance it will create invoice.pdf.lock for a file named invoice.pdf
     *
     * @param file The original file for which the  lock file is created
     * @return the lock file
     * @throws FileSystemException On error parsing the path, or on error finding the file.
     */
    public FileObject createLockFile(FileObject file) throws FileSystemException {
        try (final FileObject lockFile = resolveSibling(file, fsFileNameHelper.getLockFilename(file))) {
            LOG.debug("Creating lock file for [{}]", file.getName().getBaseName());
            lockFile.createFile();
            return lockFile;
        }
    }

    /**
     * Deletes the lock file associated to a given file
     *
     * @param file The original file for which the lock file is deleted
     * @return true if the lock file has been deleted
     * @throws FileSystemException On error parsing the path, or on error finding the file.
     */
    public boolean deleteLockFile(FileObject file) throws FileSystemException {
        try (final FileObject lockFile = resolveSibling(file, fsFileNameHelper.getLockFilename(file))) {
            if (lockFile.exists()) {
                lockFile.close();
                LOG.debug("Deleting lock file for [{}]", file.getName().getBaseName());
                return lockFile.delete();
            } else {
                LOG.debug("Lock file for [{}] not found", file.getName().getBaseName());
            }
        }
        return false;
    }


    public FileObject renameFile(FileObject file, String newFileName) throws FileSystemException {
        LOG.debug("Renaming file [{}] to [{}]", file.getName().getPath(), newFileName);

        try (FileObject newFile = resolveSibling(file, newFileName)) {
            //Close open handles on the file before rename.
            file.close();
            file.moveTo(newFile);

            forceLastModifiedTimeIfSupported(newFile);

            return newFile;
        }
    }

    public void moveFile(FileObject file, FileObject targetFile) throws FileSystemException {
        //Close open handles on the file before rename.
        file.close();
        file.moveTo(targetFile);

        forceLastModifiedTimeIfSupported(targetFile);
    }

    private void forceLastModifiedTimeIfSupported(FileObject file) throws FileSystemException {
        if (file.getFileSystem().hasCapability(Capability.SET_LAST_MODIFIED_FILE)) {
            try (FileContent fileContent = file.getContent()) {
                long currentTimeMillis = System.currentTimeMillis();
                fileContent.setLastModifiedTime(currentTimeMillis);
                LOG.debug("Forced LastModifiedTime for file [{}] to [{}]", file.getName().getPath(), currentTimeMillis);
            }
        }
    }

    public boolean deleteFile(FileObject file) throws FileSystemException {
        file.close();
        return file.delete();
    }

    public boolean deleteFolder(FileObject file) throws FileSystemException {
        file.close();
        return (file.deleteAll() > 0L);
    }

    public FileObject setUpFileSystem(String domain) throws FileSystemException {
        // Domain or default location
        String location = fsPluginProperties.getLocation(domain);
        if (StringUtils.isBlank(location)) {
            throw new FSSetUpException("Location folder is not set for domain=[" + domain + "].");
        }

        String authDomain = null;
        String user = fsPluginProperties.getUser(domain);
        String password = fsPluginProperties.getPassword(domain);

        FileObject rootDir;
        if (StringUtils.isEmpty(user) || StringUtils.isEmpty(password)) {
            rootDir = getEnsureRootLocation(location);
        } else {
            rootDir = getEnsureRootLocation(location, authDomain, user, password);
        }
        return rootDir;
    }

    public void closeAll(FileObject[] files) {
        for (FileObject file : files) {
            try {
                file.close();
            } catch (FileSystemException ex) {
                // errors with close are not very important at this point
                // just log in case there's an underlying problem
                LOG.warn("Error closing file :[{}]", file.getName(), ex);
            }
        }
    }

    /**
     * Creates a file in the directory with the given file name and content.
     *
     * @param directory base directory
     * @param fileName  file name
     * @param content   content
     */
    public void createFile(FileObject directory, String fileName, String content) throws IOException {
        try (OutputStreamWriter fileOSW = new OutputStreamWriter(directory.resolveFile(fileName).getContent().getOutputStream())) {
            fileOSW.write(content);
        }
    }

    public boolean fileExists(FileObject rootDir, String fileName) throws FileSystemException {
        //the data files can be located in folders along with their metadata so we search deep
        try (FileObject file = rootDir.resolveFile(fileName, NameScope.DESCENDENT)) {
            return file.exists();
        }
    }

    public boolean isFileOlderThan(FileObject file, Integer ageInSeconds) {
        if (ageInSeconds == null || ageInSeconds <= 0) {
            LOG.debug("Expiration limit is null or not positive; exiting");
            return false;
        }

        long currentMillis = System.currentTimeMillis();
        long modifiedMillis = 0;
        try {
            modifiedMillis = file.getContent().getLastModifiedTime();
        } catch (FileSystemException ex) {
            LOG.error("Error reading last modified time.", ex);
            return false;
        }
        long fileAgeSeconds = (currentMillis - modifiedMillis) / 1000;

        return fileAgeSeconds > ageInSeconds;
    }

    public void renameProcessedFile(FileObject processableFile, String messageId) {
        final String baseName = processableFile.getName().getBaseName();
        String newFileName = fsFileNameHelper.deriveFileName(baseName, messageId);

        LOG.debug("Renaming file [{}] to [{}]", baseName, newFileName);

        try {
            renameFile(processableFile, newFileName);
        } catch (FileSystemException ex) {
            throw new FSPluginException("Error renaming file [" + processableFile.getName().getURI() + "] to [" + newFileName + "]", ex);
        }
    }

    public void handleSendFailedMessage(FileObject processableFile, String domain, String errorMessage) {
        if (processableFile == null) {
            LOG.error("The send failed message file was not found in domain [{}]", domain);
            return;
        }
        try {
            deleteLockFile(processableFile);
        } catch (FileSystemException e) {
            LOG.error("Error deleting lock file", e);
        }

        try (FileObject rootDir = setUpFileSystem(domain)) {
            String baseName = processableFile.getName().getBaseName();
            String errorFileName = fsFileNameHelper.stripStatusSuffix(baseName) + ERROR_EXTENSION;

            String processableFileMessageURI = processableFile.getParent().getName().getPath();
            String failedDirectoryLocation = fsFileNameHelper.deriveFailedDirectoryLocation(processableFileMessageURI);
            FileObject failedDirectory = getEnsureChildFolder(rootDir, failedDirectoryLocation);

            try {
                if (fsPluginProperties.isFailedActionDelete(domain)) {
                    // Delete
                    deleteFile(processableFile);
                    LOG.debug("Send failed message file [{}] was deleted", processableFile.getName().getBaseName());
                } else if (fsPluginProperties.isFailedActionArchive(domain)) {
                    // Archive
                    String archivedFileName = fsFileNameHelper.stripStatusSuffix(baseName);
                    FileObject archivedFile = failedDirectory.resolveFile(archivedFileName);
                    moveFile(processableFile, archivedFile);
                    LOG.debug("Send failed message file [{}] was archived into [{}]", processableFile, archivedFile.getName().getURI());
                }
            } finally {
                // Create error file
                createFile(failedDirectory, errorFileName, errorMessage);
            }
        } catch (IOException e) {
            throw new FSPluginException("Error handling the send failed message file " + processableFile, e);
        }
    }
}
