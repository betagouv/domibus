package eu.domibus.ext.rest;

import eu.domibus.AbstractIT;
import eu.domibus.ext.rest.util.RestUtilBase;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
 * @author Lucian Furca
 * @since 5.2
 *
 * Base class containing common functionality for the TrustStore and TLSTrustStore IT tests
 */
public abstract class TrustStoreExtResourceBaseIT extends AbstractIT {
    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(TrustStoreExtResourceBaseIT.class);

    protected MockMvc mockMvc;

    protected RestUtilBase restUtil;

    protected RestUtilBase legacyRestUtil;

    protected MvcResult downloadTrustStore(String downloadEndpoint) throws Exception {
        return mockMvc.perform(get(downloadEndpoint)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    protected MvcResult uploadTrustStore(String name, String originalFilename, String uploadEndpoint) throws Exception {
        MvcResult result;
        KeyStore keystore;
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            keystore = KeyStore.getInstance("jks");
            keystore.load(resourceAsStream, "test123".toCharArray());
        }
        LOG.info("upload truststore with aliases [{}]", getAliases(keystore));

        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(name)) {
            MockMultipartFile multiPartFile = getMultiPartFile(originalFilename, resourceAsStream);

            result = mockMvc.perform(multipart(uploadEndpoint)
                            .file(multiPartFile)
                            .param("password", "test123")
                            .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
        return result;
    }

    protected static String getAliases(KeyStore keystore) throws KeyStoreException {
        StringBuilder stringBuilder = new StringBuilder();
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            stringBuilder.append(aliases.nextElement()).append(",");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }
}
