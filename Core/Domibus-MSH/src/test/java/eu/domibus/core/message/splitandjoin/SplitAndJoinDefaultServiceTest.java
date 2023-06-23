package eu.domibus.core.message.splitandjoin;

import eu.domibus.api.ebms3.model.Ebms3Error;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3UserMessage;
import eu.domibus.api.ebms3.model.mf.Ebms3MessageFragmentType;
import eu.domibus.api.ebms3.model.mf.Ebms3MessageHeaderType;
import eu.domibus.api.model.*;
import eu.domibus.api.model.splitandjoin.MessageGroupEntity;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Splitting;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.receiver.handler.IncomingSourceMessageHandler;
import eu.domibus.core.ebms3.sender.EbMS3MessageBuilder;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.ebms3.sender.retry.UpdateRetryLoggingService;
import eu.domibus.core.ebms3.ws.attachment.AttachmentCleanupService;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.message.*;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.message.receipt.AS4ReceiptService;
import eu.domibus.core.message.retention.MessageRetentionDefaultService;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorage;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.SoapUtil;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.MessageImpl;
import org.apache.neethi.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.soap.SOAPMessage;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static eu.domibus.core.message.splitandjoin.SplitAndJoinDefaultService.ERROR_GENERATING_THE_SIGNAL_SOAPMESSAGE_FOR_SOURCE_MESSAGE;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cosmin Baciu, Soumya
 * @since 4.1
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "unchecked", "ConstantConditions"})
@ExtendWith(JMockitExtension.class)
public class SplitAndJoinDefaultServiceTest {

    public static final long ENTITY_ID = 1L;
    public static final String MESSAGE_ID = "messageId";
    @Tested
    SplitAndJoinDefaultService splitAndJoinDefaultService;

    @Injectable
    MessageFragmentDao messageFragmentDao;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    protected MessageGroupDao messageGroupDao;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected SoapUtil soapUtil;

    @Injectable
    protected PModeProvider pModeProvider;

    @Injectable
    protected PayloadFileStorageProvider storageProvider;

    @Injectable
    protected MessageUtil messageUtil;

    @Injectable
    protected UserMessageDefaultService userMessageDefaultService;

    @Injectable
    protected UserMessageLogDao userMessageLogDao;

    @Injectable
    protected UpdateRetryLoggingService updateRetryLoggingService;

    @Injectable
    protected AttachmentCleanupService attachmentCleanupService;

    @Injectable
    protected UserMessageHandlerService userMessageHandlerService;

    @Injectable
    protected MessagingService messagingService;

    @Injectable
    protected UserMessageService userMessageService;

    @Injectable
    protected IncomingSourceMessageHandler incomingSourceMessageHandler;

    @Injectable
    protected PolicyService policyService;

    @Injectable
    protected MSHDispatcher mshDispatcher;

    @Injectable
    protected AS4ReceiptService as4ReceiptService;

    @Injectable
    protected EbMS3MessageBuilder messageBuilder;

    @Injectable
    protected MessageRetentionDefaultService messageRetentionService;

    @Injectable
    protected MessageGroupService messageGroupService;

    @Injectable
    protected ErrorLogService errorLogService;

    @Injectable
    public Ebms3Converter ebms3Converter;

    @Injectable
    public PartInfoDao partInfoDao;

    @Injectable
    public MshRoleDao mshRoleDao;

    @Injectable
    public UserMessageDao userMessageDao;

    @Injectable
    SplitAndJoinHelper splitAndJoinHelper;

    @Injectable
    UserMessagePayloadService userMessagePayloadService;

    @TempDir
    Path tempDir;

//    @Test
//    public void createUserFragmentsFromSourceFile(@Injectable SOAPMessage sourceMessageRequest,
//                                                  @Injectable UserMessage userMessage,
//                                                  @Injectable MessageExchangeConfiguration userMessageExchangeConfiguration,
//                                                  @Injectable LegConfiguration legConfiguration,
//                                                  @Mocked File file) throws EbMS3Exception, IOException {
//        String sourceMessageFileName = "invoice.pdf";
//        long sourceMessageFileLength = 23L;
//        String contentTypeString = "application/pdf";
//        boolean compression = false;
//        String pModeKey = "mykey";
//        String sourceMessageId = "123";
//
//
//        List<String> fragmentFiles = new ArrayList<>();
//        fragmentFiles.add("fragment1");
//        fragmentFiles.add("fragment2");
//
//        new Expectations(splitAndJoinDefaultService) {{
//            userMessage.getMessageId();
//            result = sourceMessageId;
//
//            userMessage.getMessageId();
//            result = sourceMessageId;
//
//            new File(sourceMessageFileName);
//            result = file;
//
//            file.delete();
//            result = true;
//
//            file.length();
//            result = sourceMessageFileLength;
//
//            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING);
//            result = userMessageExchangeConfiguration;
//
//            userMessageExchangeConfiguration.getPmodeKey();
//            result = pModeKey;
//
//            pModeProvider.getLegConfiguration(pModeKey);
//            result = legConfiguration;
//
//
//            splitAndJoinDefaultService.splitSourceMessage((File) any, anyInt);
//            result = fragmentFiles;
//        }};
//
//        splitAndJoinDefaultService.createUserFragmentsFromSourceFile(sourceMessageFileName, sourceMessageRequest, userMessage, contentTypeString, compression);
//
//        new Verifications() {{
//            MessageGroupEntity messageGroupEntity;
//            userMessageDefaultService.createMessageFragments(userMessage, messageGroupEntity = withCapture(), fragmentFiles);
//
//            Assertions.assertEquals(2L, messageGroupEntity.getFragmentCount().longValue());
//            Assertions.assertEquals(sourceMessageId, messageGroupEntity.getGroupId());
//            Assertions.assertEquals(BigInteger.valueOf(sourceMessageFileLength), messageGroupEntity.getMessageSize());
//
//            attachmentCleanupService.cleanAttachments(sourceMessageRequest);
//        }};
//    }

