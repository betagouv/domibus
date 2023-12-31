package eu.domibus.plugin.fs.worker;

import eu.domibus.ext.domain.JMSMessageDTOBuilder;
import eu.domibus.ext.domain.JmsMessageDTO;
import eu.domibus.ext.exceptions.AuthenticationExtException;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.services.AuthenticationExtService;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.JMSExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.fs.FSErrorMessageHelper;
import eu.domibus.plugin.fs.FSFileNameHelper;
import eu.domibus.plugin.fs.FSFilesManager;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Queue;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@Service
public class FSSendMessagesService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FSSendMessagesService.class);

    public static final String METADATA_FILE_NAME = "metadata.xml";
    public static final String DEFAULT_DOMAIN = "default";

    @Autowired
    protected FSPluginProperties fsPluginProperties;

    @Autowired
    protected FSFilesManager fsFilesManager;

    @Autowired
    protected FSProcessFileService fsProcessFileService;

    @Autowired
    protected AuthenticationExtService authenticationExtService;

    @Autowired
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    @Autowired
    protected DomainContextExtService domainContextExtService;

    @Autowired
    protected FSDomainService fsDomainService;

    @Autowired
    protected JMSExtService jmsExtService;

    @Autowired
    @Qualifier("fsPluginSendQueue")
    protected Queue fsPluginSendQueue;

    @Autowired
    protected FSFileNameHelper fsFileNameHelper;

    @Autowired
    protected FSErrorMessageHelper fsErrorMessageHelper;

    protected Map<String, FileInfo> observedFilesInfo = new HashMap<>();

    protected final Map<String, Optional<Pattern>> sendExcludeRegexPatternCache = new HashMap<>();

    /**
     * Triggering the send messages means that the message files from the OUT directory
     * will be processed to be sent
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendMessages() {
        final String domain = fsDomainService.getFSPluginDomain();

        if (!fsPluginProperties.getDomainEnabled(domain)) {
            LOG.debug("Domain [{}] is disabled for FSPlugin", domain);
            return;
        }

        LOG.debug("Sending file system messages...");

        sendMessagesSafely(domain);

        clearObservedFiles(domain);
    }

    @MDCKey(value = DomibusLogger.MDC_DOMAIN, cleanOnStart = true)
    protected void sendMessagesSafely(String domain) {
        if (StringUtils.isNotEmpty(domain)) {
            LOG.putMDC(DomibusLogger.MDC_DOMAIN, domain);
        }
        try {
            sendMessages(domain);
        } catch (AuthenticationExtException ex) {
            LOG.error("Authentication error for domain [{}]", domain, ex);
        }
    }

    protected void sendMessages(final String domain) {
        if (!fsPluginProperties.getDomainEnabled(domain)) {
            LOG.debug("Domain [{}] is disabled for FSPlugin", domain);
            return;
        }

        LOG.debug("Sending messages for domain [{}]", domain);

        authenticateForDomain(domain);

        FileObject[] contentFiles = null;
        try (FileObject rootDir = fsFilesManager.setUpFileSystem(domain);
             FileObject outgoingFolder = fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER)) {

            contentFiles = fsFilesManager.findAllDescendantFiles(outgoingFolder);
            LOG.trace("Found descendant files [{}] for output folder [{}]", contentFiles, outgoingFolder.getName().getPath());

            List<FileObject> processableFiles = filterProcessableFiles(outgoingFolder, contentFiles, domain);
            LOG.debug("Processable files [{}]", processableFiles);

            //we send the thread context manually since it will be lost in threads created by parallel stream
            Map<String, String> context = LOG.getCopyOfContextMap();
            processableFiles.parallelStream().forEach(file -> enqueueProcessableFileWithContext(file, context));

        } catch (FileSystemException ex) {
            LOG.error("Error sending messages", ex);
        } catch (FSSetUpException ex) {
            LOG.error("Error setting up folders for domain: " + domain, ex);
        } finally {
            if (contentFiles != null) {
                fsFilesManager.closeAll(contentFiles);
            }

            clearDomainContext();
            LOG.debug("Finished sending messages for domain [{}]", domain);
        }
    }

    protected void clearDomainContext() {
        LOG.removeMDC(DomibusLogger.MDC_USER);
        domainContextExtService.clearCurrentDomain();
    }

    /**
     * It will check authentication username and password presence
     *
     * @param domain
     */
    public void authenticateForDomain(String domain) throws AuthenticationExtException {

        if (!domibusConfigurationExtService.isSecuredLoginRequired()) {
            LOG.trace("Skip authentication for domain [{}]", domain);
            return;
        }

        String user = fsPluginProperties.getAuthenticationUser(domain);
        if (user == null) {
            LOG.error("Authentication User not defined for domain [{}]", domain);
            throw new AuthenticationExtException(DomibusErrorCode.DOM_002, "Authentication User not defined for domain [" + domain + "]");
        }

        String password = fsPluginProperties.getAuthenticationPassword(domain);
        if (password == null) {
            LOG.error("Authentication Password not defined for domain [{}]", domain);
            throw new AuthenticationExtException(DomibusErrorCode.DOM_002, "Authentication Password not defined for domain [" + domain + "]");
        }

        authenticationExtService.basicAuthenticate(user, password);
    }

    /**
     * process the file - to be called by JMS message listener
     *
     * @param processableFile
     * @param domain
     */
    public void processFileSafely(FileObject processableFile, String domain) {
        String errorMessage = null;
        try {
            fsProcessFileService.processFile(processableFile, domain);
        } catch (JAXBException ex) {
            errorMessage = fsErrorMessageHelper.buildErrorMessage("Invalid metadata file: " + ex.toString()).toString();
            LOG.error(errorMessage, ex);
        } catch (MessagingProcessingException | XMLStreamException ex) {
            errorMessage = fsErrorMessageHelper.buildErrorMessage("Error occurred submitting message to Domibus: " + ex.getMessage()).toString();
            LOG.error(errorMessage, ex);
        } catch (RuntimeException | IOException ex) {
            errorMessage = fsErrorMessageHelper.buildErrorMessage("Error processing file. Skipped it. Error message is: " + ex.getMessage()).toString();
            LOG.error(errorMessage, ex);
        } finally {
            if (errorMessage != null) {
                fsFilesManager.handleSendFailedMessage(processableFile, domain, errorMessage);
            }
        }
    }

    protected List<FileObject> filterProcessableFiles(FileObject rootFolder, FileObject[] files, String domain) {
        List<FileObject> filteredFiles = new LinkedList<>();

        List<String> lockedFileNames = Arrays.stream(files)
                .filter(f -> fsFileNameHelper.isLockFile(f.getName().getBaseName()))
                .map(f -> fsFileNameHelper.getRelativeName(rootFolder, f))
                .filter(Optional::isPresent)
                .map(fname -> fsFileNameHelper.stripLockSuffix(fname.get()))
                .collect(Collectors.toList());


        Optional<Pattern> sendExcludeRegexPattern = getSendExcludeRegexPattern(domain);

        for (FileObject file : files) {
            String fileName = file.getName().getBaseName();
            Optional<String> fileRelativePath = fsFileNameHelper.getRelativeName(rootFolder, file);

            if (!isMetadata(fileName)
                    && !fsFileNameHelper.isAnyState(fileName)
                    && !fsFileNameHelper.isProcessed(fileName)
                    // exclude lock files:
                    && !fsFileNameHelper.isLockFile(fileName)
                    // exclude locked files:
                    && !isLocked(lockedFileNames, fileRelativePath)
                    // exclude files based on send exclude regex
                    && !isExcludedFile(fileRelativePath, sendExcludeRegexPattern)
                    // exclude files that are (or could be) in use by other processes:
                    && canReadFileSafely(file, domain)) {
                filteredFiles.add(file);
            }
        }

        return filteredFiles;
    }

    protected boolean isMetadata(String baseName) {
        return StringUtils.equals(baseName, METADATA_FILE_NAME);
    }

    protected boolean isLocked(List<String> lockedFileNames, Optional<String> fileName) {
        return fileName.isPresent()
                && lockedFileNames.stream().anyMatch(fname -> fname.equals(fileName.get()));
    }

    protected boolean canReadFileSafely(FileObject fileObject, String domain) {
        String filePath = fileObject.getName().getPath();

        if (checkSizeChangedRecently(fileObject, domain)) {
            LOG.debug("Could not process file [{}] because its size has changed recently.", filePath);
            return false;
        }

        if (checkTimestampChangedRecently(fileObject, domain)) {
            LOG.debug("Could not process file [{}] because its timestamp has changed recently.", filePath);
            return false;
        }

        if (checkHasWriteLock(fileObject)) {
            LOG.debug("Could not process file [{}] because it has a write lock.", filePath);
            return false;
        }

        LOG.debug("Could read file [{}] successfully.", filePath);
        return true;
    }

    protected boolean checkSizeChangedRecently(FileObject fileObject, String domain) {
        long delta = fsPluginProperties.getSendDelay(domain);
        //disable check if delay is 0
        if (delta == 0) {
            return false;
        }
        String filePath = fileObject.getName().getPath();
        String key = filePath;
        try {
            long currentFileSize = fileObject.getContent().getSize();
            long currentTime = new Date().getTime();

            FileInfo fileInfo = observedFilesInfo.get(key);
            if (fileInfo == null || fileInfo.getSize() != currentFileSize) {
                observedFilesInfo.put(key, new FileInfo(currentFileSize, currentTime, domain));
                LOG.debug("Could not process file [{}] because its size has changed recently", filePath);
                return true;
            }

            long elapsed = currentTime - fileInfo.getModified(); // time passed since last size change
            // if the file size has changed recently, probably some process is still writing to the file
            if (elapsed < delta) {
                LOG.debug("Could not process file [{}] because its size has changed recently: [{}] ms", filePath, elapsed);
                return true;
            }
        } catch (FileSystemException e) {
            LOG.warn("Could not determine file info for file [{}] ", filePath, e);
            return true;
        }

        return false;
    }

    protected void clearObservedFiles(String domain) {
        LOG.trace("Starting clear of the observed files for domain [{}]; there are [{}] entries", domain, observedFilesInfo.size());

        int delta = 2 * fsPluginProperties.getSendWorkerInterval(domain) + fsPluginProperties.getSendDelay(domain);
        long currentTime = new Date().getTime();
        String[] keys = observedFilesInfo.keySet().toArray(new String[]{});
        for (String key : keys) {
            FileInfo fileInfo = observedFilesInfo.get(key);
            if (fileInfo.getDomain().equals(domain) && ((currentTime - fileInfo.getModified()) > delta)) {
                LOG.debug("File [{}] is old and will not be observed anymore", key);
                observedFilesInfo.remove(key);
            }
        }

        LOG.trace("Ending clear of the observed files for domain [{}]; there are [{}] entries", domain, observedFilesInfo.size());
    }

    protected boolean checkTimestampChangedRecently(FileObject fileObject, String domain) {
        long delta = fsPluginProperties.getSendDelay(domain);
        //disable check if delay is 0
        if (delta == 0) {
            return false;
        }
        String filePath = fileObject.getName().getPath();
        try {
            long fileTime = fileObject.getContent().getLastModifiedTime();
            long elapsed = new Date().getTime() - fileTime; // time passed since last file change
            // if the file timestamp is very recent it is probable that some process is still writing in the file
            if (elapsed < delta) {
                LOG.debug("Could not process file [{}] because it is too recent: [{}] ms", filePath, elapsed);
                return true;
            }
        } catch (FileSystemException e) {
            LOG.warn("Could not determine file date for file [{}] ", filePath, e);
            return true;
        }
        return false;
    }

    protected boolean checkHasWriteLock(FileObject fileObject) {
        // firstly try to lock the file
        // if this fails, it means that another process has an explicit lock on the file
        String filePath;
        if (fileObject.getName().getURI().startsWith("file://")) {
            //handle files that may be located on a different disk partition
            filePath = fileObject.getPath().toString();
            LOG.debug("Special case handling for acquiring lock on file: [{}] ", filePath);
        } else {
            filePath = fileObject.getName().getPath();
        }
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
             FileChannel fileChannel = raf.getChannel();
             FileLock lock = fileChannel.tryLock(0, 0, true)) {
            if (lock == null) {
                LOG.debug("Could not acquire lock on file [{}] ", filePath);
                return true;
            }
        } catch (Exception e) {
            LOG.debug("Could not acquire lock on file [{}] ", filePath, e);
            return true;
        }
        return false;
    }

    protected void enqueueProcessableFileWithContext(final FileObject fileObject, final Map<String, String> context) {
        if (context != null) {
            LOG.setContextMap(context);
        }

        this.enqueueProcessableFile(fileObject);
    }

    /**
     * Put a JMS message to FS Plugin Send queue
     *
     * @param fileObject
     */
    protected void enqueueProcessableFile(final FileObject fileObject) {

        String fileName;
        try {
            fileName = fileObject.getURL().getFile();
        } catch (FileSystemException e) {
            LOG.error("Exception while getting filename: ", e);
            return;
        }

        try {
            if (fsFilesManager.hasLockFile(fileObject)) {
                LOG.debug("Skipping file [{}]: it has a lock file associated", fileName);
                return;
            }
            fsFilesManager.createLockFile(fileObject);
        } catch (FileSystemException e) {
            LOG.error("Exception while checking file lock: ", e);
            return;
        }

        final JmsMessageDTO jmsMessage = JMSMessageDTOBuilder.
                create().
                property(MessageConstants.FILE_NAME, fileName).
                build();

        LOG.debug("send message: [{}] to fsPluginSendQueue for file: [{}]", jmsMessage, fileName);
        jmsExtService.sendMessageToQueue(jmsMessage, fsPluginSendQueue);
    }

    protected Optional<Pattern> getSendExcludeRegexPattern(String domain) {
        if (this.sendExcludeRegexPatternCache.containsKey(domain)) {
            return sendExcludeRegexPatternCache.get(domain);
        }
        synchronized (this.sendExcludeRegexPatternCache) {
            // check again in case the pattern was created by another thread
            if (this.sendExcludeRegexPatternCache.containsKey(domain)) {
                return sendExcludeRegexPatternCache.get(domain);
            }
            return createSendExcludeRegexPattern(domain);
        }
    }

    protected Optional<Pattern> createSendExcludeRegexPattern(String domain) {
        String sendExcludeRegex = fsPluginProperties.getSendExcludeRegex(domain);
        LOG.debug("sendExcludeRegex for domain [{}] is [{}]", domain, sendExcludeRegex);
        Optional<Pattern> result;
        if (StringUtils.isNotBlank(sendExcludeRegex)) {
            result = Optional.of(Pattern.compile(sendExcludeRegex));
        } else {
            result = Optional.empty();
        }
        sendExcludeRegexPatternCache.put(domain, result);
        return result;
    }

    protected boolean isExcludedFile(Optional<String> relativeFileName, Optional<Pattern> sendExcludeRegexPattern) {
        boolean isExcluded;
        if ((!relativeFileName.isPresent()) || (!sendExcludeRegexPattern.isPresent())) {
            isExcluded = false;
        } else {
            isExcluded = sendExcludeRegexPattern.get().matcher(relativeFileName.get()).find();
        }
        LOG.info("Checking if file is excluded. relativeFileName: [{}] isExcluded: [{}]", relativeFileName, isExcluded);
        return isExcluded;
    }
}