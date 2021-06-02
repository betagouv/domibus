package eu.domibus.core.replication;

import eu.domibus.core.converter.MessageCoreMapper;
import eu.domibus.core.message.MessageLogDaoBase;
import eu.domibus.core.message.MessagesLogServiceHelper;
import eu.domibus.web.rest.ro.MessageLogResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Catalin Enache
 * @since 4.1
 */
public class UIMessageServiceImplTest {

    @Injectable
    private UIMessageDao uiMessageDao;

    @Injectable
    private MessageCoreMapper messageCoreConverter;

    @Injectable
    MessagesLogServiceHelper messagesLogServiceHelper;

    @Tested
    UIMessageServiceImpl uiMessageService;

    private final int from = 0, max = 10;
    private final String column = "received";
    private final boolean asc = true;
    private final Map<String, Object> filters = new HashMap<>();
    private final UIMessageEntity uiMessageEntity = new UIMessageEntity();
    private final List<UIMessageEntity> uiMessageEntityList = Collections.singletonList(uiMessageEntity);

    @Test
    public void testFindPaged() {

        new Expectations() {{
            uiMessageDao.findPaged(from, max, column, asc, filters);
            result = uiMessageEntityList;
        }};

        //tested method
        uiMessageService.findPaged(from, max, column, asc, filters);

        new Verifications() {{
            messageCoreConverter.uiMessageEntityToMessageLogInfo(withAny(new UIMessageEntity()));
            times = 1;
        }};
    }

    @Test
    public void testCountAndFindPaged() {
        final long count = 20;

        new Expectations() {{
            messagesLogServiceHelper.calculateNumberOfMessages((MessageLogDaoBase)any, filters, (MessageLogResultRO)any);
            result = count;

            uiMessageDao.findPaged(from, max, column, asc, filters);
            result = uiMessageEntityList;
        }};

        //tested method
        final MessageLogResultRO messageLogResultRO = uiMessageService.countAndFindPaged(from, max, column, asc, filters);
        Assert.assertNotNull(messageLogResultRO);
        Assert.assertEquals(uiMessageEntityList.size(), messageLogResultRO.getMessageLogEntries().size());

        new Verifications() {{
            messageCoreConverter.uiMessageEntityToMessageLogRO(withAny(new UIMessageEntity()));
            times = 1;
        }};
    }

    @Test
    public void testSaveOrUpdate() {

        //tested method
        uiMessageService.saveOrUpdate(uiMessageEntity);

        new Verifications() {{
            uiMessageDao.saveOrUpdate(uiMessageEntity);
        }};
    }



}