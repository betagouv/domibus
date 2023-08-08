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
import eu.domibus.web.rest.ro.AlertResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AlertResourceDataIT  extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    AlertResource alertResource;

    @Autowired
    AlertDao alertDao;

    @Autowired
    EventDao eventDao;

    Alert newAlert;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(alertResource)
                .build();
        newAlert = createAlert();
    }

    @AfterEach
    void tearDown() {
        Set<Event> events = newAlert.getEvents();
        alertDao.delete(newAlert);
        eventDao.deleteAll(events);
    }

    @Test
    public void findAlerts_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        AlertResult alertResult = objectMapper.readValue(content, AlertResult.class);
        Assertions.assertNotNull(alertResult);
        Assertions.assertEquals(1, alertResult.getAlertsEntries().size());
        AlertRo alertRo = alertResult.getAlertsEntries().get(0);
        Assertions.assertEquals("LOW", alertRo.getAlertLevel());
        Assertions.assertEquals("SUCCESS", alertRo.getAlertStatus());
        Assertions.assertEquals("PLUGIN", alertRo.getAlertType());
        Assertions.assertEquals(5, alertRo.getMaxAttempts());
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
        Assertions.assertTrue(csv.contains("PLUGIN"));
    }

    //TODO IB !!!! processAlerts

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