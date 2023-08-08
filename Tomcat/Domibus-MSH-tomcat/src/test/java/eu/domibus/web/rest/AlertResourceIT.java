package eu.domibus.web.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.domibus.AbstractIT;
import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.core.alerts.dao.AlertDao;
import eu.domibus.core.alerts.model.common.AlertStatus;
import eu.domibus.core.alerts.model.common.AlertType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static eu.domibus.web.rest.AlertResource.forbiddenAlertTypesExtAuthProvider;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ionut Breaz
 * @since 5.1
 */

public class AlertResourceIT  extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    AlertResource alertResource;

    @Autowired
    AlertDao alertDao;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(alertResource)
                .build();
    }

    @Test
    public void getAlertTypes_OK() throws Exception {
        checkListResult("/rest/alerts/types", AlertType.values().length - forbiddenAlertTypesExtAuthProvider.size());
    }

    @Test
    public void getAlertLevels_OK() throws Exception {
        checkListResult("/rest/alerts/levels", AlertLevel.values().length);
    }

    @Test
    public void getAlertStatus_OK() throws Exception {
        checkListResult("/rest/alerts/status", AlertStatus.values().length);
    }

    @Test
    public void getAlertParameters_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts/params")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                        .param("alertType", "PASSWORD_EXPIRED")
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        List<String> list = objectMapper.readValue(content, new TypeReference<List<String>>(){});
        Assertions.assertEquals(3, list.size());
    }

    private void checkListResult(String path, int expectedLength) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        List<String> list = objectMapper.readValue(content, new TypeReference<List<String>>(){});
        Assertions.assertEquals(expectedLength, list.size());
    }
}
