package eu.domibus.web.rest;

import eu.domibus.api.csv.CsvException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.MessageType;
import eu.domibus.api.model.NotificationStatus;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DateUtil;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.message.MessageLogInfo;
import eu.domibus.core.message.MessagesLogService;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.core.party.PartyDao;
import eu.domibus.core.rest.validators.FieldBlacklistValidator;
import eu.domibus.core.util.DateUtilImpl;
import eu.domibus.web.rest.ro.MessageLogFilterRequestRO;
import eu.domibus.web.rest.ro.MessageLogRO;
import eu.domibus.web.rest.ro.MessageLogResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
class MessageLogResourceParamTest {

    private static final String CSV_TITLE = "Conversation Id, From Party Id, To Party Id, Original Sender, Final Recipient, ref To Message Id, Message Id, Message Status, Notification Status, " +
            "MSH Role, Message Type, Deleted, Received, Downloaded, Send Attempts, Send Attempts Max, Next Attempt, Failed, Restored, Message Subtype";

    @Tested
    MessageLogResource messageLogResource;

    @Injectable
    TestService testService;

    @Injectable
    PartyDao partyDao;

    @Injectable
    DateUtil dateUtil;

    @Injectable
    CsvServiceImpl csvServiceImpl;

    @Injectable
    private MessagesLogService messagesLogService;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    RequestFilterUtils requestFilterUtils;

