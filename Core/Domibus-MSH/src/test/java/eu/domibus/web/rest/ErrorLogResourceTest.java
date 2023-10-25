package eu.domibus.web.rest;

import eu.domibus.api.csv.CsvException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MSHRoleEntity;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.converter.AuditLogCoreMapper;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.web.rest.ro.ErrorLogFilterRequestRO;
import eu.domibus.web.rest.ro.ErrorLogRO;
import eu.domibus.web.rest.ro.ErrorLogResultRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class ErrorLogResourceTest {

    private static final String CSV_TITLE = "Error Signal Message Id, Msh Role, Message In Error Id, Error Code, Error Detail, Timestamp, Notified";

    @Tested
    ErrorLogResource errorLogResource;

    @Injectable
    ErrorLogService errorLogService;

    @Injectable
    DateUtil dateUtil;

    @Injectable
    CsvServiceImpl csvServiceImpl;

    @Injectable
    AuditLogCoreMapper auditLogCoreMapper;

    @Test
    public void testGetErrorLog() {
        // Given
        final List<ErrorLogEntry> resultList = new ArrayList<>();
        ErrorLogEntry errorLogEntry = new ErrorLogEntry();
        errorLogEntry.setEntityId(1);
        errorLogEntry.setErrorCode(ErrorCode.EBMS_0001.getErrorCodeName());
        errorLogEntry.setErrorDetail("errorDetail");
        errorLogEntry.setErrorSignalMessageId("errorSignalMessageId");
        errorLogEntry.setMessageInErrorId("refToMessageId");
        MSHRoleEntity mshRole = new MSHRoleEntity();
        mshRole.setRole(MSHRole.RECEIVING);
        errorLogEntry.setMshRole(mshRole);
        errorLogEntry.setNotified(new Date());
        errorLogEntry.setTimestamp(new Date());
        resultList.add(errorLogEntry);

        new Expectations() {{
            errorLogService.countEntries((HashMap<String, Object>) any);
            result = 1;

            errorLogService.findPaged(anyInt, anyInt, anyString, anyBoolean, (HashMap<String, Object>) any);
            result = resultList;
        }};

        // When
        ErrorLogResultRO errorLogResultRO = errorLogResource.getErrorLog(new ErrorLogFilterRequestRO() {{
            setOrderBy("messageId");
        }});

        // Then
        Assertions.assertNotNull(errorLogResultRO);
        Assertions.assertEquals(new Integer(1), errorLogResultRO.getCount());
        Assertions.assertEquals(1, errorLogResultRO.getErrorLogEntries().size());
        ErrorLogRO errorLogRO = errorLogResultRO.getErrorLogEntries().get(0);
        Assertions.assertEquals(errorLogEntry.getErrorCode(), errorLogRO.getErrorCode());
        Assertions.assertEquals(errorLogEntry.getErrorDetail(), errorLogRO.getErrorDetail());
        Assertions.assertEquals(errorLogEntry.getErrorSignalMessageId(), errorLogRO.getErrorSignalMessageId());
        Assertions.assertEquals(errorLogEntry.getMessageInErrorId(), errorLogRO.getMessageInErrorId());
        Assertions.assertEquals(errorLogEntry.getMshRole(), errorLogRO.getMshRole());
        Assertions.assertEquals(errorLogEntry.getNotified(), errorLogRO.getNotified());
        Assertions.assertEquals(errorLogEntry.getTimestamp(), errorLogRO.getTimestamp());
    }

    @Test
    public void testGetCsv() throws CsvException {
        // Given
        Date date = new Date();
        List<ErrorLogEntry> errorLogEntries = new ArrayList<>();
        ErrorLogEntry errorLogEntry = new ErrorLogEntry();
        errorLogEntry.setEntityId(1);
        final String errorDetailStr = "ErrorDetail";
        final String signalMessageIdStr = "SignalMessageId";
        final String refToMessageIdStr = "RefToMessageId";
        errorLogEntry.setErrorDetail(errorDetailStr);
        errorLogEntry.setErrorSignalMessageId(signalMessageIdStr);
        errorLogEntry.setMessageInErrorId(refToMessageIdStr);
        errorLogEntry.setErrorCode(ErrorCode.EBMS_0001.getErrorCodeName());
        MSHRoleEntity mshRole = new MSHRoleEntity();
        mshRole.setRole(MSHRole.RECEIVING);
        errorLogEntry.setMshRole(mshRole);
        errorLogEntry.setTimestamp(date);
        errorLogEntry.setNotified(date);
        errorLogEntries.add(errorLogEntry);

        List<ErrorLogRO> errorLogROEntries = new ArrayList<>();
        ErrorLogRO errorLogRO = new ErrorLogRO();
        errorLogRO.setErrorDetail(errorDetailStr);
        errorLogRO.setErrorSignalMessageId(signalMessageIdStr);
        errorLogRO.setMessageInErrorId(refToMessageIdStr);
        errorLogRO.setErrorCode(ErrorCode.EBMS_0001.getErrorCodeName());
        errorLogRO.setMshRole(MSHRole.RECEIVING);
        errorLogRO.setTimestamp(date);
        errorLogRO.setNotified(date);
        errorLogROEntries.add(errorLogRO);
        new Expectations() {{
            errorLogService.findPaged(anyInt, anyInt, anyString, anyBoolean, (HashMap<String, Object>) any);
            result = errorLogEntries;

            auditLogCoreMapper.errorLogEntryListToErrorLogROList(errorLogEntries);
            result = errorLogROEntries;

            csvServiceImpl.exportToCSV(errorLogROEntries, ErrorLogRO.class, (Map<String, String>) any, (List<String>) any);
            result = CSV_TITLE +
                    signalMessageIdStr + "," + MSHRole.RECEIVING + "," + refToMessageIdStr + "," + ErrorCode.EBMS_0001.getErrorCodeName() + "," +
                    errorDetailStr + "," + date + "," + date + System.lineSeparator();
        }};

        // When
        final ResponseEntity<String> csv = errorLogResource.getCsv(new ErrorLogFilterRequestRO() {{
            setOrderBy("timestamp");
            setAsc(false);
        }});

        // Then
        Assertions.assertEquals(HttpStatus.OK, csv.getStatusCode());
        Assertions.assertEquals(CSV_TITLE +
                        signalMessageIdStr + "," + MSHRole.RECEIVING + "," + refToMessageIdStr + "," + ErrorCode.EBMS_0001.getErrorCodeName() + "," +
                        errorDetailStr + "," + date + "," + date + System.lineSeparator(),
                csv.getBody());
    }
}