    @Test
    public void rejoinSourceMessage(@Injectable final SOAPMessage sourceRequest,
                                    @Injectable final Ebms3Messaging ebms3Messaging,
                                    @Injectable final Ebms3UserMessage ebms3UserMessage,
                                    @Injectable final UserMessage userMessage,
                                    @Injectable final PartInfo partInfo,
                                    @Injectable MessageExchangeConfiguration userMessageExchangeConfiguration,
                                    @Injectable LegConfiguration legConfiguration
    ) throws EbMS3Exception {
        String sourceMessageId = "123";
        String sourceMessageFile = "invoice.pdf";
        String backendName = "mybackend";
        String pModeKey = "mykey";
        String reversePModeKey = "reversemykey";

        List<PartInfo> partInfos = Collections.singletonList(partInfo);
        new Expectations(splitAndJoinDefaultService) {{
            splitAndJoinDefaultService.rejoinSourceMessage(sourceMessageId, (File) any);
            result = sourceRequest;

            messageUtil.getMessage(sourceRequest);
            result = ebms3Messaging;

            ebms3Messaging.getUserMessage();
            result = ebms3UserMessage;

            ebms3Converter.convertFromEbms3(ebms3Messaging.getUserMessage());
            result = userMessage;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.RECEIVING);
            result = userMessageExchangeConfiguration;

            userMessageExchangeConfiguration.getPmodeKey();
            result = pModeKey;

            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            userMessage.getMessageId();
            result = sourceMessageId;

            userMessageExchangeConfiguration.getReversePmodeKey();
            result = reversePModeKey;
        }};

        splitAndJoinDefaultService.rejoinSourceMessage(sourceMessageId, sourceMessageFile, backendName);

