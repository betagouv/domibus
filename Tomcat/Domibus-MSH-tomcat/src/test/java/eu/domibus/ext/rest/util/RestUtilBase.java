package eu.domibus.ext.rest.util;

import org.apache.commons.io.IOUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Lucian Furca
 * @since 5.2
 *
 * Base utility class for the REST API IT tests
 */
public abstract class RestUtilBase {
    protected String pluginUser;
    protected String pluginPassword;

    protected MockMvc mockMvc;

    public RestUtilBase(String pluginUser, String pluginPassword, MockMvc mockMvc) {
        this.pluginUser = pluginUser;
        this.pluginPassword = pluginPassword;
        this.mockMvc = mockMvc;
    }

    protected static MockMultipartFile getMultiPartFile(String originalFilename, InputStream resourceAsStream) throws IOException {
        return new MockMultipartFile("file", originalFilename, "octetstream", IOUtils.toByteArray(resourceAsStream));
    }

    public abstract MvcResult addCertificateToStore(String addEndpoint) throws Exception;
    public abstract MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception;
}
