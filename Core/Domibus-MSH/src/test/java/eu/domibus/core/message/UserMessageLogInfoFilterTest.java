package eu.domibus.core.message;

import com.google.common.collect.ImmutableMap;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.TsidUtil;
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
public class UserMessageLogInfoFilterTest {

    private static final String QUERY = "select new eu.domibus.core.message.MessageLogInfo(log, message.collaborationInfo.conversationId, partyFrom.value, partyTo.value, propsFrom.value, propsTo.value, info.refToMessageId) from UserMessageLog log, " +
            "UserMessage message " +
            "left join log.messageInfo info " +
            "left join message.messageProperties.property propsFrom " +
            "left join message.messageProperties.property propsTo " +
            "left join message.partyInfo.from.fromPartyId partyFrom " +
            "left join message.partyInfo.to.toPartyId partyTo " +
            "where message.messageInfo = info and propsFrom.name = 'originalSender'" +
            "and propsTo.name = 'finalRecipient'";

    @Injectable
    private DomibusPropertyProvider domibusProperties;

    @Injectable
    TsidUtil tsidUtil;

    @Tested
    UserMessageLogInfoFilter userMessageLogInfoFilter;

    private static HashMap<String, Object> filters = new HashMap<>();

    @BeforeAll
    public static void before() {
        filters = MessageLogInfoFilterTest.returnFilters();
    }

    @Test
    public void createUserMessageLogInfoFilter() {
        new Expectations(userMessageLogInfoFilter) {{
            userMessageLogInfoFilter.filterQuery(anyString, anyString, anyBoolean, filters);
            result = QUERY;

            userMessageLogInfoFilter.isFourCornerModel();
            result = true;
        }};

        String query = userMessageLogInfoFilter.filterMessageLogQuery("column", true, filters);

        Assertions.assertEquals(QUERY, query);
    }

    @Test
    public void testGetHQLKeyConversationId() {
        Assertions.assertEquals("message.conversationId", userMessageLogInfoFilter.getHQLKey("conversationId"));
    }

    @Test
    public void testGetHQLKeyMessageId() {
        Assertions.assertEquals("log.messageStatus.messageStatus", userMessageLogInfoFilter.getHQLKey("messageStatus"));
    }

    @Test
    public void testFilterQuery() {
        StringBuilder resultQuery = userMessageLogInfoFilter.filterQuery("select * from table where column = ''", "messageId", true, filters);
        String resultQueryString = resultQuery.toString();
        Assertions.assertTrue(resultQueryString.contains("log.notificationStatus.status = :notificationStatus"));
        Assertions.assertTrue(resultQueryString.contains("partyFrom.value = :fromPartyId"));
        Assertions.assertTrue(resultQueryString.contains("log.sendAttemptsMax = :sendAttemptsMax"));
        Assertions.assertTrue(resultQueryString.contains("propsFrom.value = :originalSender"));
        Assertions.assertTrue(resultQueryString.contains("log.received <= :receivedTo"));
        Assertions.assertTrue(resultQueryString.contains("message.messageId = :messageId"));
        Assertions.assertTrue(resultQueryString.contains("message.refToMessageId = :refToMessageId"));
        Assertions.assertTrue(resultQueryString.contains("log.received = :received"));
        Assertions.assertTrue(resultQueryString.contains("log.sendAttempts = :sendAttempts"));
        Assertions.assertTrue(resultQueryString.contains("propsTo.value = :finalRecipient"));
        Assertions.assertTrue(resultQueryString.contains("log.nextAttempt = :nextAttempt"));
        Assertions.assertTrue(resultQueryString.contains("log.messageStatus.messageStatus = :messageStatus"));
        Assertions.assertTrue(resultQueryString.contains("log.deleted = :deleted"));
        Assertions.assertTrue(resultQueryString.contains("log.received >= :receivedFrom"));
        Assertions.assertTrue(resultQueryString.contains("partyTo.value = :toPartyId"));
        Assertions.assertTrue(resultQueryString.contains("log.mshRole.role = :mshRole"));
        Assertions.assertTrue(resultQueryString.contains("order by message.messageId asc"));
    }

    @Test
    public void createFromClause_MessageTableNotDirectly() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageId", "111",
                "fromPartyId", "222",
                "originalSender", "333");
        String messageTable = "join log.userMessage message";
        String partyFromTable = "left join message.partyInfo.from.fromPartyId partyFrom ";
        String propsCriteria = "and propsFrom.name = 'originalSender' ";

        String result = userMessageLogInfoFilter.getCountQueryBody(filters);

        Assertions.assertTrue(result.contains(userMessageLogInfoFilter.getMainTable()));
        Assertions.assertTrue(result.contains(messageTable));
        Assertions.assertTrue(result.contains(partyFromTable));
        Assertions.assertTrue(result.contains(propsCriteria));
    }
}
