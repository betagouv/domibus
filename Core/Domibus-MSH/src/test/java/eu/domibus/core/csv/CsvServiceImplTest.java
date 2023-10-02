package eu.domibus.core.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.domibus.api.csv.CsvException;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.NotificationStatus;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.routing.RoutingCriteria;
import eu.domibus.api.util.DomibusStringUtil;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.csv.serializer.*;
import eu.domibus.core.message.MessageLogInfo;
import eu.domibus.web.rest.ro.ErrorLogRO;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.LongSupplier;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_UI_CSV_MAX_ROWS;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Tiago Miguel
 * @since 4.0
 */

@ExtendWith(JMockitExtension.class)
public class CsvServiceImplTest {

    public static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2020, 1, 1, 12, 59);

    private static final String MESSAGE_FILTER_HEADER = "Plugin,From,To,Action,Service,Persisted";

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomibusStringUtil domibusStringUtil;

    @Injectable
    private Field field;

    private CsvServiceImpl csvServiceImpl;

    private void setCsvSerializer() {
        ReflectionTestUtils.setField(csvServiceImpl, "csvSerializers", Arrays.asList(
                new CsvSerializerDate(),
                new CsvSerializerErrorCode(),
                new CsvSerializerRoutingCriteria(),
                new CsvSerializerLocalDateTime(),
                new CsvSerializerMap(),
                new CsvSerializerNull()));
    }

    @BeforeEach
    void setUp() {
        csvServiceImpl = new CsvServiceImpl(domibusPropertyProvider, new ArrayList<>(), domibusStringUtil);
    }

    @Test
    public void getPageSizeForExport() {
        new Expectations(csvServiceImpl) {{
            csvServiceImpl.getMaxNumberRowsToExport();
            result = 1;
        }};

        int pageSizeForExport = csvServiceImpl.getPageSizeForExport();

        assertThat(pageSizeForExport, Is.is(2));

        new FullVerifications() {
        };
    }

    @Test
    public void testExportToCsv_EmptyList() throws CsvException {
        // Given
        // When
        final String exportToCSV = csvServiceImpl.exportToCSV(new ArrayList<>(), null, null, null);

        // Then
        Assertions.assertTrue(exportToCSV.trim().isEmpty());
    }

    @Test
    public void testExportToCsv_NullList() throws CsvException {
        // Given
        // When
        final String exportToCSV = csvServiceImpl.exportToCSV(null, null, null, null);

        // Then
        Assertions.assertTrue(exportToCSV.trim().isEmpty());
    }

    @Test
    @Ignore
    public void testExportToCsv() throws CsvException {
        testExportCsvBySubtype(null);
    }

    @Test
    @Ignore
    public void testExportToCsvTest() throws CsvException {
        testExportCsvBySubtype(true);
    }

    private void testExportCsvBySubtype(Boolean testMessage) {
        // Given
        Date date = new Date();
        List<MessageLogInfo> messageLogInfoList = getMessageList(date, testMessage);

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'GMT'Z");
        ZonedDateTime d = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        String csvDate = d.format(f);

        setCsvSerializer();

        List<Field> activeFields = new ArrayList<>();
        activeFields.add(field);
        setCsvSerializer();

        new Expectations(csvServiceImpl) {{
            field.getName();
            result = "messageId";
            csvServiceImpl.getExportedFields(messageLogInfoList, MessageLogInfo.class, null);
            result = activeFields;
            domibusStringUtil.unCamelCase("messageId");
            result = "Message Id";
        }};


        // When
        final String exportToCSV = csvServiceImpl.exportToCSV(messageLogInfoList, MessageLogInfo.class, null, null);

        // Then
        Assert.assertTrue(exportToCSV.contains(" Message Id,From Party Id,To Party Id,Message Status,Notification Status,Received,Msh Role,Send Attempts,Send Attempts Max,Next Attempt,Next Attempt Timezone Id,Next Attempt Offset Seconds,Conversation Id,Message Type,Test Message,Deleted,Original Sender,Final Recipient,Ref To Message Id,Failed,Restored,Message Fragment,Source Message,Action,Service Type,Service Value"));
        Assert.assertTrue(exportToCSV.contains("messageId,fromPartyId,toPartyId,ACKNOWLEDGED,NOTIFIED," + csvDate + ",RECEIVING,1,5," + csvDate + ",Europe/Brussels,3600,conversationId,USER_MESSAGE," + (testMessage != null ? testMessage : "") + "," + csvDate + ",originalSender,finalRecipient,refToMessageId," + csvDate + "," + csvDate));
    }

    private List<MessageLogInfo> getMessageList(Date date, Boolean testMessage) {
        List<MessageLogInfo> result = new ArrayList<>();
        MessageLogInfo messageLog = new MessageLogInfo("messageId", 1L, 1L, 1L,
                date, date, 1, 5, date, 1L,
                "conversationId", 1L, 1L, "originalSender", "finalRecipient",
                "refToMessageId", date, date, testMessage, false, false, 1L, 1L,
                "pluginType", 1L, date);
        result.add(messageLog);
        return result;
    }


    @Test
    void validateMaxRows() {
        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_UI_CSV_MAX_ROWS);
            result = 1000;
        }};

        Assertions.assertThrows(RequestValidationException.class, () -> csvServiceImpl.validateMaxRows(5000));

        new FullVerifications() {
        };

    }

    @Test
    public void validateMaxRows_ok() {
        new Expectations(csvServiceImpl) {{
            csvServiceImpl.validateMaxRows(5000, null);
        }};

        csvServiceImpl.validateMaxRows(5000);

        new FullVerifications() {
        };
    }

    @Test
    public void testValidateMaxRowsWithCount() {
        long actualCount = 8000L;
        LongSupplier actualCountSupplier = () -> actualCount;
        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_UI_CSV_MAX_ROWS);
            result = 1000;
        }};

        csvServiceImpl.validateMaxRows(1000, actualCountSupplier);

        try {
            csvServiceImpl.validateMaxRows(5000, actualCountSupplier);
            Assertions.fail("RequestValidationException not thrown");
        } catch (RequestValidationException ex) {
            Assertions.assertTrue(ex.getMessage().contains(actualCount + ""), "Row count present in message");
        }

        new FullVerifications() {
        };
    }

    @Test
    public void serializeFieldValue_null() throws NoSuchFieldException, IllegalAccessException, JsonProcessingException {
        TestCsvFields o = new TestCsvFields();

        Field declaredField = TestCsvFields.class.getDeclaredField("nullField");

        String s = csvServiceImpl.serializeFieldValue(declaredField, o);

        Assertions.assertEquals("", s);
    }

    @Test
    public void serializeFieldValue_Map() throws NoSuchFieldException, IllegalAccessException, JsonProcessingException {
        TestCsvFields o = new TestCsvFields();
        HashMap<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        o.setMapField(map);

        Field declaredField = TestCsvFields.class.getDeclaredField("mapField");

        setCsvSerializer();

        String s = csvServiceImpl.serializeFieldValue(declaredField, o);

        Assertions.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", s);

        new FullVerifications() {
        };
    }

    @Test
    public void serializeFieldValue_Date() throws NoSuchFieldException, IllegalAccessException, JsonProcessingException {
        TestCsvFields o = new TestCsvFields();
        o.setDateField(Date.from(LOCAL_DATE_TIME.atZone(ZoneOffset.UTC).toInstant()));

        Field declaredField = TestCsvFields.class.getDeclaredField("dateField");

        setCsvSerializer();

        String s = csvServiceImpl.serializeFieldValue(declaredField, o);

        Assertions.assertEquals("2020-01-01 12:59:00GMT+0000", s);

        new FullVerifications() {
        };
    }

    @Test
    public void serializeFieldValue_LocalDateTime() throws IllegalAccessException, NoSuchFieldException, JsonProcessingException {
        TestCsvFields o = new TestCsvFields();
        o.setLocalDateTimeField(LOCAL_DATE_TIME);

        Field declaredField = TestCsvFields.class.getDeclaredField("localDateTimeField");

        setCsvSerializer();

        String s = csvServiceImpl.serializeFieldValue(declaredField, o);

        Assertions.assertEquals("2020-01-01 12:59:00GMT+0000", s);

        new FullVerifications() {
        };
    }

    @Test
    public void serializeFieldValue_Objects() throws IllegalAccessException, NoSuchFieldException, JsonProcessingException {
        TestCsvFields o = new TestCsvFields();
        o.setStringField("TEST");

        Field declaredField = TestCsvFields.class.getDeclaredField("stringField");

        setCsvSerializer();

        String s = csvServiceImpl.serializeFieldValue(declaredField, o);

        Assertions.assertEquals("TEST", s);

        new FullVerifications() {
        };
    }

    @Test
    void createCSVContents() throws IllegalAccessException, NoSuchFieldException, JsonProcessingException {
        Object o = new Object();
        Field declaredField = TestCsvFields.class.getDeclaredField("stringField");

        new Expectations(csvServiceImpl) {{
            csvServiceImpl.serializeFieldValue(declaredField, o);
            result = new IllegalAccessException("TEST");
        }};

        Assertions.assertThrows(CsvException.class,
                () -> csvServiceImpl.createCSVContents(Collections.singletonList(o), null, Collections.singletonList(declaredField)))
        ;

        new FullVerifications() {
        };
    }

    @Test
    public void getCsvFilename() {
        String test = csvServiceImpl.getCsvFilename("test", "");
        assertThat(test, CoreMatchers.containsString("test_datatable_"));
        assertThat(test, CoreMatchers.containsString(".csv"));
    }

    @Test
    public void getCsvFilename_domain() {
        String test = csvServiceImpl.getCsvFilename("test", "default");
        assertThat(test, CoreMatchers.containsString("default"));
        assertThat(test, CoreMatchers.containsString("test_datatable_"));
        assertThat(test, CoreMatchers.containsString(".csv"));
    }

    @Test
    public void testExportToCsv_ErrorLog(@Injectable Field field) throws CsvException {
        // Given
        Date date = new Date();
        List<ErrorLogRO> errorLogROList = getErrorLogList(date);

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'GMT'Z");
        ZonedDateTime d = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
        String csvDate = d.format(f);
        List<Field> activeFields = new ArrayList<>();
        activeFields.add(field);
        setCsvSerializer();

        new Expectations(csvServiceImpl) {{
            field.getName();
            result = "errorSignalMessageId";

            csvServiceImpl.getExportedFields(errorLogROList, ErrorLogRO.class, null);
            result = activeFields;

            domibusStringUtil.unCamelCase("errorSignalMessageId");
            result = "Error Signal Message Id";
        }};

        // When
        final String exportToCSV = csvServiceImpl.exportToCSV(errorLogROList, ErrorLogRO.class, null, null);

        // Then
        Assertions.assertTrue(exportToCSV.contains("Error Signal Message Id"));
    }

    @Test
    public void testExportToCsv_messageFilterROL() throws CsvException {
        // Given
        List<MessageFilterCSV> messageFilterROList = new ArrayList<>();
        MessageFilterCSV messageFilterRO = new MessageFilterCSV();
        messageFilterRO.setPlugin("backendName");
        RoutingCriteria fromRoutingCriteria = new RoutingCriteria();
        fromRoutingCriteria.setName("from");
        fromRoutingCriteria.setExpression("from:from");
        fromRoutingCriteria.setEntityId("1");
        messageFilterRO.setPersisted(true);
        messageFilterRO.setFrom(fromRoutingCriteria);
        messageFilterROList.add(messageFilterRO);

        setCsvSerializer();

        // When
        final String exportToCSV = csvServiceImpl.exportToCSV(messageFilterROList, MessageFilterCSV.class, null, null);

        // Then
        assertThat(exportToCSV, CoreMatchers.containsString("backendName,from:from,,,,true"));
    }

    private List<ErrorLogRO> getErrorLogList(Date date) {
        List<ErrorLogRO> result = new ArrayList<>();
        ErrorLogRO errorLogRO = new ErrorLogRO();
        errorLogRO.setErrorCode(ErrorCode.EBMS_0001);
        errorLogRO.setErrorDetail("errorDetail");
        errorLogRO.setErrorSignalMessageId("signalMessageId");
        errorLogRO.setMessageInErrorId("messageInErrorId");
        errorLogRO.setMshRole(MSHRole.RECEIVING);
        errorLogRO.setNotified(date);
        errorLogRO.setTimestamp(date);
        result.add(errorLogRO);
        return result;
    }

    static class TestCsvFields {
        public Object nullField = null;
        public String stringField = null;
        public Map<String, String> mapField = null;
        public Date dateField = null;
        public LocalDateTime localDateTimeField = null;

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }

        public void setMapField(Map<String, String> mapField) {
            this.mapField = mapField;
        }

        public void setDateField(Date dateField) {
            this.dateField = dateField;
        }

        public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
            this.localDateTimeField = localDateTimeField;
        }
    }
}
