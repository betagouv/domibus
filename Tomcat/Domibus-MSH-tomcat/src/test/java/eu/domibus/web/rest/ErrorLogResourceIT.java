package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MSHRole;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.error.ErrorLogDao;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.common.CsvUtil;
import eu.domibus.web.rest.ro.ErrorLogFilterRequestRO;
import eu.domibus.web.rest.ro.ErrorLogResultRO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ErrorLogResourceIT extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    UserMessageDao userMessageDao;

    @Autowired
    ErrorLogResource errorLogResource;

    @Autowired
    ErrorLogDao errorLogDao;

    @Autowired
    MshRoleDao mshRoleDao;

    @Autowired
    CsvUtil csvUtil;

    private final String mockMessageId = "9008713e-1912-460c-97b3-40ec12a29f49@domibus.eu";

    @BeforeEach
    public void setUp() {
        createEntries();
        this.mockMvc = MockMvcBuilders.standaloneSetup(errorLogResource)
                .build();
    }

    @Test
    void testFindErrorLogEntries() {
        ErrorLogFilterRequestRO filters = new ErrorLogFilterRequestRO();

        ErrorLogResultRO result = errorLogResource.getErrorLog(filters);

        Assertions.assertTrue(result.getErrorLogEntries().size() > 0);
    }

    @Test
    void testCsv() {
        ErrorLogFilterRequestRO filters = new ErrorLogFilterRequestRO();

        ResponseEntity<String> result = errorLogResource.getCsv(filters);
        String csv = result.getBody();

        Assertions.assertTrue(csv.contains(mockMessageId));
    }

    @Test
    void getErrorLog_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/errorlogs")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("orderBy", "timestamp")
                        .param("asc", "false")
                        .param("page", "0")
                        .param("pageSize", "10")

                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ErrorLogResultRO errorLogResultRO = objectMapper.readValue(content, ErrorLogResultRO.class);
        Assertions.assertNotNull(errorLogResultRO);
        Assertions.assertEquals(1, errorLogResultRO.getErrorLogEntries().size());
        Assertions.assertEquals(mockMessageId, errorLogResultRO.getErrorLogEntries().get(0).getMessageInErrorId());
        Assertions.assertEquals(MSHRole.SENDING, errorLogResultRO.getErrorLogEntries().get(0).getMshRole());
        Assertions.assertEquals(ErrorCode.EBMS_0004, errorLogResultRO.getErrorLogEntries().get(0).getErrorCode());
    }

    @Test
    void getCsv_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/errorlogs/csv")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("orderBy", "timestamp")
                        .param("asc", "false")
                        .param("page", "0")
                        .param("pageSize", "10")

                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        Assertions.assertNotNull(csv);

        List<List<String>> csvRecords = csvUtil.getCsvRecords(csv);
        Assertions.assertEquals(2, csvRecords.size());
        List<String> header = csvRecords.get(0);
        List<String> row = csvRecords.get(1);
        Assertions.assertEquals("AP Role", header.get(1));
        Assertions.assertEquals(MSHRole.SENDING.name(), row.get(1));
        Assertions.assertEquals("Message Id", header.get(2));
        Assertions.assertEquals(mockMessageId, row.get(2));
        Assertions.assertEquals("Error Code", header.get(3));
        Assertions.assertEquals("EBMS_0004", row.get(3));
    }

    private void createEntries() {
        ErrorLogEntry logEntry = new ErrorLogEntry();
        logEntry.setMessageInErrorId(mockMessageId);
        logEntry.setMshRole(mshRoleDao.findOrCreate(MSHRole.SENDING));
        logEntry.setErrorCode(ErrorCode.EBMS_0004);
        logEntry.setTimestamp(new Date());
        logEntry.setUserMessage(userMessageDao.findByEntityId(19700101L));
        errorLogDao.create(logEntry);
    }
}
