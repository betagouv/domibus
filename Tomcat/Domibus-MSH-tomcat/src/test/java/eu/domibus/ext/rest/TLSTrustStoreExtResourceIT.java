package eu.domibus.ext.rest;

import eu.domibus.AbstractIT;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author François Gautier
 * @since 5.0
 */
@EnableMethodSecurity
public class TLSTrustStoreExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(TLSTrustStoreExtResourceIT.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private TLSTrustStoreExtResource tlsTrustStoreExtResource;

    public static final String TEST_ENDPOINT_RESOURCE = "/ext/tlstruststore";
    public static final String TEST_ENDPOINT_DOWNLOAD = TEST_ENDPOINT_RESOURCE + "/download";

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tlsTrustStoreExtResource).build();
    }

    @Test
    @Transactional
    @Ignore
    public void downloadTrustStore() throws Exception {

        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_DOWNLOAD)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        Assert.assertNotNull(content);
    }

    @Test
    @Transactional
    @Ignore
    public void uploadTrustStore() throws Exception {

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/gateway_truststore.jks")) {
            MockMultipartFile multiPartFile = new MockMultipartFile("gateway_truststore.jks", "gateway_truststore.jks",
                    "octetstream", IOUtils.toByteArray(resourceAsStream));

            MvcResult result = mockMvc.perform(multipart(TEST_ENDPOINT_RESOURCE)
                            .file(multiPartFile)
                            .param("password", "test123")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
            String content = result.getResponse().getContentAsString();
            Assert.assertEquals("TLS truststore file has been successfully replaced.", content);
        }
    }

}