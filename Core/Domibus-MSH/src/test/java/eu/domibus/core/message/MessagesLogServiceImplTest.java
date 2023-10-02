package eu.domibus.core.message;

import eu.domibus.api.model.MessageType;
import eu.domibus.core.converter.MessageCoreMapper;
import eu.domibus.core.message.nonrepudiation.NonRepudiationService;
import eu.domibus.core.message.signal.SignalMessageLogDao;
import eu.domibus.web.rest.ro.MessageLogRO;
import eu.domibus.web.rest.ro.MessageLogResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@ExtendWith(JMockitExtension.class)
public class MessagesLogServiceImplTest {

    @Tested
    private MessagesLogServiceImpl messagesLogServiceImpl;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private SignalMessageLogDao signalMessageLogDao;

    @Injectable
    private MessageCoreMapper messageCoreConverter;

    @Injectable
    MessagesLogServiceHelper messagesLogServiceHelper;

    @Injectable
    NonRepudiationService nonRepudiationService;

    @Injectable
    MessageLogDictionaryDataService messageLogDictionaryDataService;

    @Test
    public void countAndFilter1() {
        int from = 1, max = 20;
        String column = "col1";
        boolean asc = true;
        HashMap<String, Object> filters = new HashMap<>();
        long numberOfUserMessageLogs = 1;
        MessageLogInfo item1 = new MessageLogInfo();
        List<MessageLogInfo> resultList = Arrays.asList(item1);
        MessageLogResultRO resultRo = new MessageLogResultRO();

        new Expectations() {{
            messagesLogServiceHelper.calculateNumberOfMessages((MessageLogDaoBase) any, filters, (MessageLogResultRO) any);
            result = numberOfUserMessageLogs;
            userMessageLogDao.findAllInfoPaged(from, max, column, asc, filters, Collections.emptyList());
            result = resultList;
        }};

        List<MessageLogInfo> res = messagesLogServiceImpl.countAndFilter(userMessageLogDao, from, max, column, asc, filters, Collections.emptyList(), resultRo);

        new Verifications() {{
            userMessageLogDao.findAllInfoPaged(from, max, column, asc, filters, Collections.emptyList());
            times = 1;
        }};

        Assertions.assertEquals(numberOfUserMessageLogs, res.size());
    }

    @Test
    public void countAndFindPagedTest2() {
        int from = 2, max = 30;
        String column = "col1";
        boolean asc = true;
        MessageType messageType = MessageType.SIGNAL_MESSAGE;
        HashMap<String, Object> filters = new HashMap<>();
        MessageLogInfo item1 = new MessageLogInfo();
        List<MessageLogInfo> resultList = Arrays.asList(item1);

        new Expectations(messagesLogServiceImpl) {{
            messagesLogServiceImpl.countAndFilter((MessageLogDao) any, from, max, column, asc, filters, null, (MessageLogResultRO) any);
            result = resultList;
        }};

        MessageLogResultRO res = messagesLogServiceImpl.countAndFindPaged(messageType, from, max, column, asc, filters, Collections.emptyList());

        new Verifications() {{
            messagesLogServiceImpl.getMessageLogDao(messageType);
            times = 1;
        }};

        Assertions.assertEquals(resultList.size(), res.getMessageLogEntries().size());
    }

}
