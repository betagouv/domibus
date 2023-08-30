package eu.domibus.web.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.domibus.AbstractIT;
import eu.domibus.common.CsvUtil;
import eu.domibus.web.rest.ro.UserResponseRO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Ionut Breaz
 * @since 5.1
 */

@Transactional
class UserResourceIT extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    UserResource userResource;

    @Autowired
    CsvUtil csvUtil;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userResource)
                .build();
    }

    @Test
    void testUsersList_Add_Update_Delete_OK() throws Exception {
        // List users
        mockMvc.perform(get("/rest/user/users")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                // since users are not returned in order we need to match them differently
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$.[?(@.userName == \"admin\" && @.roles == \"ROLE_ADMIN\" && @.domain == \"default\")]").exists())
                .andExpect(jsonPath("$.[?(@.userName == \"user\" && @.roles == \"ROLE_USER\" && @.domain == \"default\")]").exists());


        // Add, Update, Delete
        List<UserResponseRO> usersList = Arrays.asList(
                getUserResponseRO("admin", "ROLE_ADMIN", "UPDATED"),
                getUserResponseRO("user", "ROLE_USER", "REMOVED"),
                getUserResponseRO("user2", "ROLE_USER", "NEW"));
        String usersListJson = objectMapper.writeValueAsString(usersList);

        mockMvc.perform(put("/rest/user/users")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())
                        .content(usersListJson)
                )
                .andExpect(status().is2xxSuccessful());


        // List updated users
        mockMvc.perform(get("/rest/user/users")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$.[?(@.userName == \"admin\" && @.roles == \"ROLE_ADMIN\" && @.domain == \"default\" && @.deleted == false)]").exists())
                .andExpect(jsonPath("$.[?(@.userName == \"user\" && @.roles == \"ROLE_USER\" && @.domain == \"default\" && @.deleted == true)]").exists())
                .andExpect(jsonPath("$.[?(@.userName == \"user2\" && @.roles == \"ROLE_USER\" && @.domain == \"default\" && @.deleted == false)]").exists())
                .andReturn();
    }

    @Test
    void testUserRoles_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/user/userroles")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        List<String> list = objectMapper.readValue(content, new TypeReference<List<String>>(){});
        assertThat(list, containsInAnyOrder("ROLE_USER", "ROLE_ADMIN"));
    }

    @Test
    void getCsv_OK() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/user/csv")
                        .contentType("text/html; charset=UTF-8")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf())
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        String csv = result.getResponse().getContentAsString();
        Assertions.assertNotNull(csv);

        List<List<String>> csvRecords = csvUtil.getCsvRecords(csv);
        Assertions.assertEquals(3, csvRecords.size());
        List<String> header = csvRecords.get(0);

        List<List<String>> rows = Arrays.asList(csvRecords.get(1), csvRecords.get(2));
        rows.sort(Comparator.comparing(row -> row.get(0)));
        List<String> adminRow = rows.get(0);
        List<String> userRow = rows.get(1);
        Assertions.assertEquals("User Name", header.get(0));
        Assertions.assertEquals("admin", adminRow.get(0));
        Assertions.assertEquals("user", userRow.get(0));
        Assertions.assertEquals("Role", header.get(1));
        Assertions.assertEquals("ROLE_ADMIN", adminRow.get(1));
        Assertions.assertEquals("ROLE_USER", userRow.get(1));
        Assertions.assertEquals("Active", header.get(3));
        Assertions.assertEquals("true", adminRow.get(3));
        Assertions.assertEquals("true", userRow.get(3));
        Assertions.assertEquals("Domain", header.get(4));
        Assertions.assertEquals("blue domain", adminRow.get(4));
        Assertions.assertEquals("blue domain", userRow.get(4));
    }

    private UserResponseRO getUserResponseRO(String userName, String role, String status) {
        UserResponseRO userResponseRO = new UserResponseRO();
        userResponseRO.setUserName(userName);
        userResponseRO.setEmail("email@domain.com");
        userResponseRO.setPassword("123abc%_ABC");
        userResponseRO.setActive(true);
        userResponseRO.setDomain("default");
        userResponseRO.setAuthorities(Collections.singletonList(role));
        userResponseRO.updateRolesField();
        userResponseRO.setStatus(status);
        return userResponseRO;
    }
}