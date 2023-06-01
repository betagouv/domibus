package eu.domibus.ext.rest.util;

import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
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
 *  Utility class for the REST API IT tests
 */
public class RestUtil extends RestUtilBase {

    public RestUtil(String pluginUser, String pluginPassword, MockMvc mockMvc) {
        super(pluginUser, pluginPassword, mockMvc);
    }

    @Override
    public MvcResult addCertificateToStore(String addEndpoint) throws Exception {
        MvcResult result;
        try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("keystores/red_gw.cer")) {
            MockMultipartFile multiPartFile = getMultiPartFile("red_gw.cer", resourceAsStream);

            result = mockMvc.perform(multipart(addEndpoint)
                            .file(multiPartFile)
                            .param("partyName", "red_gw")
                            .param("securityProfile", SecurityProfile.RSA.getProfile())
                            .param("certificatePurpose", CertificatePurpose.DECRYPT.getCertificatePurpose())
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

            result = mockMvc.perform(multipart(HttpMethod.DELETE, deleteEndpoint, "red_gw")
                            .file(multiPartFile)
                            .param("securityProfile", SecurityProfile.RSA.getProfile())
                            .param("certificatePurpose", CertificatePurpose.DECRYPT.getCertificatePurpose())
                            .with(httpBasic(pluginUser, pluginPassword))
                            .with(csrf()))
                    .andExpect(status().is2xxSuccessful())
                    .andReturn();
        }
        return result;
    }
}
