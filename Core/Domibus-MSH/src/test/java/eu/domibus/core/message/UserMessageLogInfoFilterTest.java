package eu.domibus.core.message;

import com.google.common.collect.ImmutableMap;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.core.message.dictionary.*;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
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
            "where message.messageInfo = info and propsFrom.name = 'originalSender'" +
            "and propsTo.name = 'finalRecipient'";

    @Injectable
    ServiceDao serviceDao;

    @Injectable
    PartyIdDao partyIdDao;

    @Injectable
    private MessageStatusDao messageStatusDao;

    @Injectable
    private MshRoleDao mshRoleDao;

    @Injectable
    private NotificationStatusDao notificationStatusDao;

    @Injectable
    private ActionDao actionDao;

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
        }};

        String query = userMessageLogInfoFilter.getFilterMessageLogQuery("column", true, filters, Collections.emptyList());

        Assertions.assertEquals(QUERY, query);
    }

    @Test
    public void testGetHQLKeyConversationId() {
        Assertions.assertEquals("message.conversationId", userMessageLogInfoFilter.getHQLKey("conversationId"));
    }

    @Test
    public void testGetHQLKeyMessageId() {
        Assertions.assertEquals("log.messageStatus", userMessageLogInfoFilter.getHQLKey("messageStatus"));
    }

    @Test
    public void testFilterQuery() {
        StringBuilder resultQuery = userMessageLogInfoFilter.filterQuery("select * from table where column = ''", "messageId", true, filters);
        String resultQueryString = resultQuery.toString();
        Assertions.assertTrue(resultQueryString.contains("log.notificationStatus = :notificationStatus"));
        Assertions.assertTrue(resultQueryString.contains("and message.partyInfo.from.fromPartyId IN :fromPartyId"));
        Assertions.assertTrue(resultQueryString.contains("log.sendAttemptsMax = :sendAttemptsMax"));
        Assertions.assertTrue(resultQueryString.contains("propsFrom.value = :originalSender"));
        Assertions.assertTrue(resultQueryString.contains("log.received <= :receivedTo"));
        Assertions.assertTrue(resultQueryString.contains("message.messageId = :messageId"));
        Assertions.assertTrue(resultQueryString.contains("message.refToMessageId = :refToMessageId"));
        Assertions.assertTrue(resultQueryString.contains("log.received = :received"));
        Assertions.assertTrue(resultQueryString.contains("log.sendAttempts = :sendAttempts"));
        Assertions.assertTrue(resultQueryString.contains("propsTo.value = :finalRecipient"));
        Assertions.assertTrue(resultQueryString.contains("log.nextAttempt = :nextAttempt"));
        Assertions.assertTrue(resultQueryString.contains("log.messageStatus = :messageStatus"));
        Assertions.assertTrue(resultQueryString.contains("log.deleted = :deleted"));
        Assertions.assertTrue(resultQueryString.contains("log.received >= :receivedFrom"));
        Assertions.assertTrue(resultQueryString.contains("message.partyInfo.to.toPartyId IN :toPartyId"));
        Assertions.assertTrue(resultQueryString.contains("log.mshRole = :mshRole"));
        Assertions.assertTrue(resultQueryString.contains("order by message.messageId asc"));
    }

    @Test
    public void testCountMessageLogQuery() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageId", "111",
                "fromPartyId", "222",
                "serviceType", "serviceType1",
                "serviceValue", "serviceValue1",
                "originalSender", "333");

        String result = userMessageLogInfoFilter.getCountMessageLogQuery(filters);

        Assertions.assertTrue(result.contains(userMessageLogInfoFilter.getMainTable()));
        Assertions.assertTrue(result.contains("left join message.messageProperties propsFrom"));
        Assertions.assertTrue(result.contains("propsFrom.name = 'originalSender'"));
        Assertions.assertFalse(result.contains("left join message.messageProperties propsTo"));
        Assertions.assertFalse(result.contains("left join message.partyInfo.from.fromPartyId"));
        Assertions.assertTrue(result.contains("message.partyInfo.from.fromPartyId IN :fromPartyId"));
        Assertions.assertFalse(result.contains("message.partyInfo.to.toPartyId IN :toPartyId"));
        Assertions.assertTrue(result.contains("message.service IN :serviceType"));
        Assertions.assertTrue(result.contains("message.service IN :serviceValue"));
    }

    @Test
    public void testCountMessageLogQuery1() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageId", "111",
                "fromPartyId", "222",
                "serviceType", "serviceType1",
                "serviceValue", "serviceValue1",
                "originalSender", "333");

        String result = userMessageLogInfoFilter.getFilterMessageLogQuery("column", true, filters, Collections.emptyList());

        Assertions.assertTrue(result.contains(userMessageLogInfoFilter.getMainTable()));
        Assertions.assertTrue(result.contains("left join message.messageProperties propsFrom"));
        Assertions.assertTrue(result.contains("propsFrom.name = 'originalSender'"));
        Assertions.assertFalse(result.contains("left join message.messageProperties propsTo"));
        Assertions.assertFalse(result.contains("left join message.partyInfo.from.fromPartyId"));
        Assertions.assertTrue(result.contains("message.partyInfo.from.fromPartyId IN :fromPartyId"));
        Assertions.assertFalse(result.contains("message.partyInfo.to.toPartyId IN :toPartyId"));
        Assertions.assertTrue(result.contains("message.service IN :serviceType"));
        Assertions.assertTrue(result.contains("message.service IN :serviceValue"));
    }
}
