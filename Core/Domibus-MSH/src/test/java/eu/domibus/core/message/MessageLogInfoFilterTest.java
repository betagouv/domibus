package eu.domibus.core.message;

import com.google.common.collect.ImmutableMap;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.TypedQuery;
import java.util.*;

import static org.mockito.Mockito.spy;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class MessageLogInfoFilterTest {

    public static final String QUERY1 = "select * from table where z = 1 and log.notificationStatus = :notificationStatus and partyFrom.value = :fromPartyId and log.sendAttemptsMax = :sendAttemptsMax and propsFrom.value = :originalSender and log.received <= :receivedTo and message.collaborationInfo.conversationId = :conversationId and log.messageId = :messageId and info.refToMessageId = :refToMessageId and log.received = :received and log.sendAttempts = :sendAttempts and propsTo.value = :finalRecipient and log.nextAttempt = :nextAttempt and log.messageStatus = :messageStatus and log.deleted = :deleted and log.messageType = :messageType and log.received >= :receivedFrom and partyTo.value = :toPartyId and log.mshRole = :mshRole order by log.messageStatus";

    @Tested
    MessageLogInfoFilter messageLogInfoFilter;

    @Injectable
    private DomibusPropertyProvider domibusProperties;

    public static HashMap<String, Object> returnFilters() {
        HashMap<String, Object> filters = new HashMap<>();

        filters.put("conversationId", "CONVERSATIONID");
        filters.put("messageId", "MESSAGEID");
        filters.put("mshRole", "MSHROLE");
        filters.put("messageStatus", "MESSAGESTATUS");
        filters.put("notificationStatus", "NOTIFICATIONSTATUS");
        filters.put("deleted", "DELETED");
        filters.put("received", "RECEIVED");
        filters.put("sendAttempts", "SENDATTEMPTS");
        filters.put("sendAttemptsMax", "SENDATTEMPTSMAX");
        filters.put("nextAttempt", "NEXTATTEMPT");
        filters.put("fromPartyId", "FROMPARTYID");
        filters.put("toPartyId", "TOPARTYID");
        filters.put("refToMessageId", "REFTOMESSAGEID");
        filters.put("originalSender", "ORIGINALSENDER");
        filters.put("finalRecipient", "FINALRECIPIENT");
        filters.put("receivedFrom", new Date());
        filters.put("receivedTo", new Date());

        return filters;
    }


    @Test
    public void testGetHQLKeyMessageStatus() {
        Assertions.assertEquals("log.messageStatus.messageStatus", messageLogInfoFilter.getHQLKey("messageStatus"));
    }

    @Test
    public void testGetHQLKeyFromPartyId() {
        Assertions.assertEquals("partyFrom.value", messageLogInfoFilter.getHQLKey("fromPartyId"));
    }

    @Test
    public void testFilterQueryDesc() {
        StringBuilder filterQuery = messageLogInfoFilter.filterQuery("select * from table where z = 1", "messageStatus", false, returnFilters());

        String filterQueryString = filterQuery.toString();
        Assertions.assertTrue(filterQueryString.contains("log.notificationStatus.status = :notificationStatus"));
        Assertions.assertTrue(filterQueryString.contains("partyFrom.value = :fromPartyId"));
        Assertions.assertTrue(filterQueryString.contains("log.sendAttemptsMax = :sendAttemptsMax"));
        Assertions.assertTrue(filterQueryString.contains("propsFrom.value = :originalSender"));
        Assertions.assertTrue(filterQueryString.contains("log.received <= :receivedTo"));
        Assertions.assertTrue(filterQueryString.contains("message.messageId = :messageId"));
        Assertions.assertTrue(filterQueryString.contains("message.refToMessageId = :refToMessageId"));
        Assertions.assertTrue(filterQueryString.contains("log.received = :received"));
        Assertions.assertTrue(filterQueryString.contains("log.sendAttempts = :sendAttempts"));
        Assertions.assertTrue(filterQueryString.contains("propsTo.value = :finalRecipient"));
        Assertions.assertTrue(filterQueryString.contains("log.nextAttempt = :nextAttempt"));
        Assertions.assertTrue(filterQueryString.contains("log.messageStatus.messageStatus = :messageStatus"));
        Assertions.assertTrue(filterQueryString.contains("log.deleted = :deleted"));
        Assertions.assertTrue(filterQueryString.contains("log.received >= :receivedFrom"));
        Assertions.assertTrue(filterQueryString.contains("partyTo.value = :toPartyId"));
        Assertions.assertTrue(filterQueryString.contains("log.mshRole.role = :mshRole"));

        Assertions.assertTrue(filterQueryString.contains("log.messageStatus.messageStatus desc"));
    }

    @Test
    public void testFilterQueryAsc() {
        StringBuilder filterQuery = messageLogInfoFilter.filterQuery("select * from table where z = 1", "messageStatus", true, returnFilters());

        String filterQueryString = filterQuery.toString();
        Assertions.assertTrue(filterQueryString.contains("log.notificationStatus.status = :notificationStatus"));
        Assertions.assertTrue(filterQueryString.contains("partyFrom.value = :fromPartyId"));
        Assertions.assertTrue(filterQueryString.contains("log.sendAttemptsMax = :sendAttemptsMax"));
        Assertions.assertTrue(filterQueryString.contains("propsFrom.value = :originalSender"));
        Assertions.assertTrue(filterQueryString.contains("log.received <= :receivedTo"));
        Assertions.assertTrue(filterQueryString.contains("message.messageId = :messageId"));
        Assertions.assertTrue(filterQueryString.contains("message.refToMessageId = :refToMessageId"));
        Assertions.assertTrue(filterQueryString.contains("log.received = :received"));
        Assertions.assertTrue(filterQueryString.contains("log.sendAttempts = :sendAttempts"));
        Assertions.assertTrue(filterQueryString.contains("propsTo.value = :finalRecipient"));
        Assertions.assertTrue(filterQueryString.contains("log.nextAttempt = :nextAttempt"));
        Assertions.assertTrue(filterQueryString.contains("log.messageStatus.messageStatus = :messageStatus"));
        Assertions.assertTrue(filterQueryString.contains("log.deleted = :deleted"));
        Assertions.assertTrue(filterQueryString.contains("log.received >= :receivedFrom"));
        Assertions.assertTrue(filterQueryString.contains("partyTo.value = :toPartyId"));
        Assertions.assertTrue(filterQueryString.contains("log.mshRole.role = :mshRole"));

        Assertions.assertTrue(filterQueryString.contains("log.messageStatus.messageStatus asc"));
    }

    @Test
    public void testApplyParameters() {
        TypedQuery<MessageLogInfo> typedQuery = spy(TypedQuery.class);
        TypedQuery<MessageLogInfo> messageLogInfoTypedQuery = messageLogInfoFilter.applyParameters(typedQuery, returnFilters());
    }


    @Test
    public void getCountMessageLogQuery() {
        String result = messageLogInfoFilter.getCountMessageLogQuery(new HashMap<>());
        Assertions.assertTrue(result.contains("select count"));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getMessageLogIdQuery() {
        String result = messageLogInfoFilter.getMessageLogIdQuery(new HashMap<>());
        Assertions.assertTrue(result.contains("select log.id"));
    }

    @Test
    public void getQuery() {
        String selectExpression = "selectExpression";
        String countQueryBody = "countQueryBody";
        StringBuilder resultQuery = new StringBuilder("resultQuery");
        HashMap<String, Object> filters = new HashMap<>();

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.getCountQueryBody(filters);
            result = countQueryBody;
            messageLogInfoFilter.filterQuery(selectExpression + countQueryBody, null, false, filters);
            result = resultQuery;
        }};

        String result = messageLogInfoFilter.getQuery(filters, selectExpression);
        Assertions.assertEquals(resultQuery.toString(), result);
    }

    @Test
    public void getCountQueryBody() {
        StringBuilder fromQuery = new StringBuilder("fromQuery");
        StringBuilder whereQuery = new StringBuilder("whereQuery");
        HashMap<String, Object> filters = new HashMap<>();

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.getNonEmptyParams(filters);
            result = filters;
            messageLogInfoFilter.createFromClause(filters);
            result = fromQuery;
            messageLogInfoFilter.createWhereQuery(fromQuery);
            result = whereQuery;
        }};

        String result = messageLogInfoFilter.getCountQueryBody(new HashMap<>());
        Assertions.assertEquals("fromQuery where whereQuery", result);
    }

    @Test
    public void createFromClause_MainTableOnly() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageStatus", "SEND_FAILURE",
                "mshRole", "AP Role",
                "messageType", "type1");

        String mainTable = "UserMessageLog log ";

        String messageTable = ", UserMessage message left join log.messageInfo info ";
        Map<String, List<String>> mappings = ImmutableMap.of(
                "message", Arrays.asList(messageTable),
                "info", Arrays.asList(messageTable));

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.createFromMappings();
            result = mappings;
            messageLogInfoFilter.getMainTable();
            this.result = mainTable;
        }};

        StringBuilder result = messageLogInfoFilter.createFromClause(filters);
        Assertions.assertTrue(result.toString().contains(mainTable));
        Assertions.assertFalse(result.toString().contains(messageTable));
    }

    @Test
    public void createFromClause_MessageTableDirectly() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageId", "111",
                "refToMessageId", "222");

        String mainTable = "UserMessageLog log ";

        String messageTable = ", UserMessage message left join log.messageInfo info ";
        Map<String, List<String>> mappings = ImmutableMap.of(
                "message", Arrays.asList(messageTable),
                "info", Arrays.asList(messageTable));

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.createFromMappings();
            result = mappings;
            messageLogInfoFilter.getMainTable();
            this.result = mainTable;
        }};

        StringBuilder result = messageLogInfoFilter.createFromClause(filters);
        Assertions.assertTrue(result.toString().contains(mainTable));
        Assertions.assertTrue(result.toString().contains(messageTable));
    }

    @Test
    public void createFromClause_MessageTableNotDirectly() {
        Map<String, Object> filters = ImmutableMap.of(
                "messageId", "111",
                "fromPartyId", "222");

        String mainTable = "UserMessageLog log ";

        String messageTable = ", UserMessage message left join log.messageInfo info ";
        String partyFromTable = "left join message.partyInfo.from.fromPartyId partyFrom ";
        Map<String, List<String>> mappings = ImmutableMap.of(
                "message", Arrays.asList(messageTable),
                "info", Arrays.asList(messageTable),
                "partyFrom", Arrays.asList(messageTable, partyFromTable));

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.createFromMappings();
            result = mappings;
            messageLogInfoFilter.getMainTable();
            this.result = mainTable;
        }};

        StringBuilder result = messageLogInfoFilter.createFromClause(filters);
        Assertions.assertTrue(result.toString().contains(mainTable));
        Assertions.assertTrue(result.toString().contains(messageTable));
        Assertions.assertTrue(result.toString().contains(partyFromTable));
    }

    @Test
    public void createWhereQuery_MainTableOnly() {
        StringBuilder fromQuery = new StringBuilder(" from UserMessageLog log ");

        String messageCriteria = "message.messageInfo = info ";
        Map<String, List<String>> mappings = ImmutableMap.of(
                "message", Arrays.asList(messageCriteria),
                "propsFrom", Arrays.asList(messageCriteria, "and propsFrom.name = 'originalSender' "));

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.createWhereMappings();
            result = mappings;
        }};

        StringBuilder result = messageLogInfoFilter.createWhereQuery(fromQuery);
        Assertions.assertTrue(StringUtils.isEmpty(result.toString()));
    }

    @Test
    public void createWhereQuery_MessageTableDirectly() {
        StringBuilder fromQuery = new StringBuilder(" from UserMessageLog log , UserMessage message left join log.messageInfo info ");

        String messageCriteria = "message.messageInfo = info ";
        Map<String, List<String>> mappings = ImmutableMap.of(
                "message", Arrays.asList(messageCriteria),
                "propsFrom", Arrays.asList(messageCriteria, "and propsFrom.name = 'originalSender' "));

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.createWhereMappings();
            result = mappings;
        }};

        StringBuilder result = messageLogInfoFilter.createWhereQuery(fromQuery);
        Assertions.assertTrue(result.toString().contains(messageCriteria));
    }

    @Test
    public void createWhereQuery_MessageTableNotDirectly() {
        StringBuilder fromQuery = new StringBuilder(" from UserMessageLog log , UserMessage message left join log.messageInfo info left join message.messageProperties.property propsFrom ");

        String messageCriteria = "message.messageInfo = info ";
        String propsCriteria = "and propsFrom.name = 'originalSender' ";
        Map<String, List<String>> mappings = ImmutableMap.of(
                "message", Arrays.asList(messageCriteria),
                "propsFrom", Arrays.asList(messageCriteria, propsCriteria));

        new Expectations(messageLogInfoFilter) {{
            messageLogInfoFilter.createWhereMappings();
            result = mappings;
        }};

        StringBuilder result = messageLogInfoFilter.createWhereQuery(fromQuery);
        Assertions.assertTrue(result.toString().contains(messageCriteria));
        Assertions.assertTrue(result.toString().contains(propsCriteria));

    }
}