    @Injectable
    FieldBlacklistValidator fieldBlacklistValidator;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void testMessageLog(MessageType messageType) {
        ReflectionTestUtils.setField(messageLogResource, "requestFilterUtils", new RequestFilterUtils(new DateUtilImpl()));

        getMessageLogExe(messageType, false);
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void testMessageLog_test(MessageType messageType) {
        ReflectionTestUtils.setField(messageLogResource, "requestFilterUtils", new RequestFilterUtils(new DateUtilImpl()));

        getMessageLogExe(messageType, true);
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void testMessageLogInfoGetCsv(MessageType messageType) throws CsvException {
        ReflectionTestUtils.setField(messageLogResource, "requestFilterUtils", new RequestFilterUtils(new DateUtilImpl()));

        getCsv(messageType, false);
    }

    @ParameterizedTest
    @EnumSource(MessageType.class)
    void testMessageLogInfoGetCsv_test(MessageType messageType) throws CsvException {
        ReflectionTestUtils.setField(messageLogResource, "requestFilterUtils", new RequestFilterUtils(new DateUtilImpl()));

        getCsv(messageType, true);
    }

    private void getMessageLogExe(MessageType messageType, Boolean testMessage1) {
        // Given
        final MessageLogRO messageLogRO = createMessageLog(messageType, testMessage1);
        final List<MessageLogRO> resultList = Collections.singletonList(messageLogRO);
        MessageLogResultRO expectedMessageLogResult = new MessageLogResultRO();
        expectedMessageLogResult.setMessageLogEntries(resultList);

        new Expectations() {{
            messagesLogService.countAndFindPaged(messageType, anyInt, anyInt, anyString, anyBoolean, (HashMap<String, Object>) any);
            result = expectedMessageLogResult;
        }};

        // When
        final MessageLogResultRO messageLogResultRO = getMessageLog(messageType, testMessage1);

        // Then
        Assertions.assertNotNull(messageLogResultRO);
        Assertions.assertEquals(1, messageLogResultRO.getMessageLogEntries().size());

        MessageLogRO actualMessageLogRO = messageLogResultRO.getMessageLogEntries().get(0);
        Assertions.assertEquals(messageLogRO.getMessageId(), actualMessageLogRO.getMessageId());
        Assertions.assertEquals(messageLogRO.getMessageStatus(), actualMessageLogRO.getMessageStatus());
        Assertions.assertEquals(messageLogRO.getMessageType(), actualMessageLogRO.getMessageType());
        Assertions.assertEquals(messageLogRO.getDeleted(), actualMessageLogRO.getDeleted());
        Assertions.assertEquals(messageLogRO.getMshRole(), actualMessageLogRO.getMshRole());
        Assertions.assertEquals(messageLogRO.getNextAttempt(), actualMessageLogRO.getNextAttempt());
        Assertions.assertEquals(messageLogRO.getNotificationStatus(), actualMessageLogRO.getNotificationStatus());
        Assertions.assertEquals(messageLogRO.getReceived(), actualMessageLogRO.getReceived());
        Assertions.assertEquals(messageLogRO.getDownloaded(), actualMessageLogRO.getDownloaded());
        Assertions.assertEquals(messageLogRO.getSendAttempts(), actualMessageLogRO.getSendAttempts());
        Assertions.assertEquals(messageLogRO.getTestMessage(), actualMessageLogRO.getTestMessage());
    }

    private void getCsv(final MessageType messageType, final Boolean testMessage) {
        // Given
        Date date = new Date();
        List<MessageLogInfo> messageList = getMessageList(date, testMessage);

        new Expectations() {{
            messagesLogService.findAllInfoCSV(messageType, anyInt, "received", true, (HashMap<String, Object>) any);
            result = messageList;

            csvServiceImpl.exportToCSV(messageList, null, (Map<String, String>) any, (List<String>) any);
            result = CSV_TITLE +
                    "conversationId,fromPartyId,toPartyId,originalSender,finalRecipient,refToMessageId,messageId," + MessageStatus.ACKNOWLEDGED + "," +
                    NotificationStatus.NOTIFIED + "," + MSHRole.RECEIVING + "," + messageType + "," + date + "," + date + "," + date + ",1,5," + date + "," +
                    date + "," + date + "," + testMessage + System.lineSeparator();
        }};

        // When
        final ResponseEntity<String> csv = messageLogResource.getCsv(new MessageLogFilterRequestRO() {{
            setOrderBy("received");
            setMessageType(messageType);
            setTestMessage(testMessage);
        }});

        // Then
        Assertions.assertEquals(HttpStatus.OK, csv.getStatusCode());
        Assertions.assertEquals(CSV_TITLE +
                        "conversationId,fromPartyId,toPartyId,originalSender,finalRecipient,refToMessageId,messageId," + MessageStatus.ACKNOWLEDGED + "," + NotificationStatus.NOTIFIED + "," +
                        MSHRole.RECEIVING + "," + messageType + "," + date + "," + date + "," + date + ",1,5," + date + "," + date + "," + date + "," + testMessage + System.lineSeparator(),
                csv.getBody());
    }

    /**
     * Creates a {@link MessageLogRO} based on <code>messageType</code> and <code>testMessage</code>
     *
     * @param messageType Message Type
     * @param testMessage Test Message
     * @return <code>MessageLog</code>
     */
    private static MessageLogRO createMessageLog(MessageType messageType, Boolean testMessage) {

        MessageLogRO messageLogRO = new MessageLogRO();
        messageLogRO.setMessageId("messageId");
        messageLogRO.setMessageStatus(MessageStatus.ACKNOWLEDGED);
        messageLogRO.setNotificationStatus(NotificationStatus.REQUIRED);
        messageLogRO.setMshRole(MSHRole.RECEIVING);
        messageLogRO.setMessageType(messageType);
        messageLogRO.setDeleted(new Date());
        messageLogRO.setReceived(new Date());
        messageLogRO.setDownloaded(new Date());
        messageLogRO.setFromPartyId("fromPartyId");
        messageLogRO.setToPartyId("toPartyId");
        messageLogRO.setConversationId("conversationId");
        messageLogRO.setOriginalSender("originalSender");
        messageLogRO.setFinalRecipient("finalRecipient");
        messageLogRO.setRefToMessageId("refToMessageId");
        messageLogRO.setTestMessage(testMessage);


        return messageLogRO;
    }

    /**
     * Gets a MessageLog based on <code>messageType</code> and <code>testMessage</code>
     *
     * @param messageType Message Type
     * @param testMessage Message Subtype
     * @return <code>MessageLogResultRO</code> object
     */
    private MessageLogResultRO getMessageLog(MessageType messageType, Boolean testMessage) {
        return messageLogResource.getMessageLog(new MessageLogFilterRequestRO() {{
            setPage(1);
            setMessageId("MessageId");
            setMessageType(messageType);
            setTestMessage(testMessage);
        }});
    }

    /**
     * Get a MessageLogInfo List based on <code>messageInfo</code>, <code>date</code> and <code>testMessage</code>
     *
     * @param date        Date
     * @param testMessage test Message
     * @return <code>List</code> of <code>MessageLogInfo</code> objects
     */
    private List<MessageLogInfo> getMessageList(Date date, Boolean testMessage) {
        List<MessageLogInfo> result = new ArrayList<>();
        MessageLogInfo messageLog = new MessageLogInfo("messageId", MessageStatus.ACKNOWLEDGED,
                NotificationStatus.NOTIFIED, MSHRole.RECEIVING, date, date, date, 1, 5, date, "Europe/Brussels",
                0, "conversationId", "fromPartyId", "toPartyId", "originalSender", "finalRecipient",
                "refToMessageId", date, date, testMessage, false, false, "action", "serviceType", "serviceValue",
                "pluginType", 1L, date);
        result.add(messageLog);
        return result;
    }
}