        new Verifications() {{
            messageGroupService.setSourceMessageId(sourceMessageId, sourceMessageId);
            incomingSourceMessageHandler.processMessage(sourceRequest, ebms3Messaging);
            userMessageService.scheduleSourceMessageReceipt(sourceMessageId, reversePModeKey);
        }};
    }

    @Test
    public void rejoinSourceMessage1(@Injectable File sourceMessageFile,
                                     @Injectable MessageGroupEntity messageGroupEntity) {
        String sourceMessageId = "123";
        String contentType = "application/xml";


        new Expectations(splitAndJoinDefaultService) {{
            messageGroupDao.findByGroupIdWithMessageHeader(sourceMessageId);
            result = messageGroupEntity;

            splitAndJoinDefaultService.createContentType(anyString, anyString);
            result = contentType;

            splitAndJoinDefaultService.getUserMessage(sourceMessageFile, contentType);
        }};

        splitAndJoinDefaultService.rejoinSourceMessage(sourceMessageId, sourceMessageFile);

        new Verifications() {{
            splitAndJoinDefaultService.getUserMessage(sourceMessageFile, contentType);
        }};
    }

    @Test
    public void sendSourceMessageReceipt(@Injectable final SOAPMessage sourceRequest) throws EbMS3Exception {
        String sourceMessageId = "123";
        String pModeKey = "mykey";

        new Expectations(splitAndJoinDefaultService) {{
            as4ReceiptService.generateReceipt(sourceMessageId, MSHRole.RECEIVING, false);
            result = sourceRequest;

            splitAndJoinDefaultService.sendSignalMessage(sourceRequest, pModeKey);
        }};

        splitAndJoinDefaultService.sendSourceMessageReceipt(sourceMessageId, pModeKey);

        new Verifications() {{
            splitAndJoinDefaultService.sendSignalMessage(sourceRequest, pModeKey);
            times = 1;
        }};
    }

    @Test
    public void sendSignalError(@Injectable SOAPMessage soapMessage) throws EbMS3Exception {
        String messageId = "123";
        String pModeKey = "mykey";
        String ebMS3ErrorCode = ErrorCode.EbMS3ErrorCode.EBMS_0004.getCode().getErrorCode().getErrorCodeName();
        String errorDetail = "Split and Joing error";

        new Expectations() {{
            messageBuilder.buildSOAPFaultMessage((Ebms3Error) any);
            result = soapMessage;
        }};

        splitAndJoinDefaultService.sendSignalError(messageId, ebMS3ErrorCode, errorDetail, pModeKey);

        new Verifications() {{
            Ebms3Error error;
            messageBuilder.buildSOAPFaultMessage(error = withCapture());

            Assertions.assertEquals(error.getErrorCode(), ebMS3ErrorCode);
            Assertions.assertEquals(error.getErrorDetail(), errorDetail);

            splitAndJoinDefaultService.sendSignalMessage(soapMessage, pModeKey);
        }};
    }

    @Test
    public void sendSignalMessage(@Injectable SOAPMessage soapMessage,
                                  @Injectable LegConfiguration legConfiguration,
                                  @Injectable Party receiverParty,
                                  @Injectable Policy policy
    ) throws EbMS3Exception {
        String pModeKey = "mykey";
        String endpoint = "http://localhost/msh";

        new Expectations() {{
            pModeProvider.getLegConfiguration(pModeKey);
            result = legConfiguration;

            pModeProvider.getReceiverParty(pModeKey);
            result = receiverParty;

            receiverParty.getEndpoint();
            result = endpoint;

            policyService.getPolicy(legConfiguration);
            result = policy;
        }};

        splitAndJoinDefaultService.sendSignalMessage(soapMessage, pModeKey);

        new Verifications() {{
            mshDispatcher.dispatch(soapMessage, endpoint, policy, legConfiguration, pModeKey);
        }};
    }

    @Test
    public void generateSourceFileName() {
        String directory = "/home/temp";

        final String generateSourceFileName = splitAndJoinDefaultService.generateSourceFileName(directory);

        Assertions.assertTrue(StringUtils.contains(generateSourceFileName, directory + "/" ));
    }

    @Test
    public void rejoinMessageFragments(@Injectable MessageGroupEntity messageGroupEntity,
                                       @Mocked UserMessage userMessage1,
                                       @Injectable PartInfo partInfo) {
        String groupId = "123";
        String fileName = "invoice.pdf";

        List<UserMessage> userMessageFragments = Collections.singletonList(userMessage1);

        List<PartInfo> partInfoList = Collections.singletonList(partInfo);

        new Expectations(splitAndJoinDefaultService) {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;

            messageGroupEntity.getFragmentCount();
            result = 1;

            partInfo.getFileName();
            result = fileName;

            userMessageDao.findUserMessageByGroupId(groupId);
            result = userMessageFragments;

            userMessage1.getEntityId();
            result = ENTITY_ID;

            partInfoDao.findPartInfoByUserMessageEntityId(ENTITY_ID);
            result = partInfoList;

            partInfo.getFileName();
            result = "fileName";

            splitAndJoinDefaultService.mergeSourceFile((List<File>) any, messageGroupEntity);
        }};

        splitAndJoinDefaultService.rejoinMessageFragments(groupId);

        new Verifications() {{
            List<File> fragmentFilesInOrder;

            splitAndJoinDefaultService.mergeSourceFile(fragmentFilesInOrder = withCapture(), messageGroupEntity);

            Assertions.assertEquals(1, fragmentFilesInOrder.size());
        }};
    }

    @Test
    public void setUserMessageFragmentAsFailedSendEnqueued(@Injectable UserMessage userMessage,
                                                           @Injectable UserMessageLog messageLog) {
        setUserMessageFragmentAsFailed(userMessage, messageLog, MessageStatus.SEND_ENQUEUED);
    }

    @Test
    public void setUserMessageFragmentAsFailedWaitingForRetry(@Injectable UserMessage userMessage,
                                                              @Injectable UserMessageLog messageLog) {
        setUserMessageFragmentAsFailed(userMessage, messageLog, MessageStatus.WAITING_FOR_RETRY);
    }

    @Test
    public void setUserMessageFragmentAsFailedAcknowledged(@Injectable UserMessage userMessage,
                                                           @Injectable UserMessageLog messageLog) {
        String messageId = "123";
        new Expectations() {{
            userMessageLogDao.findByMessageIdSafely(messageId, MSHRole.RECEIVING);
            result = messageLog;

            messageLog.getMessageStatus();
            result = MessageStatus.ACKNOWLEDGED;

        }};

        splitAndJoinDefaultService.setUserMessageFragmentAsFailed(messageId);

        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, messageLog);
            times = 0;
        }};
    }

    protected void setUserMessageFragmentAsFailed(@Injectable UserMessage userMessage,
                                                  @Injectable UserMessageLog messageLog, MessageStatus messageStatus) {
        String messageId = "123";
        new Expectations() {{
            userMessageLogDao.findByMessageIdSafely(messageId, MSHRole.RECEIVING);
            result = messageLog;

            messageLog.getMessageStatus();
            result = messageStatus;

        }};

        splitAndJoinDefaultService.setUserMessageFragmentAsFailed(messageId);

        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, messageLog);
            times = 1;
        }};
    }

    @Test
    public void setUserMessageFragmentAsFailedWithOtherStatus(@Injectable UserMessage userMessage,
                                                              @Injectable UserMessageLog messageLog) {
        String messageId = "123";
        new Expectations() {{
            userMessageLogDao.findByMessageIdSafely(messageId, MSHRole.RECEIVING);
            result = messageLog;

            messageLog.getMessageStatus();
            result = MessageStatus.ACKNOWLEDGED;
        }};

        splitAndJoinDefaultService.setUserMessageFragmentAsFailed(messageId);

        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, messageLog);
            times = 0;

        }};
    }

    @Test
    public void handleExpiredReceivedGroups(@Injectable MessageGroupEntity group1) {
        List<MessageGroupEntity> messageGroupEntities = new ArrayList<>();
        messageGroupEntities.add(group1);

        List<MessageGroupEntity> expiredGroups = new ArrayList<>();
        expiredGroups.add(group1);

        new Expectations(splitAndJoinDefaultService) {{
            messageGroupDao.findOngoingReceivedNonExpiredOrRejected();
            result = messageGroupEntities;

            splitAndJoinDefaultService.getReceivedExpiredGroups(messageGroupEntities);
            result = expiredGroups;

            splitAndJoinDefaultService.setReceivedGroupAsExpired(group1);
        }};

        splitAndJoinDefaultService.handleExpiredReceivedGroups();

        new Verifications() {{
            splitAndJoinDefaultService.setReceivedGroupAsExpired(group1);
            times = 1;

        }};
    }

    @Test
    public void handleExpiredSendGroups(@Injectable MessageGroupEntity group1) {
        List<MessageGroupEntity> messageGroupEntities = new ArrayList<>();
        messageGroupEntities.add(group1);

        List<MessageGroupEntity> expiredGroups = new ArrayList<>();
        expiredGroups.add(group1);

        new Expectations(splitAndJoinDefaultService) {{
            messageGroupDao.findOngoingSendNonExpiredOrRejected();
            result = messageGroupEntities;

            splitAndJoinDefaultService.getSendExpiredGroups(messageGroupEntities);
            result = expiredGroups;

            splitAndJoinDefaultService.setSendGroupAsExpired(group1);
        }};

        splitAndJoinDefaultService.handleExpiredSendGroups();

        new Verifications() {{
            splitAndJoinDefaultService.setSendGroupAsExpired(group1);
            times = 1;

        }};
    }

    @Test
    public void setReceivedGroupAsExpired(@Injectable MessageGroupEntity group1,
                                          @Injectable UserMessage userMessage) {
        String groupId = "123";

        new Expectations() {{
            group1.getGroupId();
            result = groupId;

            group1.getEntityId();
            result = ENTITY_ID;

            userMessageDao.findByGroupEntityId(ENTITY_ID);
            result = userMessage;

            userMessage.getMessageId();
            result = MESSAGE_ID;
        }};

        splitAndJoinDefaultService.setReceivedGroupAsExpired(group1);

        new Verifications() {{
            group1.setExpired(true);
            messageGroupDao.update(group1);

            userMessageService.scheduleSplitAndJoinReceiveFailed(groupId, MESSAGE_ID, ErrorCode.EbMS3ErrorCode.EBMS_0051.getCode().getErrorCode().getErrorCodeName(), SplitAndJoinDefaultService.ERROR_MESSAGE_GROUP_HAS_EXPIRED);
        }};
    }

    @Test
    public void setSendGroupAsExpired(@Injectable MessageGroupEntity group1) {
        String groupId = "123";

        new Expectations() {{
            group1.getGroupId();
            result = groupId;
        }};

        splitAndJoinDefaultService.setSendGroupAsExpired(group1);

        new Verifications() {{
            group1.setExpired(true);
            messageGroupDao.update(group1);

            userMessageService.scheduleSplitAndJoinSendFailed(groupId, anyString);
        }};
    }

    @Test
    public void getReceivedExpiredGroups(@Injectable MessageGroupEntity group1) {
        List<MessageGroupEntity> messageGroupEntities = new ArrayList<>();
        messageGroupEntities.add(group1);

        new Expectations(splitAndJoinDefaultService) {{
            splitAndJoinDefaultService.isReceivedGroupExpired(group1);
            result = true;
        }};

        final List<MessageGroupEntity> expiredGroups = splitAndJoinDefaultService.getReceivedExpiredGroups(messageGroupEntities);
        assertNotNull(expiredGroups);
        assertEquals(1, expiredGroups.size());
        assertEquals(expiredGroups.iterator().next(), group1);


    }

    @Test
    public void isReceivedGroupExpired(@Injectable MessageGroupEntity group,
                                       @Injectable UserMessage userMessageFragment) {
        String groupId = "123";

        final List<UserMessage> fragments = Collections.singletonList(userMessageFragment);

        new Expectations(splitAndJoinDefaultService) {{
            group.getGroupId();
            result = groupId;

            userMessageDao.findUserMessageByGroupId(groupId);
            result = fragments;

            splitAndJoinDefaultService.isGroupExpired((UserMessage) any, anyString);
            result = true;
        }};

        final boolean groupExpired = splitAndJoinDefaultService.isReceivedGroupExpired(group);
        Assertions.assertTrue(groupExpired);

        new Verifications() {{
            splitAndJoinDefaultService.isGroupExpired(userMessageFragment, groupId);
        }};
    }

    @Test
    public void isSendGroupExpired(@Injectable MessageGroupEntity group,
                                   @Injectable final UserMessage sourceUserMessage) {
        String sourceMessageId = "123";

        new Expectations(splitAndJoinDefaultService) {{
            group.getEntityId();
            result = ENTITY_ID;

            userMessageDao.findByGroupEntityId(ENTITY_ID);
            result = sourceUserMessage;

            group.getGroupId();
            result = sourceMessageId;

            splitAndJoinDefaultService.isGroupExpired(sourceUserMessage, anyString);
            result = true;
        }};

        final boolean groupExpired = splitAndJoinDefaultService.isSendGroupExpired(group);
        Assertions.assertTrue(groupExpired);

        new Verifications() {{
            splitAndJoinDefaultService.isGroupExpired(sourceUserMessage, sourceMessageId);
        }};


    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void isGroupExpired(@Injectable final UserMessage userMessage,
                               @Injectable MessageExchangeConfiguration userMessageExchangeContext,
                               @Injectable LegConfiguration legConfiguration,
                               @Injectable UserMessageLog userMessageLog) throws EbMS3Exception {
        final long entityId = 123;
        String pmodeKey = "pModeKey";


        new Expectations(LocalDateTime.class) {{
            userMessage.getEntityId();
            result = entityId;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING);
            result = userMessageExchangeContext;

            userMessageExchangeContext.getPmodeKey();
            result = pmodeKey;

            pModeProvider.getLegConfiguration(pmodeKey);
            result = legConfiguration;

            legConfiguration.getSplitting().getJoinInterval();
            result = 1;

            userMessageLogDao.findByEntityId(entityId);
            result = userMessageLog;
        }};

        final boolean groupExpired = splitAndJoinDefaultService.isGroupExpired(userMessage, "anyGroupId");
        Assertions.assertTrue(groupExpired);

    }

    @Test
    public void messageFragmentSendFailed(@Injectable UserMessage userMessage) {
        String groupId = "123";

        final List<UserMessage> fragments = new ArrayList<>();
        fragments.add(userMessage);

        new Expectations(splitAndJoinDefaultService) {{
            splitAndJoinDefaultService.sendSplitAndJoinFailed(groupId);

            userMessageDao.findUserMessageByGroupId(groupId);
            result = fragments;

            userMessage.getMessageId();
            result = MESSAGE_ID;
        }};

        splitAndJoinDefaultService.splitAndJoinSendFailed(groupId, "Send failed");

        new Verifications() {{
            userMessageService.scheduleSetUserMessageFragmentAsFailed(MESSAGE_ID, userMessage.getMshRole().getRole());
            times = 1;

            errorLogService.createErrorLog(groupId, ErrorCode.EBMS_0004, "[SPLIT] " + "Send failed", MSHRole.SENDING, userMessage);
            times = 1;
        }};
    }

    @Test
    public void sendSplitAndJoinFailed(@Injectable UserMessage userMessage,
                                       @Injectable MessageGroupEntity messageGroupEntity) {
        String groupId = "123";

        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;

            splitAndJoinDefaultService.setSourceMessageAsFailed(userMessage);
        }};

        splitAndJoinDefaultService.sendSplitAndJoinFailed(groupId);

        new Verifications() {{
            messageGroupEntity.setRejected(true);
            messageGroupDao.update(messageGroupEntity);

            splitAndJoinDefaultService.setSourceMessageAsFailed(userMessage);
            times = 1;
        }};
    }

    @Test
    public void splitAndJoinReceiveFailed_messageGroupEntity_null() {
        String groupId = "123";
        final String ebMS3ErrorCode = "004";
        final String errorDetail = "Random error";

        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = null;
        }};

        splitAndJoinDefaultService.splitAndJoinReceiveFailed(groupId, groupId, ebMS3ErrorCode, errorDetail);

        new FullVerifications() {
        };
    }

    @Test
    public void splitAndJoinReceiveFailed_noUserMessages(@Injectable MessageGroupEntity messageGroupEntity) {
        String groupId = "123";
        final String ebMS3ErrorCode = "004";
        final String errorDetail = "Random error";

        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;

            userMessageDao.findUserMessageByGroupId(groupId);
            result = null;
        }};

        try {
            splitAndJoinDefaultService.splitAndJoinReceiveFailed(groupId, groupId, ebMS3ErrorCode, errorDetail);
            fail();
        } catch (SplitAndJoinException e) {
            assertTrue(StringUtils.containsIgnoreCase(e.getMessage(), ERROR_GENERATING_THE_SIGNAL_SOAPMESSAGE_FOR_SOURCE_MESSAGE));
        }

        new FullVerifications() {{
            messageGroupEntity.setRejected(true);
            messageGroupDao.update(messageGroupEntity);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void splitAndJoinReceiveFailed_exception(
            @Injectable UserMessage fragment,
            @Injectable MessageGroupEntity messageGroupEntity,
            @Injectable MessageExchangeConfiguration userMessageExchangeContext,
            @Injectable LegConfiguration legConfiguration,
            @Injectable EbMS3Exception exception) throws EbMS3Exception {
        String groupId = "123";
        final String ebMS3ErrorCode = "004";
        final String errorDetail = "Random error";

        final List<UserMessage> fragments = new ArrayList<>();
        fragments.add(fragment);

        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;

            userMessageDao.findUserMessageByGroupId(groupId);
            result = fragments;

            pModeProvider.findUserMessageExchangeContext(fragment, MSHRole.RECEIVING);
            result = exception;
        }};


        try {
            splitAndJoinDefaultService.splitAndJoinReceiveFailed(groupId, groupId, ebMS3ErrorCode, errorDetail);
            fail();
        } catch (SplitAndJoinException e) {
            assertTrue(StringUtils.containsIgnoreCase(e.getMessage(), ERROR_GENERATING_THE_SIGNAL_SOAPMESSAGE_FOR_SOURCE_MESSAGE));
        }

        new FullVerifications() {{
            messageGroupEntity.setRejected(true);
            messageGroupDao.update(messageGroupEntity);

            List<UserMessage> messageIds;
            messageRetentionService.scheduleDeleteMessages(messageIds = withCapture());
            assertEquals(1, messageIds.size());
        }};
    }

    @Test
    public void splitAndJoinReceiveFailed(@Injectable UserMessage fragment,
                                          @Injectable MessageGroupEntity messageGroupEntity,
                                          @Injectable MessageExchangeConfiguration userMessageExchangeContext,
                                          @Injectable LegConfiguration legConfiguration) throws EbMS3Exception {
        String groupId = "123";
        final String ebMS3ErrorCode = "004";
        final String errorDetail = "Random error";
        String reversePmodeKey = "reverseKey";

        final List<UserMessage> fragments = new ArrayList<>();
        fragments.add(fragment);


        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;

            pModeProvider.findUserMessageExchangeContext(fragment, MSHRole.RECEIVING);
            result = userMessageExchangeContext;

            userMessageExchangeContext.getReversePmodeKey();
            result = reversePmodeKey;

            userMessageDao.findUserMessageByGroupId(groupId);
            result = fragments;
        }};


        splitAndJoinDefaultService.splitAndJoinReceiveFailed(groupId, groupId, ebMS3ErrorCode, errorDetail);

        new FullVerifications() {{
            messageGroupEntity.setRejected(true);
            messageGroupDao.update(messageGroupEntity);

            messageRetentionService.scheduleDeleteMessages(fragments);
            times = 1;

            userMessageDefaultService.scheduleSendingSignalError(groupId, ebMS3ErrorCode, errorDetail, reversePmodeKey);
        }};
    }

    @Test
    public void incrementSentFragments(@Injectable MessageGroupEntity messageGroupEntity) {
        String groupId = "123";

        new Expectations() {{
            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;
        }};

        splitAndJoinDefaultService.incrementSentFragments(groupId);

        new Verifications() {{
            messageGroupDao.update(messageGroupEntity);
            messageGroupEntity.incrementSentFragments();

        }};
    }

    @Test
    public void incrementReceivedFragments(@Injectable MessageGroupEntity messageGroupEntity) {
        String groupId = "123";
        String backendName = "mybackend";

        new Expectations() {{
            messageGroupEntity.getGroupId();
            result = groupId;

            messageGroupDao.findByGroupId(groupId);
            result = messageGroupEntity;

            messageGroupEntity.getReceivedFragments();
            result = 2;

            messageGroupEntity.getFragmentCount();
            result = 2;
        }};

        splitAndJoinDefaultService.incrementReceivedFragments(groupId, backendName);

        new Verifications() {{
            messageGroupEntity.incrementReceivedFragments();
            messageGroupDao.update(messageGroupEntity);
            userMessageService.scheduleSourceMessageRejoinFile(groupId, backendName);

        }};
    }

    @Test
    // Note for running this test on Mac OS with JDK 8: before trying to fix this test or marking it as @Disabledd,
    // ensure that you have the ".mime.types" file in your user home folder (please check the JDK implementation
    // for Mac OS sun.nio.fs.MacOSXFileSystemProvider); this file is used to determine the MIME type of files from
    // their extensions when the call to Files#probeContentType(Path) is made below.
    //
    // You can also append the mapping for the ZIP extension used below with the following command:
    // $ echo "application/zip					zip" >> ~/.mime.types
    public void compressAndDecompressSourceMessage() throws IOException {
        File sourceFile = Files.createFile(tempDir.resolve("file.txt")).toFile();
        FileUtils.writeStringToFile(sourceFile, "mycontent", Charset.defaultCharset());

        final File file = splitAndJoinDefaultService.compressSourceMessage(sourceFile.getAbsolutePath());
        Assertions.assertTrue(file.exists());
        Assertions.assertTrue(file.getAbsolutePath().endsWith(".zip"));
        Assertions.assertTrue(file.toPath().toString().contains("zip"));
    }

    @Test
    public void splitSourceMessage() throws IOException {
        File tempFile = Files.createFile(tempDir.resolve("file.txt")).toFile();
        final File storageDirectory = tempDir.toFile();

        new Expectations(splitAndJoinDefaultService) {{
            splitAndJoinDefaultService.getFragmentStorageDirectory();
            result = storageDirectory;
        }};


        byte[] b = new byte[2058576];
        new Random().nextBytes(b);
        FileUtils.writeByteArrayToFile(tempFile, b);

        final List<String> fragmentFiles = splitAndJoinDefaultService.splitSourceMessage(tempFile, 1);
        Assertions.assertEquals(2, fragmentFiles.size());
        Assertions.assertTrue(fragmentFiles.stream().anyMatch(s -> s.contains("file.txt_1")));
        Assertions.assertTrue(fragmentFiles.stream().anyMatch(s -> s.contains("file.txt_2")));
    }

    @Test
    public void createContentType() {
        String boundary = "myboundary";
        String start = "mystart";

        final String contentType = splitAndJoinDefaultService.createContentType(boundary, start);
        Assertions.assertEquals("multipart/related; type=\"application/soap+xml\"; boundary=" + boundary + "; start=" + start + "; start-info=\"application/soap+xml\"", contentType);

    }

    @Test
    public void mergeSourceFile(@Injectable MessageGroupEntity messageGroupEntity,
                                @Injectable Domain domain) throws IOException {
        List<File> fragmentFilesInOrder = new ArrayList<>();
        final File file1 = Files.createFile(tempDir.resolve("file1.txt")).toFile();
        FileUtils.writeStringToFile(file1, "text1", Charset.defaultCharset());

        final File file2 = Files.createFile(tempDir.resolve("file2.txt")).toFile();
        FileUtils.writeStringToFile(file2, "text2", Charset.defaultCharset());

        fragmentFilesInOrder.add(file1);
        fragmentFilesInOrder.add(file2);
        final File temporaryDirectoryLocation = tempDir.toFile();

        final File sourceFile = Files.createFile(tempDir.resolve("sourceFile.txt")).toFile();
        String sourceFileName = sourceFile.getAbsolutePath();

        new Expectations(splitAndJoinDefaultService) {{
            domibusPropertyProvider.getProperty(PayloadFileStorage.TEMPORARY_ATTACHMENT_STORAGE_LOCATION);
            result = temporaryDirectoryLocation.getAbsolutePath();

            splitAndJoinDefaultService.generateSourceFileName(temporaryDirectoryLocation.getAbsolutePath());
            result = sourceFileName;

            splitAndJoinDefaultService.isSourceMessageCompressed(messageGroupEntity);
            result = false;

            splitAndJoinDefaultService.mergeFiles(fragmentFilesInOrder, (OutputStream) any);

        }};

        final File result = splitAndJoinDefaultService.mergeSourceFile(fragmentFilesInOrder, messageGroupEntity);

        assertNotNull(result);
        new Verifications() {{
            OutputStream outputStream;
            splitAndJoinDefaultService.mergeFiles(fragmentFilesInOrder, outputStream = withCapture());
            Assertions.assertTrue(outputStream instanceof FileOutputStream);
        }};
    }

    @Test
    public void isSourceMessageCompressed(@Injectable MessageGroupEntity messageGroupEntity) {
        new Expectations() {{
            messageGroupEntity.getCompressionAlgorithm();
            result = "application/zip";
        }};

        final boolean sourceMessageCompressed = splitAndJoinDefaultService.isSourceMessageCompressed(messageGroupEntity);
        Assertions.assertTrue(sourceMessageCompressed);
    }

    @Test
    public void decompressGzip() throws IOException {
        final File file1 = Files.createFile(tempDir.resolve("file1.txt")).toFile();
        final String text1 = "text1";
        FileUtils.writeStringToFile(file1, text1, Charset.defaultCharset());
        final File compressSourceMessage = splitAndJoinDefaultService.compressSourceMessage(file1.getAbsolutePath());

        final File decompressed = Files.createFile(tempDir.resolve("file1_decompressed.txt")).toFile();
        splitAndJoinDefaultService.decompressGzip(compressSourceMessage, decompressed);
        Assertions.assertEquals(text1, FileUtils.readFileToString(decompressed, Charset.defaultCharset()));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getUserMessage(@Injectable FileInputStream fileInputStream,
                               @Injectable InputStream inputStream,
                               @Injectable MessageImpl messageImpl,
                               @Injectable MessageGroupEntity messageGroupEntity,
                               @Injectable final SOAPMessage soapMessage) throws IOException {
        final File sourceMessageFileName = Files.createFile(tempDir.resolve("file1.txt")).toFile();

        final String text1 = "text1";
        FileUtils.writeStringToFile(sourceMessageFileName, text1, Charset.defaultCharset());
        String contentTypeString = "application/xml";
        final File temporaryDirectoryLocation = tempDir.toFile();

        new Expectations(splitAndJoinDefaultService) {{
            domibusPropertyProvider.getProperty(PayloadFileStorage.TEMPORARY_ATTACHMENT_STORAGE_LOCATION);
            result = temporaryDirectoryLocation.getAbsolutePath();
        }};

        Assertions.assertNotNull(splitAndJoinDefaultService.getUserMessage(sourceMessageFileName, contentTypeString));
    }


    @SuppressWarnings("AccessStaticViaInstance")
    @Test
    public void mergeFilesTest(@Mocked File file1,
                               @Mocked File file2,
                               @Injectable OutputStream mergingStream,
                               @Injectable Files files,
                               @Injectable Path path) throws IOException {
        List<File> filesList = new ArrayList<>();
        filesList.add(file1);
        filesList.add(file2);
        new Expectations(splitAndJoinDefaultService) {
            {
                file1.toPath();
                result = path;
            }
        };
        splitAndJoinDefaultService.mergeFiles(filesList, mergingStream);
        new Verifications() {{
            files.copy(path, mergingStream);
            times = 2;
            mergingStream.flush();
            times = 2;
        }};

    }

    @Test
    public void testValidateUserMessageFragmentWithWrongFragmentsCount(@Injectable UserMessage userMessage,
                                                                       @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                                       @Injectable MessageGroupEntity messageGroupEntity,
                                                                       @Injectable LegConfiguration legConfiguration) {

        long totalFragmentCount = 5;

        new Expectations() {{
            legConfiguration.getSplitting();
            result = new Splitting();

            storageProvider.isPayloadsPersistenceInDatabaseConfigured();
            result = false;

            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            messageGroupEntity.getRejected();
            result = false;

            messageGroupEntity.getExpired();
            result = false;

            messageGroupEntity.getFragmentCount();
            result = totalFragmentCount;

            ebms3MessageFragmentType.getFragmentCount();
            result = 7;

            userMessage.getMessageId();
            result = "messageId";
        }};

        try {
            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);
            fail();
        } catch (EbMS3Exception e) {
            Assertions.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0048, e.getErrorCode());
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateUserMessageFragmentNoMessageGroupEntity(@Injectable UserMessage userMessage,
                                                                    @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                                    @Injectable MessageGroupEntity messageGroupEntity,
                                                                    @Injectable LegConfiguration legConfiguration) throws EbMS3Exception {
        new Expectations() {{
            legConfiguration.getSplitting();
            result = new Splitting();

            storageProvider.isPayloadsPersistenceInDatabaseConfigured();
            result = false;

            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            userMessage.getMessageId();
            result = "messageId";
        }};


        splitAndJoinDefaultService.validateUserMessageFragment(userMessage, null, ebms3MessageFragmentType, legConfiguration);

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateUserMessageFragment_ok(@Injectable UserMessage userMessage,
                                                   @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                   @Injectable MessageGroupEntity messageGroupEntity,
                                                   @Injectable LegConfiguration legConfiguration)
            throws EbMS3Exception {

        new Expectations() {{
            legConfiguration.getSplitting();
            result = new Splitting();

            storageProvider.isPayloadsPersistenceInDatabaseConfigured();
            result = false;
            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            messageGroupEntity.getRejected();
            result = false;

            messageGroupEntity.getExpired();
            result = false;

            messageGroupEntity.getFragmentCount();
            result = null;
        }};

        splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateUserMessageFragmentWithNoSplittingConfigured(@Injectable UserMessage userMessage,
                                                                         @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                                         @Injectable MessageGroupEntity messageGroupEntity,
                                                                         @Injectable LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getSplitting();
            result = null;

            legConfiguration.getName();
            result = "legName";

            userMessage.getMessageId();
            result = "messageId";
        }};

        try {
            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);
            fail("Not possible to use SplitAndJoin without PMode leg configuration");
        } catch (EbMS3Exception e) {
            Assertions.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0002, e.getErrorCode());
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateUserMessageFragmentWithDatabaseStorage(@Injectable UserMessage userMessage,
                                                                   @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                                   @Injectable MessageGroupEntity messageGroupEntity,
                                                                   @Injectable LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getSplitting();
            result = new Splitting();

            storageProvider.isPayloadsPersistenceInDatabaseConfigured();
            result = true;

            userMessage.getMessageId();
            result = "messageId";
        }};

        try {
            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);
            fail("Not possible to use SplitAndJoin with database payloads");
        } catch (EbMS3Exception e) {
            Assertions.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0002, e.getErrorCode());
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateUserMessageFragmentWithRejectedGroup(@Injectable UserMessage userMessage,
                                                                 @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                                 @Injectable MessageGroupEntity messageGroupEntity,
                                                                 @Injectable LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getSplitting();
            result = new Splitting();

            storageProvider.isPayloadsPersistenceInDatabaseConfigured();
            result = false;

            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            messageGroupEntity.getExpired();
            result = false;

            messageGroupEntity.getRejected();
            result = true;

            userMessage.getMessageId();
            result = "messageId";
        }};

        try {
            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);
            fail();
        } catch (EbMS3Exception e) {
            Assertions.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0040, e.getErrorCode());
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateUserMessageFragmentWithExpired(@Injectable UserMessage userMessage,
                                                           @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                           @Injectable MessageGroupEntity messageGroupEntity,
                                                           @Injectable LegConfiguration legConfiguration) {
        new Expectations() {{
            legConfiguration.getSplitting();
            result = new Splitting();

            storageProvider.isPayloadsPersistenceInDatabaseConfigured();
            result = false;

            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            messageGroupEntity.getExpired();
            result = true;

            userMessage.getMessageId();
            result = "messageId";
        }};

        try {
            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);
            fail();
        } catch (EbMS3Exception e) {
            Assertions.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0051, e.getErrorCode());
        }

        new FullVerifications() {
        };
    }

    @Test
    public void testHandleMessageFragment_createMessageGroup(@Injectable UserMessage userMessage,
                                                             @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                             @Injectable LegConfiguration legConfiguration,
                                                             @Injectable Ebms3MessageHeaderType ebms3MessageHeaderType) throws EbMS3Exception {
        new Expectations(splitAndJoinDefaultService) {{
            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            messageGroupDao.findByGroupId("groupId");
            result = null;
            times = 1;

            ebms3MessageFragmentType.getMessageHeader();
            result = ebms3MessageHeaderType;

            ebms3MessageHeaderType.getStart();
            result = "Start";

            ebms3MessageHeaderType.getBoundary();
            result = "Boundary";

            ebms3MessageFragmentType.getAction();
            result = "action";

            ebms3MessageFragmentType.getCompressionAlgorithm();
            result = "compressionAlgorithm";

            ebms3MessageFragmentType.getMessageSize();
            result = BigInteger.TEN;

            ebms3MessageFragmentType.getCompressedMessageSize();
            result = BigInteger.TEN;

            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            ebms3MessageFragmentType.getFragmentCount();
            result = 5L;

            userMessage.toString();
            result = "userMessage";

            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, (MessageGroupEntity) any, ebms3MessageFragmentType, legConfiguration);
            times = 1;

            ebms3MessageFragmentType.getFragmentNum();
            result = 41L;

//            userMessageHandlerService.addPartInfoFromFragment(userMessage, ebms3MessageFragmentType);
//            times = 1;
        }};

        splitAndJoinDefaultService.persistReceivedUserFragment(userMessage, ebms3MessageFragmentType, legConfiguration);

        new Verifications() {{
            messageGroupDao.create((MessageGroupEntity) any);
            times = 1;

        }};
    }

    @Test
    public void testHandleMessageFragmentWithGroupAlreadyExisting(@Injectable UserMessage userMessage,
                                                                  @Injectable Ebms3MessageFragmentType ebms3MessageFragmentType,
                                                                  @Injectable MessageGroupEntity messageGroupEntity,
                                                                  @Injectable LegConfiguration legConfiguration) throws EbMS3Exception {
        new Expectations(splitAndJoinDefaultService) {{
            ebms3MessageFragmentType.getGroupId();
            result = "groupId";

            messageGroupDao.findByGroupId("groupId");
            result = messageGroupEntity;
            times = 1;

            splitAndJoinDefaultService.validateUserMessageFragment(userMessage, messageGroupEntity, ebms3MessageFragmentType, legConfiguration);
            times = 1;

            ebms3MessageFragmentType.getFragmentNum();
            result = 41L;

//            userMessageHandlerService.addPartInfoFromFragment(userMessage, ebms3MessageFragmentType);
//            times = 1;
        }};

        splitAndJoinDefaultService.persistReceivedUserFragment(userMessage, ebms3MessageFragmentType, legConfiguration);

        new Verifications() {{

        }};
    }
}
