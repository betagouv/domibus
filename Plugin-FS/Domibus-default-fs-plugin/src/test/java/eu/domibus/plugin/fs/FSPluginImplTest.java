package eu.domibus.plugin.fs;

import eu.domibus.common.*;
import eu.domibus.ext.services.*;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.fs.ebms3.UserMessage;
import eu.domibus.plugin.fs.exception.FSPluginException;
import eu.domibus.plugin.fs.exception.FSSetUpException;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import eu.domibus.plugin.fs.queue.FSSendMessageListenerContainer;
import eu.domibus.plugin.fs.worker.FSDomainService;
import eu.domibus.plugin.fs.worker.FSProcessFileService;
import eu.domibus.plugin.fs.worker.FSSendMessagesService;
import eu.domibus.plugin.handler.MessagePuller;
import eu.domibus.plugin.handler.MessageRetriever;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.plugin.transformer.MessageRetrievalTransformer;
import eu.domibus.plugin.transformer.MessageSubmissionTransformer;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.tika.mime.MimeTypeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class FSPluginImplTest {

    private static final String TEXT_XML = "text/xml";

    @Injectable
    protected MessageRetriever messageRetriever;

    @Injectable
    protected MessageSubmitter messageSubmitter;

    @Injectable
    protected MessagePuller messagePuller;

    @Injectable
    private FSFilesManager fsFilesManager;

    @Injectable
    private FSPluginProperties fsPluginProperties;

    @Injectable
    protected FSErrorMessageHelper fsErrorMessageHelper;

    @Injectable
    private FSMessageTransformer defaultTransformer;

    @Injectable
    private FileUtilExtService fileUtilExtService;

    @Injectable
    private DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    private DomainExtService domainExtService;

    @Injectable
    private DomainContextExtService domainContextExtService;

    @Injectable
    private FSSendMessagesService fsSendMessagesService;

    @Injectable
    private MessageExtService messageExtService;

    @Injectable
    protected DomainTaskExtExecutor domainTaskExtExecutor;

    @Injectable
    protected FSDomainService fsDomainService;

    @Injectable
    String name = "fsplugin";

    @Injectable
    FSXMLHelper fsxmlHelper;

    @Injectable
    protected FSMimeTypeHelper fsMimeTypeHelper;

    @Injectable
    protected FSFileNameHelper fsFileNameHelper;

    @Injectable
    BackendConnectorProviderExtService backendConnectorProviderExtService;

    @Injectable
    DomibusPropertyManagerExt domibusPropertyManagerExt;

    @Injectable
    protected DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    FSSendMessageListenerContainer fsSendMessageListenerContainer;

    @Tested
    FSPluginImpl backendFS;

    private FileObject rootDir;

    private FileObject incomingFolder;

    private FileObject incomingFolderByRecipient, incomingFolderByMessageId;

    private FileObject outgoingFolder;

    private FileObject sentFolder;

    private FileObject failedFolder;

    private final String location = "ram:///BackendFSImplTest";
    private final String messageId = "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu";
    private final String finalRecipientFolder = "urn_oasis_names_tc_ebcore_partyid-type_unregistered_C4";
    private DeliverMessageEvent messageEvent = new DeliverMessageEvent(123, messageId, new HashMap<>());
    private final String messageIdFolder = messageId;

    @BeforeEach
    public void setUp() throws org.apache.commons.vfs2.FileSystemException {
        FileSystemManager fsManager = VFS.getManager();

        rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        incomingFolder = rootDir.resolveFile(FSFilesManager.INCOMING_FOLDER);
        incomingFolder.createFolder();


        incomingFolderByRecipient = incomingFolder.resolveFile(finalRecipientFolder);
        incomingFolderByRecipient.createFolder();

        incomingFolderByMessageId = incomingFolderByRecipient.resolveFile(messageId);
        incomingFolderByMessageId.createFolder();

        outgoingFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
        outgoingFolder.createFolder();

        sentFolder = rootDir.resolveFile(FSFilesManager.SENT_FOLDER);
        sentFolder.createFolder();

        failedFolder = rootDir.resolveFile(FSFilesManager.FAILED_FOLDER);
        failedFolder.createFolder();

        final HashMap<String, String> properties = new HashMap<>();
        properties.put(MessageConstants.FINAL_RECIPIENT, finalRecipientFolder);
        messageEvent = new DeliverMessageEvent(123, messageId, properties);
    }

    @AfterEach
    public void tearDown() throws FileSystemException {
        incomingFolder.close();
        incomingFolderByRecipient.close();
        incomingFolderByMessageId.close();

        outgoingFolder.close();
        sentFolder.close();

        rootDir.close();
        rootDir.deleteAll();
    }

    @Test
    public void testDeliverMessage_NormalFlow(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {
        ReflectionTestUtils.setField(backendFS, "fileUtilExtService", new FileUtilExtService() {
            @Override
            public String sanitizeFileName(String fileName) {
                return fileName;
            }

            @Override
            public String urlEncode(String s) {
                return s;
            }
        });
        final String payloadFileName = "message_test.xml";
        final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(payloadContent.getBytes(), TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, payloadFileName, dataHandler));

        expectationsDeliverMessage(FSSendMessagesService.DEFAULT_DOMAIN, userMessage, fsPayloads);

        backendFS.deliverMessage(messageEvent);

        // Assert results
        FileObject[] files = incomingFolderByMessageId.findFiles(new FileTypeSelector(FileType.FILE));

        Assertions.assertEquals(2, files.length);

        //metadata first
        FileObject metadataFile = files[0];

        Assertions.assertEquals(FSSendMessagesService.METADATA_FILE_NAME, metadataFile.getName().getBaseName());


        UserMessage expectedUserMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        new Verifications() {{
            UserMessage savedUserMessage;
            fsxmlHelper.writeXML((OutputStream) any, UserMessage.class, savedUserMessage = withCapture());
            Assertions.assertEquals(expectedUserMessage, savedUserMessage);
        }};


        metadataFile.close();
        metadataFile.delete();

        //payload
        FileObject payloadFile = files[1];

        Assertions.assertEquals(payloadFileName, payloadFile.getName().getBaseName());
        Assertions.assertEquals(payloadContent, IOUtils.toString(payloadFile.getContent().getInputStream(), StandardCharsets.UTF_8));
        payloadFile.close();
        payloadFile.delete();
    }

    private void expectationsDeliverMessage(String domain, UserMessage userMessage, Map<String, FSPayload> fsPayloads) throws MessageNotFoundException, FileSystemException {
        new Expectations(backendFS) {{
            backendFS.browseMessage(messageId, MSHRole.RECEIVING, null);
            result = new FSMessage(fsPayloads, userMessage);

            backendFS.downloadMessage(messageId, null);
            result = new FSMessage(fsPayloads, userMessage);

            fsFilesManager.setUpFileSystem(domain);
            result = rootDir;

            fsDomainService.getFSPluginDomain();
            result = FSSendMessagesService.DEFAULT_DOMAIN;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;

            fsFilesManager.getEnsureChildFolder(incomingFolder, finalRecipientFolder);
            result = incomingFolderByRecipient;

            fsFilesManager.getEnsureChildFolder(incomingFolderByRecipient, messageIdFolder);
            result = incomingFolderByMessageId;

            backendFS.getFileNameExtension(TEXT_XML);
            result = ".xml";

            backendFS.isEnabled(anyString);
            result = true;
        }};
    }

    @Test
    public void testDeliverMessage_MultiplePayloads(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {
        ReflectionTestUtils.setField(backendFS, "fileUtilExtService", new FileUtilExtService() {
            @Override
            public String sanitizeFileName(String fileName) {
                return fileName;
            }

            @Override
            public String urlEncode(String s) {
                return s;
            }
        });
        final UserMessage userMessage = FSTestHelper.getUserMessage(FSPluginImplTest.class, "testDeliverMessageNormalFlow_metadata.xml");
        final String messageContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGludm9pY2U+aGVsbG88L2ludm9pY2U+";
        final String invoiceContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";


        final DataHandler messageHandler = new DataHandler(new ByteArrayDataSource(messageContent.getBytes(), TEXT_XML));
        final DataHandler invoiceHandler = new DataHandler(new ByteArrayDataSource(invoiceContent.getBytes(), TEXT_XML));
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, "message.xml", messageHandler));
        fsPayloads.put("cid:invoice", new FSPayload(TEXT_XML, "invoice.xml", invoiceHandler));

        expectationsDeliverMessage(FSSendMessagesService.DEFAULT_DOMAIN, userMessage, fsPayloads);

        //tested method
        backendFS.deliverMessage(messageEvent);

        // Assert results
        FileObject[] files = incomingFolderByMessageId.findFiles(new FileTypeSelector(FileType.FILE));
        Assertions.assertEquals(3, files.length);

        FileObject fileMetadata = files[0];
        Assertions.assertEquals(FSSendMessagesService.METADATA_FILE_NAME,
                fileMetadata.getName().getBaseName());

        UserMessage expectedUserMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        new Verifications() {{
            UserMessage savedUserMessage;
            fsxmlHelper.writeXML((OutputStream) any, UserMessage.class, savedUserMessage = withCapture());
            Assertions.assertEquals(expectedUserMessage, savedUserMessage);
        }};


        fileMetadata.delete();
        fileMetadata.close();

        FileObject fileMessage0 = files[1];
        Assertions.assertEquals("message.xml",
                fileMessage0.getName().getBaseName());
        Assertions.assertEquals(messageContent, IOUtils.toString(fileMessage0.getContent().getInputStream(), StandardCharsets.UTF_8));
        fileMessage0.close();
        fileMessage0.delete();

        FileObject fileMessage1 = files[2];
        Assertions.assertEquals("invoice.xml",
                fileMessage1.getName().getBaseName());
        Assertions.assertEquals(invoiceContent, IOUtils.toString(fileMessage1.getContent().getInputStream(), StandardCharsets.UTF_8));
        fileMessage1.close();
        fileMessage1.delete();
    }

    @Test
    public void testDeliverMessage_MultiplePayloads_WrongPayloadNames(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {
        ReflectionTestUtils.setField(backendFS, "fileUtilExtService", new FileUtilExtService() {
            @Override
            public String sanitizeFileName(String fileName) {
                return fileName;
            }

            @Override
            public String urlEncode(String s) {
                return s;
            }
        });
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final String messageContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGludm9pY2U+aGVsbG88L2ludm9pY2U+";
        final String invoiceContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";


        final DataHandler messageHandler = new DataHandler(new ByteArrayDataSource(messageContent.getBytes(), TEXT_XML));
        final DataHandler invoiceHandler = new DataHandler(new ByteArrayDataSource(invoiceContent.getBytes(), TEXT_XML));
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message2", new FSPayload(TEXT_XML, "./../message.xml", messageHandler));
        fsPayloads.put("cid:invoice2", new FSPayload(TEXT_XML, ".%2F..%2Finvoice.xml", invoiceHandler));

        expectationsDeliverMessage(FSSendMessagesService.DEFAULT_DOMAIN, userMessage, fsPayloads);

        //tested method
        backendFS.deliverMessage(messageEvent);

        // Assert results
        FileObject[] files = incomingFolderByMessageId.findFiles(new FileTypeSelector(FileType.FILE));
        Assertions.assertEquals(3, files.length);

        FileObject fileMetadata = files[0];
        Assertions.assertEquals(FSSendMessagesService.METADATA_FILE_NAME,
                fileMetadata.getName().getBaseName());

        UserMessage expectedUserMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        new Verifications() {{
            UserMessage savedUserMessage;
            fsxmlHelper.writeXML((OutputStream) any, UserMessage.class, savedUserMessage = withCapture());
            Assertions.assertEquals(expectedUserMessage, savedUserMessage);
        }};


        fileMetadata.delete();
        fileMetadata.close();

        FileObject fileMessage0 = files[1];
        Assertions.assertEquals("message2.xml",
                fileMessage0.getName().getBaseName());
        Assertions.assertEquals(messageContent, IOUtils.toString(fileMessage0.getContent().getInputStream(), StandardCharsets.UTF_8));
        fileMessage0.close();
        fileMessage0.delete();

        FileObject fileMessage1 = files[2];
        Assertions.assertEquals("invoice2.xml",
                fileMessage1.getName().getBaseName());
        Assertions.assertEquals(invoiceContent, IOUtils.toString(fileMessage1.getContent().getInputStream(), StandardCharsets.UTF_8));
        fileMessage1.close();
        fileMessage1.delete();
    }

    @Test
    void testDeliverMessage_MessageNotFound(@Injectable final FSMessage fsMessage) throws MessageNotFoundException {

        new Expectations(backendFS) {{
            backendFS.browseMessage(messageId, MSHRole.RECEIVING, null);
            result = new MessageNotFoundException("message not found");

            backendFS.isEnabled(anyString);
            result = true;
        }};

        Assertions.assertThrows(FSPluginException.class, () -> backendFS.deliverMessage(messageEvent));
    }

    @Test
    void testDeliverMessage_FSSetUpException(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        final String payloadContent = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=";
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(payloadContent.getBytes(), TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, "message.xml", dataHandler));

        new Expectations(backendFS) {{
            backendFS.browseMessage(messageId, MSHRole.RECEIVING, null);
            result = new FSMessage(fsPayloads, userMessage);

            fsDomainService.getFSPluginDomain();
            result = FSSendMessagesService.DEFAULT_DOMAIN;

            backendFS.isEnabled(anyString);
            result = true;

            fsFilesManager.setUpFileSystem(FSSendMessagesService.DEFAULT_DOMAIN);
            result = new FSSetUpException("Test-forced exception");
        }};

        Assertions.assertThrows(FSPluginException.class, () -> backendFS.deliverMessage(messageEvent));
    }

    @Test
    void testDeliverMessage_IOException(@Injectable final FSMessage fsMessage)
            throws MessageNotFoundException, JAXBException, IOException, FSSetUpException {

        // the null causes an IOException
        final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource((byte[]) null, TEXT_XML));
        final UserMessage userMessage = FSTestHelper.getUserMessage(this.getClass(), "testDeliverMessageNormalFlow_metadata.xml");
        final Map<String, FSPayload> fsPayloads = new HashMap<>();
        fsPayloads.put("cid:message", new FSPayload(TEXT_XML, "message.xml", dataHandler));

        new Expectations(backendFS) {{
            backendFS.browseMessage(messageId, MSHRole.RECEIVING, null);
            result = new FSMessage(fsPayloads, userMessage);

            fsDomainService.getFSPluginDomain();
            result = FSSendMessagesService.DEFAULT_DOMAIN;

            fsFilesManager.setUpFileSystem(FSSendMessagesService.DEFAULT_DOMAIN);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.INCOMING_FOLDER);
            result = incomingFolder;

            backendFS.isEnabled(anyString);
            result = true;
        }};

        Assertions.assertThrows(FSPluginException.class, () -> backendFS.deliverMessage(messageEvent));
    }

    @Test
    public void testGetMessageSubmissionTransformer() {
        MessageSubmissionTransformer<FSMessage> result = backendFS.getMessageSubmissionTransformer();

        Assertions.assertEquals(defaultTransformer, result);
    }

    @Test
    public void testGetMessageRetrievalTransformer() {
        MessageRetrievalTransformer<FSMessage> result = backendFS.getMessageRetrievalTransformer();

        Assertions.assertEquals(defaultTransformer, result);
    }

    @Test
    public void testMessageStatusChanged() throws FSSetUpException, FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId(messageId);
        event.setFromStatus(MessageStatus.READY_TO_SEND);
        event.setToStatus(MessageStatus.SEND_ENQUEUED);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        String file = "content_" + messageId + ".xml.READY_TO_SEND";
        final FileObject contentFile = outgoingFolder.resolveFile(file);

        new Expectations(backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{contentFile};

            fsFileNameHelper.deriveFileName(file, MessageStatus.SEND_ENQUEUED);
            result = "content_" + messageId + ".xml.SEND_ENQUEUED";

            backendFS.isEnabled(anyString);
            result = true;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder() {{
            fsFilesManager.renameFile(contentFile, "content_" + messageId + ".xml.SEND_ENQUEUED");
        }};
    }


    @Test
    public void testMessageStatusChanged_SendSuccessDelete() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId(messageId);
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.ACKNOWLEDGED);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_" + messageId + ".xml.ACKNOWLEDGED");

        new Expectations(backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{contentFile};

            fsPluginProperties.isSentActionDelete(null);
            result = true;

            backendFS.isEnabled(anyString);
            result = true;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder() {{
            fsFilesManager.deleteFile(contentFile);
        }};
    }

    @Test
    public void testMessageStatusChanged_SendSuccessArchive(@Injectable MessageStatusChangeEvent event) {
        String domain = "myDomain";
        String service = "myService";
        String action = "myAction";
        Map<String, Object> properties = new HashMap<>();
        properties.put("service", service);
        properties.put("action", action);

        new Expectations(backendFS) {{
            backendFS.isEnabled(anyString);
            result = true;

            fsDomainService.getFSPluginDomain();
            result = domain;

            event.getMessageId();
            result = messageId;

            backendFS.isSendingEvent(event);
            result = false;

            backendFS.isSendSuccessEvent(event);
            result = true;
        }};

        backendFS.messageStatusChanged(event);

        new Verifications() {{
            backendFS.handleSentMessage(domain, messageId);
        }};

    }

    @Test
    public void testHandleSentMessage(@Injectable FileObject contentFile,
                                      @Injectable FileObject rootDirectory,
                                      @Injectable FileObject outgoingFolder,
                                      @Injectable FileObject sentDirectory,
                                      @Injectable FileObject archivedFile) throws FileSystemException {
        String file = "content_" + messageId + ".xml";
        String sentFile = file + ".SENT";


        new Expectations(backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDirectory;

            fsFilesManager.getEnsureChildFolder(rootDirectory, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            backendFS.findMessageFile((FileObject) any, messageId);
            result = contentFile;

            fsPluginProperties.isSentActionDelete(null);
            result = false;

            fsPluginProperties.isSentActionArchive(null);
            result = true;

            contentFile.getParent().getName().getPath();
            result = sentFile;

            fsFileNameHelper.deriveSentDirectoryLocation(sentFile);
            result = FSFilesManager.OUTGOING_FOLDER;

            fsFilesManager.getEnsureChildFolder(rootDirectory, FSFilesManager.OUTGOING_FOLDER);
            result = sentDirectory;

            contentFile.getName().getBaseName();
            result = sentFile;

            fsFileNameHelper.stripStatusSuffix(sentFile);
            result = file;

            sentDirectory.resolveFile(file);
            result = archivedFile;

        }};

        backendFS.handleSentMessage(null, messageId);

        new Verifications() {{
            fsFilesManager.moveFile(contentFile, archivedFile);
        }};

    }

    @Test
    public void testMessageStatusChanged_SendFailedDelete() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId(messageId);
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.SEND_FAILURE);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_" + messageId + ".xml.SEND_ENQUEUED");

        new Expectations(backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{contentFile};

            backendFS.isEnabled(anyString);
            result = true;

        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder() {{
            fsFilesManager.handleSendFailedMessage(contentFile, anyString, anyString);
        }};
    }

    @Test
    public void testMessageStatusChanged_SendFailedArchive() throws FileSystemException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId(messageId);
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.SEND_FAILURE);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_" + messageId + ".xml.SEND_ENQUEUED");

        new Expectations(backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{contentFile};

            backendFS.isEnabled(anyString);
            result = true;

        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder() {{
            fsFilesManager.handleSendFailedMessage(contentFile, anyString, anyString);
        }};
    }

    @Test
    public void testMessageStatusChanged_SendFailedErrorFile() throws IOException, MessageNotFoundException {
        MessageStatusChangeEvent event = new MessageStatusChangeEvent();
        event.setMessageId(messageId);
        event.setFromStatus(MessageStatus.SEND_ENQUEUED);
        event.setToStatus(MessageStatus.SEND_FAILURE);
        event.setChangeTimestamp(new Timestamp(new Date().getTime()));

        final FileObject contentFile = outgoingFolder.resolveFile("content_" + messageId + ".xml.SEND_ENQUEUED");

        final List<ErrorResult> errorList = new ArrayList<>();
        ErrorResultImpl errorResult = new ErrorResultImpl();
        errorResult.setErrorCode(ErrorCode.EBMS_0001);
        errorList.add(errorResult);

        new Expectations(backendFS) {{
            fsFilesManager.setUpFileSystem(null);
            result = rootDir;

            fsFilesManager.getEnsureChildFolder(rootDir, FSFilesManager.OUTGOING_FOLDER);
            result = outgoingFolder;

            fsFilesManager.findAllDescendantFiles(outgoingFolder);
            result = new FileObject[]{contentFile};

            backendFS.getErrorsForMessage(messageId, MSHRole.SENDING);
            result = errorList;

            backendFS.isEnabled(anyString);
            result = true;
        }};

        backendFS.messageStatusChanged(event);

        contentFile.close();

        new VerificationsInOrder() {{
            fsFilesManager.handleSendFailedMessage(contentFile, anyString, anyString);
        }};
    }


    @Test
    public void payloadProcessedEvent(@Injectable PayloadProcessedEvent event,
                                      @Injectable FileObject fileObject) throws FileSystemException {
        new Expectations(backendFS) {{
            backendFS.isEnabled(anyString);
            result = true;

            fsFilesManager.getEnsureRootLocation(event.getFileName());
            result = fileObject;
        }};

        backendFS.payloadProcessedEvent(event);

        new Verifications() {{
            fsFilesManager.renameProcessedFile(fileObject, event.getMessageId());
            fsFilesManager.deleteLockFile(fileObject);
        }};
    }

    @Test
    public void test_getFileName(final @Mocked FSPayload fsPayload,
                                 final @Mocked FileObject incomingFolderByMessageId,
                                 final @Mocked FileObject fileNameObject) throws Exception {
        final String contentId = "cid:message";
        final String fileName = "message.xml";

        new Expectations(backendFS) {{
            fsPayload.getFileName();
            result = fileName;

            fsPayload.getMimeType();
            result = TEXT_XML;

            fsMimeTypeHelper.getExtension(anyString);
            result = ".xml";

            UriParser.decode(fileName);
            result = fileName;

            incomingFolderByMessageId.resolveFile(fileName, NameScope.CHILD);
            result = fileNameObject;
        }};

        String result = backendFS.getFileName(contentId, fsPayload, incomingFolderByMessageId);
        Assertions.assertEquals(fileName, result);

        new FullVerifications() {{
            incomingFolderByMessageId.resolveFile(fileName, NameScope.CHILD);
            fileNameObject.close();
        }};
    }

    @Test
    public void test_getFileName_Exception(final @Mocked FSPayload fsPayload,
                                           final @Mocked FileObject incomingFolderByMessageId) throws Exception {
        final String contentId = "cid:message";
        final String fileName = "%%";
        final String fileExtension = ".xml";
        final String expectedResult = "message" + fileExtension;

        new Expectations(backendFS) {{
            fsPayload.getFileName();
            result = fileName;

            fsPayload.getMimeType();
            result = TEXT_XML;

            fsMimeTypeHelper.getExtension(anyString);
            result = fileExtension;
        }};

        String result = backendFS.getFileName(contentId, fsPayload, incomingFolderByMessageId);
        Assertions.assertEquals(expectedResult, result);

        new FullVerifications() {
        };
    }

    @Test
    public void test_getFileName_Decode(final @Mocked FSPayload fsPayload,
                                        final @Mocked FileObject incomingFolderByMessageId) throws Exception {
        final String contentId = "cid:message";
        final String fileNameInput = ".%2F..%2Fmessage.xml";
        final String fileNameDecoded = "./../message.xml";
        final String fileNameExpected = "message.xml";

        new Expectations(backendFS) {{
            fsPayload.getFileName();
            result = fileNameInput;

            fsPayload.getMimeType();
            result = TEXT_XML;

            fsMimeTypeHelper.getExtension(anyString);
            result = ".xml";

            UriParser.decode(fileNameInput);
            result = fileNameDecoded;

            incomingFolderByMessageId.resolveFile(anyString, NameScope.CHILD);
            result = new FileSystemException("folder outside the parent");
        }};

        final String fileName = backendFS.getFileName(contentId, fsPayload, incomingFolderByMessageId);
        Assertions.assertNotNull(fileName);
        Assertions.assertEquals(fileNameExpected, fileName);

        new FullVerifications() {{
            String fileNameActual;
            incomingFolderByMessageId.resolveFile(fileNameActual = withCapture(), NameScope.CHILD);
            Assertions.assertEquals(fileNameDecoded, fileNameActual);
        }};
    }

    @Test
    public void test_getFileName_Empty(final @Mocked FSPayload fsPayload,
                                       final @Mocked FileObject incomingFolderByMessageId) throws Exception {
        final String contentId = "cid:message2";
        final String fileExtension = ".xml";
        final String fileName = "message2" + fileExtension;

        new Expectations(backendFS) {{
            fsPayload.getFileName();
            result = null;

            fsPayload.getMimeType();
            result = TEXT_XML;

            fsMimeTypeHelper.getExtension(anyString);
            result = fileExtension;
        }};

        String fileNameActual = backendFS.getFileName(contentId, fsPayload, incomingFolderByMessageId);
        Assertions.assertTrue(StringUtils.isNotBlank(fileNameActual));
        Assertions.assertEquals(fileName, fileNameActual);

        new FullVerifications() {
        };
    }

    @Test
    public void test_getFileName_Empty_cidEmpty(final @Mocked FSPayload fsPayload,
                                                final @Mocked FileObject incomingFolderByMessageId) throws Exception {
        final String contentId = null;
        final String fileExtension = ".xml";

        new Expectations(backendFS) {{
            fsPayload.getFileName();
            result = null;

            fsPayload.getMimeType();
            result = TEXT_XML;

            fsMimeTypeHelper.getExtension(anyString);
            result = fileExtension;
        }};

        String fileNameActual = backendFS.getFileName(contentId, fsPayload, incomingFolderByMessageId);
        Assertions.assertTrue(StringUtils.isNotBlank(fileNameActual));
        Assertions.assertTrue(StringUtils.endsWith(fileNameActual, fileExtension));

        new FullVerifications() {
        };
    }

    @Test
    public void getFileNameExtensionTest() throws MimeTypeException {
        String mimeType = "text/xml";

        new Expectations() {{
            fsMimeTypeHelper.getExtension(mimeType);
            result = new MimeTypeException("Invalid Type");
        }};

        Assertions.assertNotNull(backendFS.getFileNameExtension(mimeType));
    }
}
