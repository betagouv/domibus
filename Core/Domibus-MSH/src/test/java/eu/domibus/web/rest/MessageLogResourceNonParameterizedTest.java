package eu.domibus.web.rest;

import com.google.common.collect.Sets;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.util.DateUtil;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.message.MessagesLogService;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.core.party.PartyDao;
import eu.domibus.web.rest.ro.MessageLogFilterRequestRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.domibus.core.message.MessageLogInfoFilter.*;

@ExtendWith(JMockitExtension.class)
public class MessageLogResourceNonParameterizedTest {

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

    @Test
    public void getCsv_fourCornersModeEnabled(@Injectable MessageLogFilterRequestRO messageLogFilter) {
        // GIVEN
        new Expectations(messageLogResource) {{
            requestFilterUtils.createFilterMap(messageLogFilter);
            result = createFilterMap();
            domibusConfigurationService.isFourCornerEnabled();
            result = true;
            messageLogResource.exportToCSV((List<?>) any, (Class<?>) any, (Map<String, String>) any, (List<String>) any, anyString);
            result = any;
        }};

        // WHEN
        messageLogResource.getCsv(messageLogFilter);

        // THEN
        new Verifications() {{
            List<String> excludedColumns;
            messageLogResource.exportToCSV((List<?>) any, (Class<?>) any, (Map<String, String>) any, excludedColumns = withCapture(), anyString);

            Assertions.assertTrue(excludedColumns.stream().allMatch(excludedColumn -> !Sets.newHashSet("originalSender", "finalRecipient").contains(excludedColumn)),
                    "Should have not excluded the Original Sender and the Final Recipient columns when the four corners mode is enabled");
        }};
    }

    @Test
    public void getCsv_fourCornersModeDisabled(@Injectable MessageLogFilterRequestRO messageLogFilter) {

        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        // GIVEN
        new Expectations(messageLogResource) {{
            requestFilterUtils.createFilterMap(messageLogFilter);
            result = createFilterMap();
            domibusConfigurationService.isFourCornerEnabled();
            result = false;
            messageLogResource.exportToCSV((List<?>) any, (Class<?>) any, (Map<String, String>) any, (List<String>) any, anyString);
            result = any;
        }};

        // WHEN
        messageLogResource.getCsv(messageLogFilter);

        // THEN
        new Verifications() {{
            List<String> excludedColumns;
            messageLogResource.exportToCSV((List<?>) any, (Class<?>) any, (Map<String, String>) any, excludedColumns = withCapture(), anyString);

            Assertions.assertTrue(excludedColumns.containsAll(Sets.newHashSet("originalSender", "finalRecipient")),
                    "Should have excluded the Original Sender and the Final Recipient columns when the four corners mode is disabled");
        }};
    }

    protected HashMap<String, Object> createFilterMap() {
        HashMap<String, Object> filters = new HashMap<>();
        filters.put(MESSAGE_ACTION, "request.getAction()");
        filters.put(MESSAGE_SERVICE_TYPE, "request.getServiceType()");
        filters.put(MESSAGE_SERVICE_VALUE, "request.getServiceValue()");
        return filters;
    }
}
