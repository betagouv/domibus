package eu.domibus.api.usermessage;

import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.messaging.MessagingException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.usermessage.domain.UserMessage;

import java.io.IOException;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
public interface UserMessageService {

    String COMMAND_SOURCE_MESSAGE_REJOIN = "SourceMessageRejoin";
    String COMMAND_SOURCE_MESSAGE_RECEIPT = "SourceMessageReceipt";
    String COMMAND_SOURCE_MESSAGE_REJOIN_FILE = "SourceMessageRejoinFile";
    String COMMAND_SPLIT_AND_JOIN_SEND_FAILED = "SplitAndJoinSendFailed";
    String COMMAND_SET_MESSAGE_FRAGMENT_AS_FAILED = "SetMessageFragmentAsFailed";
    String COMMAND_SEND_SIGNAL_ERROR = "SendSignalError";
    String COMMAND_SPLIT_AND_JOIN_RECEIVE_FAILED = "SplitAndJoinReceiveFailed";


    String MSG_SOURCE_MESSAGE_FILE = "SourceMessageFile";
    String MSG_TYPE = "messageType";
    String MSG_GROUP_ID = "groupId";
    String MSG_BACKEND_NAME = "backendName";
    String MSG_SOURCE_MESSAGE_ID = "sourceMessageId";
    String MSG_USER_MESSAGE_ID = "userMessageId";
    String MSG_MSH_ROLE = "userMessageRole";
    String MSG_EBMS3_ERROR_CODE = "ebms3ErrorCode";
    String MSG_EBMS3_ERROR_DETAIL = "ebms3ErrorDetail";

    String PULL_RECEIPT_REF_TO_MESSAGE_ID = "pullReceiptRefToMessageId";

    String getFinalRecipient(final String messageId, MSHRole mshRole);

    /**
     * @param messageId of the {@link eu.domibus.api.model.UserMessage}
     * @param mshRole
     * @return database object
     * @throws MessageNotFoundException if not found
     */
    eu.domibus.api.model.UserMessage getByMessageId(String messageId, MSHRole mshRole) throws MessageNotFoundException;

    /**
     * @param messageEntityId entity id of the {@link eu.domibus.api.model.UserMessage}
     * @return database object
     * @throws MessageNotFoundException if not found
     */
    eu.domibus.api.model.UserMessage getByMessageEntityId(long messageEntityId) throws MessageNotFoundException;

    String getOriginalSender(String messageId, MSHRole mshRole);

    List<String> getFailedMessages(String finalRecipient, String originalUser);

    Long getFailedMessageElapsedTime(String messageId);

    void sendEnqueuedMessage(String messageId);

    void deleteFailedMessage(String messageId);

    void deleteMessage(String messageId, MSHRole mshRole);

    void deleteMessagesWithIDs(List<Long> ids);

    void deleteMessageNotInFinalStatus(String messageId, MSHRole mshRole) throws MessageNotFoundException, MessagingException;

    void deleteMessageInFinalStatus(String messageId, MSHRole mshRole);

    List<String> deleteMessagesNotInFinalStatusDuringPeriod(Long begin, Long end, String originalUser);

    List<String> deleteMessagesInFinalStatusDuringPeriod(Long begin, Long end, String originalUser);

    /**
     * Schedules the handling of the SplitAndJoin send failed event
     *
     * @param groupId     The groupId for which the failure will be triggered
     * @param errorDetail The error detail
     */
    void scheduleSplitAndJoinSendFailed(String groupId, String errorDetail);

    /**
     * Schedules the marking of the UserMessageFragment as failed
     *
     * @param messageId
     * @param role
     */
    void scheduleSetUserMessageFragmentAsFailed(String messageId, MSHRole role);

    /**
     * Schedules the sending of the SourceMessage
     *
     * @param messageId
     */
    void scheduleSourceMessageSending(String messageId, Long messageEntityId);

