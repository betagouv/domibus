package eu.domibus.ext.rest.util;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Lucian Furca
 * @since 5.2
 *
 * Utility class for the IT tests of Legacy REST API
 */
public class LegacyRestUtil extends RestUtilBase {

    public LegacyRestUtil(String pluginUser, String pluginPassword, MockMvc mockMvc) {
        super(pluginUser, pluginPassword, mockMvc);
    }

    @Override
    public MvcResult addCertificateToStore(String addEndpoint) throws Exception {
        MvcResult result;
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/red_gw.cer")) {
            MockMultipartFile multiPartFile = getMultiPartFile("red_gw.cer", resourceAsStream);

            result = mockMvc.perform(multipart(addEndpoint)
                            .file(multiPartFile)
                            .param("alias", "red_gw")
                            .param("password", "test123")
                            .with(httpBasic(pluginUser, pluginPassword))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
        return result;
    }

    @Override
    public MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception {
        MvcResult result;
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/default.jks")) {
            MockMultipartFile multiPartFile = getMultiPartFile("default.jks", resourceAsStream);

            result = mockMvc.perform(multipart(HttpMethod.DELETE, deleteEndpoint, "blue_gw")
                            .file(multiPartFile)
                            .param("password", "test123")
                            .with(httpBasic(pluginUser, pluginPassword))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
        return result;
    }
}
