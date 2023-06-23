package eu.domibus.core.message.signal;

import com.google.common.collect.ImmutableMap;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.message.MessageLogInfoFilterTest;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class SignalMessageLogInfoFilterTest {

    @Injectable
    private DomibusPropertyProvider domibusProperties;

    public static final String QUERY = "select new eu.domibus.core.message.MessageLogInfo(log, partyFrom.value, partyTo.value, propsFrom.value, propsTo.value, info.refToMessageId) from SignalMessageLog log, " +
            "SignalMessage message " +
            "left join log.messageInfo info " +
            "left join message.messageProperties.property propsFrom " +
            "left join message.messageProperties.property propsTo " +
            "left join message.partyInfo.from.fromPartyId partyFrom " +
            "left join message.partyInfo.to.toPartyId partyTo " +
            "where message.messageInfo = info and propsFrom.name = 'originalSender'" +
            "and propsTo.name = 'finalRecipient'";

    @Tested
    SignalMessageLogInfoFilter signalMessageLogInfoFilter;

    private static HashMap<String, Object> filters = new HashMap<>();

    @BeforeAll
    public static void before() {
        filters = MessageLogInfoFilterTest.returnFilters();
    }

    @Test
    public void createSignalMessageLogInfoFilter() {

        new Expectations(signalMessageLogInfoFilter) {{
            signalMessageLogInfoFilter.filterQuery(anyString,anyString,anyBoolean,filters);
            result = QUERY;

            signalMessageLogInfoFilter.isFourCornerModel();
            result = true;
        }};

        String query = signalMessageLogInfoFilter.filterMessageLogQuery("column", true, filters);

        Assertions.assertEquals(QUERY, query);
    }

    @Test
    public void testGetHQLKeyConversationId() {
        Assertions.assertEquals("", signalMessageLogInfoFilter.getHQLKey("conversationId"));
    }

    @Test
    public void testGetHQLKeyMessageId() {
        Assertions.assertEquals("signal.signalMessageId", signalMessageLogInfoFilter.getHQLKey("messageId"));
    }

    @Test
    public void testFilterQuery() {
        StringBuilder resultQuery = signalMessageLogInfoFilter.filterQuery("select * from table where column = ''","messageId", true, filters);
        String resultQueryString = resultQuery.toString();

        Assertions.assertFalse(resultQueryString.contains("log.notificationStatus.status = :notificationStatus"));
        Assertions.assertTrue(resultQueryString.contains("partyFrom.value = :fromPartyId"));
        Assertions.assertFalse(resultQueryString.contains("log.sendAttemptsMax = :sendAttemptsMax"));
        Assertions.assertTrue(resultQueryString.contains("propsFrom.value = :originalSender"));
        Assertions.assertTrue(resultQueryString.contains("log.received <= :receivedTo"));
        Assertions.assertTrue(resultQueryString.contains("signal.signalMessageId = :messageId"));
        Assertions.assertTrue(resultQueryString.contains("signal.refToMessageId = :refToMessageId"));
        Assertions.assertTrue(resultQueryString.contains("log.received = :received"));
        Assertions.assertFalse(resultQueryString.contains("log.sendAttempts = :sendAttempts"));
        Assertions.assertTrue(resultQueryString.contains("propsTo.value = :finalRecipient"));
        Assertions.assertFalse(resultQueryString.contains("log.nextAttempt = :nextAttempt"));
        Assertions.assertTrue(resultQueryString.contains("log.messageStatus.messageStatus = :messageStatus"));
        Assertions.assertTrue(resultQueryString.contains("log.deleted = :deleted"));
        Assertions.assertTrue(resultQueryString.contains("log.received >= :receivedFrom"));
        Assertions.assertTrue(resultQueryString.contains("partyTo.value = :toPartyId"));
        Assertions.assertTrue(resultQueryString.contains("log.mshRole.role = :mshRole"));
        Assertions.assertTrue(resultQueryString.contains("order by signal.signalMessageId asc"));

        Assertions.assertFalse(resultQueryString.contains("conversationId"));
        Assertions.assertFalse(resultQueryString.contains("message.collaborationInfo.conversationId"));
    }

    @Test
    public void createFromClause_MessageTableNotDirectly() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageId", "111",
                "fromPartyId", "222",
                "originalSender", "333");

        String messageTable = "join log.signalMessage signal join signal.userMessage message";
        String partyFromTable = "left join message.partyInfo.from.fromPartyId partyFrom ";
        String propsCriteria = "and propsFrom.name = 'originalSender' ";

        String result = signalMessageLogInfoFilter.getCountQueryBody(filters);

        Assertions.assertTrue(result.contains(signalMessageLogInfoFilter.getMainTable()));
        Assertions.assertTrue(result.contains(messageTable));
        Assertions.assertTrue(result.contains(partyFromTable));
        Assertions.assertTrue(result.contains(propsCriteria));
    }
}