    /**
     * Schedules the rejoining of the SourceMessage file
     *
     * @param groupId
     * @param backendName
     */
    void scheduleSourceMessageRejoinFile(String groupId, String backendName);

    /**
     * Schedules the rejoining of the SourceMessage
     *
     * @param groupId
     * @param file
     * @param backendName
     */
    void scheduleSourceMessageRejoin(String groupId, String file, String backendName);

    /**
     * Schedules the sending of the SourceMessage receipt
     *
     * @param messageId
     * @param pmodeKey
     */
    void scheduleSourceMessageReceipt(String messageId, String pmodeKey);

    /**
     * Schedules the sending of the Signal error in case a SourceMessage fails to be rejoined
     *
     * @param messageId
     * @param errorCode
     * @param errorDetail
     * @param pmodeKey
     */
    void scheduleSendingSignalError(String messageId, String errorCode, String errorDetail, String pmodeKey);

    void scheduleSplitAndJoinReceiveFailed(String groupId, String sourceMessageId, String errorCode, String errorDetail);

    /**
     * Schedule the sending of the asynchronous Pull Receipt
     *
     * @param messageId MessageId of the UserMessage (for which the pull receipt was generated)
     * @param pmodeKey  the pmode key of the UserMessage
     */
    void scheduleSendingPullReceipt(String messageId, String pmodeKey);

    /**
     * Schedule the sending of the asynchronous Pull Receipt (counting the retries)
     *
     * @param messageId  MessageId of the UserMessage (for which the pull receipt was generated)
     * @param pmodeKey   the pmode key of the UserMessage
     * @param retryCount the number of current attempts to send the receipt
     */
    void scheduleSendingPullReceipt(String messageId, String pmodeKey, int retryCount);

    /**
     * Gets a User Message based on the {@code messageId}
     *
     * @param messageId User Message Identifier
     * @return User Message {@link UserMessage}
     */
    UserMessage getMessage(String messageId, MSHRole mshRole);

    eu.domibus.api.model.UserMessage getMessageEntity(String messageId, MSHRole mshRole);

    eu.domibus.api.model.UserMessage getMessageEntity(Long messageEntityId);

    void checkCanGetMessageContent(String messageId, MSHRole mshRole);

    /**
     * Retrieves a message by id as a byte array
     *
     * @param messageId
     * @return the message serialized as byte array
     * @throws MessageNotFoundException in case there is no message with this id
     */
    byte[] getMessageAsBytes(String messageId, MSHRole mshRole) throws MessageNotFoundException;

    /**
     * Retrieves the message content as a zip file(used for downloading a message)
     *
     * @param messageId
     * @return a zip file with the message content
     * @throws MessageNotFoundException in case the message does nor exists or the content could not be retrieved( already sent and deleted)
     * @throws IOException              in case a read error
     */
    byte[] getMessageWithAttachmentsAsZip(String messageId, MSHRole mshRole) throws MessageNotFoundException, IOException;

    /**
     * Retrieves the user and signal message envelopes xmls as a zip file(used for downloading a message)
     *
     * @param messageId user message id
     * @param mshRole
     * @return a zip file with the envelopes
     */
    byte[] getMessageEnvelopesAsZip(String messageId, MSHRole mshRole);

    /**
     * Retrieves the user message envelope xml
     *
     * @param userMessageId user message id
     * @param mshRole
     * @return a string representing the envelope in xml format
     */
    String getUserMessageEnvelope(String userMessageId, MSHRole mshRole);

    /**
     * Retrieves the signal message envelope xml corresponding to the user message with the specified id
     *
     * @param userMessageId user message id
     * @param mshRole
     * @return a string representing the envelope in xml format
     */
    String getSignalMessageEnvelope(String userMessageId, MSHRole mshRole);

    eu.domibus.api.model.UserMessage getByMessageId(String messageId);

    void clearPayloadData(List<Long> entityIds);
}
