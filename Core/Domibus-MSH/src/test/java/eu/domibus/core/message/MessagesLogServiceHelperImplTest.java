package eu.domibus.core.message;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.web.rest.ro.MessageLogResultRO;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_UI_MESSAGE_LOGS_COUNT_LIMIT;

/**
 * @author Ion Perpegel
 * @since 4.2.1
 */
@ExtendWith(JMockitExtension.class)
public class MessagesLogServiceHelperImplTest {

    @Tested
    MessagesLogServiceHelperImpl messagesLogServiceHelper;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void calculateNumberOfMessages_NotEstimated(@Injectable MessageLogDaoBase dao) {
        long count = 100;
        MessageLogResultRO resultRO = new MessageLogResultRO();
        HashMap<String, Object> filters = new HashMap<>();

        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_UI_MESSAGE_LOGS_COUNT_LIMIT);
            result = 0;
            dao.countEntries(filters);
            result = count;
        }};

        long result = messagesLogServiceHelper.calculateNumberOfMessages(dao, filters, resultRO);

        new Verifications() {{
            dao.hasMoreEntriesThan(filters, anyInt);
            times = 0;
        }};

        Assertions.assertEquals(count, result);
        Assertions.assertEquals(count, (long) resultRO.getCount());
        Assertions.assertEquals(false, resultRO.isEstimatedCount());
    }

    @Test
    public void calculateNumberOfMessages_Estimated(@Injectable MessageLogDaoBase dao) {
        long count = 1000;
        int limit = 100;
        MessageLogResultRO resultRO = new MessageLogResultRO();
        HashMap<String, Object> filters = new HashMap<>();

        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_UI_MESSAGE_LOGS_COUNT_LIMIT);
            this.result = limit;
            dao.hasMoreEntriesThan(filters, anyInt);
            result = true;
        }};

        long result = messagesLogServiceHelper.calculateNumberOfMessages(dao, filters, resultRO);

        new Verifications() {{
            dao.countEntries(filters);
            times = 0;
        }};

        Assertions.assertEquals(limit, result);
        Assertions.assertEquals(limit, (long) resultRO.getCount());
        Assertions.assertEquals(true, resultRO.isEstimatedCount());
    }
}
