package eu.domibus.ext.rest.util;

import eu.domibus.ext.domain.CertificatePurposeDTO;
import eu.domibus.ext.domain.SecurityProfileDTO;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    protected MvcResult addCertificateToStore(String addEndpoint, final MultiValueMap<String, String> params) throws Exception {
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/red_gw.cer")) {
            MockMultipartFile multiPartFile = getMultiPartFile("red_gw.cer", resourceAsStream);
            return  mockMvc.perform(multipart(addEndpoint)
                            .file(multiPartFile)
                            .params(params)
                            .with(httpBasic(pluginUser, pluginPassword))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
    }

    protected MvcResult deleteCertificateFromStore(String deleteEndpoint, final MultiValueMap<String, String> params, String partyName) throws Exception {
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/default.jks")) {
            MockMultipartFile multiPartFile = getMultiPartFile("default.jks", resourceAsStream);
            return mockMvc.perform(multipart(HttpMethod.DELETE, deleteEndpoint, partyName)
                            .file(multiPartFile)
                            .params(params)
                            .with(httpBasic(pluginUser, pluginPassword))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
    }

    public abstract MvcResult addCertificateToStore(String addEndpoint) throws Exception;
    public abstract MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception;
}
