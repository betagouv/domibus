package eu.domibus.ext.rest;

import eu.domibus.AbstractIT;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Enumeration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author François Gautier
 * @since 5.0
 *
 * Pay attention with using gateway_truststore.jks because it will be overridden by 'uploadTruststore'
 */
@EnableMethodSecurity
public class TLSTrustStoreExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(TLSTrustStoreExtResourceIT.class);

    @Autowired
    private TLSTrustStoreExtResource tlsTrustStoreExtResource;

    public static final String TEST_ENDPOINT_RESOURCE = "/ext/tlstruststore";
    public static final String TEST_ENDPOINT_DOWNLOAD = TEST_ENDPOINT_RESOURCE + "/download";
    public static final String TEST_ENDPOINT_ADD = TEST_ENDPOINT_RESOURCE + "/entries";
    public static final String TEST_ENDPOINT_DELETE = TEST_ENDPOINT_RESOURCE + "/entries/{alias}";

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(tlsTrustStoreExtResource).build();
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void downloadTrustStore() throws Exception {
        uploadTrustStore("keystores/default.jks", "default.jks");

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
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void uploadTrustStore() throws Exception {
        uploadTrustStore("keystores/default.jks", "default.jks");

        MvcResult result;
        result = uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks");
        String content = result.getResponse().getContentAsString();
        Assert.assertEquals("TLS truststore file has been successfully replaced.", content);

    }

    private MvcResult uploadTrustStore(String name, String originalFilename) throws Exception {
        MvcResult result;
        KeyStore keystore;
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            keystore = KeyStore.getInstance("jks");
            keystore.load(resourceAsStream, "test123".toCharArray());
        }
        LOG.info("upload truststore with aliases [{}]", getAliases(keystore));

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            MockMultipartFile multiPartFile = getMultiPartFile(originalFilename, resourceAsStream);

            result = mockMvc.perform(multipart(TEST_ENDPOINT_RESOURCE)
                            .file(multiPartFile)
                            .param("password", "test123")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
        return result;
    }

    private static String getAliases(KeyStore keystore) throws KeyStoreException {
        StringBuilder stringBuilder = new StringBuilder();
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            stringBuilder.append(aliases.nextElement()).append(",");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void add() throws Exception {
        uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks");

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/red_gw.cer")) {
            MockMultipartFile multiPartFile = getMultiPartFile("red_gw.cer", resourceAsStream);

            MvcResult result = mockMvc.perform(multipart(TEST_ENDPOINT_ADD)
                            .file(multiPartFile)
                            .param("alias", "red_gw")
                            .param("password", "test123")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
            String content = result.getResponse().getContentAsString();
            Assert.assertEquals("Certificate [red_gw] has been successfully added to the TLS truststore.", content);
        }
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void delete() throws Exception {
        uploadTrustStore("keystores/default.jks", "default.jks");

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/default.jks")) {
            MockMultipartFile multiPartFile = getMultiPartFile("default.jks", resourceAsStream);

            MvcResult result = mockMvc.perform(multipart(HttpMethod.DELETE, TEST_ENDPOINT_DELETE, "blue_gw")
                            .file(multiPartFile)
                            .param("password", "test123")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
            String content = result.getResponse().getContentAsString();
            Assert.assertEquals("Certificate [blue_gw] has been successfully removed from the [TLS.truststore].", content);
        }
    }

}