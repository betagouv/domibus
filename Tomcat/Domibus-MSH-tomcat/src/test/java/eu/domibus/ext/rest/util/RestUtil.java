package eu.domibus.ext.rest.util;

import eu.domibus.ext.domain.CertificatePurposeDTO;
import eu.domibus.ext.domain.SecurityProfileDTO;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("partyName", "red_gw");
        params.add("securityProfile", SecurityProfileDTO.RSA.getProfile());
        params.add("certificatePurpose", CertificatePurposeDTO.DECRYPT.getCertificatePurpose());

        return addCertificateToStore(addEndpoint, params);
    }

    @Override
    public MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("securityProfile", SecurityProfileDTO.RSA.getProfile());
        params.add("certificatePurpose", CertificatePurposeDTO.DECRYPT.getCertificatePurpose());

        return deleteCertificateFromStore(deleteEndpoint, params, "red_gw");
    }
}
