package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.core.alerts.dao.AlertDao;
import eu.domibus.core.alerts.dao.EventDao;
import eu.domibus.core.alerts.model.common.AlertStatus;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.persist.Alert;
import eu.domibus.core.alerts.model.persist.Event;
import eu.domibus.core.alerts.model.web.AlertRo;
import eu.domibus.common.CsvUtil;
import eu.domibus.web.rest.ro.AlertResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.transaction.Transactional;
import java.util.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ionut Breaz
 * @since 5.1
 */

@Transactional
class AlertResourceDataIT  extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    AlertResource alertResource;

    @Autowired
    AlertDao alertDao;

    @Autowired
    EventDao eventDao;

    @Autowired
    CsvUtil csvUtil;

    Alert newAlert;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(alertResource)
                .build();
        newAlert = createAlert();
    }

    @Test
    void findAlerts_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        AlertResult alertResult = objectMapper.readValue(content, AlertResult.class);
        Assertions.assertNotNull(alertResult);
        Assertions.assertFalse(alertResult.getAlertsEntries().isEmpty());
    }

    @Test
    void getCsv_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts/csv")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("orderBy", "creationTime")
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
        Assertions.assertTrue(csvRecords.size() >= 2);
        List<String> header = csvRecords.get(0);
        Assertions.assertEquals("Alert Type", header.get(2));
        Assertions.assertEquals("Alert Level", header.get(3));
        Assertions.assertEquals("Alert Status", header.get(4));
    }

    private Alert createAlert() {
        Event event = new Event();
        event.setType(EventType.PLUGIN);
        event.setReportingTime(new Date());

        Alert alert = new Alert();
        alert.setAlertLevel(AlertLevel.LOW);
        alert.setAlertStatus(AlertStatus.SUCCESS);
        alert.setAlertType(AlertType.PLUGIN);
        alert.setReportingTime(new Date());
        alert.setMaxAttempts(5);
        alert.setEvents(new HashSet<>(Collections.singletonList(event)));
        alertDao.create(alert);
        return alert;
    }
}