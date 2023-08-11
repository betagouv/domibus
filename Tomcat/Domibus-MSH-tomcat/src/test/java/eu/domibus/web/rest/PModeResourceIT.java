package eu.domibus.web.rest;

import eu.domibus.AbstractIT;
import eu.domibus.web.rest.ro.PModeResponseRO;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MimeTypeUtils;

import javax.transaction.Transactional;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ionut Breaz
 * @since 5.1
 */

@Transactional
class PModeResourceIT extends AbstractIT {
    private MockMvc mockMvc;

    @Autowired
    PModeResource pmodeResource;

    @BeforeEach
    public void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders.standaloneSetup(pmodeResource)
                .build();
    }

    @Test
    void upload_and_download_OK() throws Exception {
        // Upload
        try(InputStream pmodeInputStream = new ClassPathResource("dataset/pmode/PModeTemplate.xml").getInputStream()) {
            byte[] pmodeByteArray = IOUtils.toByteArray(pmodeInputStream);
            MockMultipartFile multiPartFile = new MockMultipartFile("file", "pmode1.xml", MimeTypeUtils.APPLICATION_XML_VALUE, pmodeByteArray);
            mockMvc.perform(multipart("/rest/pmode")
                            .file(multiPartFile)
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf())
                            .param("description", "testPmode")
                    )
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();

            // Get current
            MvcResult getCurrentResult = mockMvc.perform(get("/rest/pmode/current")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf())
                    )
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();

            String getCurrentContent = getCurrentResult.getResponse().getContentAsString();
            PModeResponseRO pmodeResponseRO = objectMapper.readValue(getCurrentContent, PModeResponseRO.class);
            Assertions.assertTrue(pmodeResponseRO.isCurrent());
            Assertions.assertEquals("testPmode", pmodeResponseRO.getDescription());

            // Download
            MvcResult downloadResult = mockMvc.perform(get("/rest/pmode/" + pmodeResponseRO.getId())
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf())
                    )
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();

            String downloadedContent = downloadResult.getResponse().getContentAsString();
            String pmodeText = new String(pmodeByteArray, UTF_8);
            Assertions.assertEquals(pmodeText, downloadedContent);
        }
    }
}