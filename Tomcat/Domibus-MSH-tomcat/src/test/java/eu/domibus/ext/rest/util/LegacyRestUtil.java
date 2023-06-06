package eu.domibus.ext.rest.util;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("alias", "red_gw");
        params.add("password", "test123");

        return addCertificateToStore(addEndpoint, params);
    }

    @Override
    public MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("password", "test123");

        return deleteCertificateFromStore(deleteEndpoint, params, "blue_gw");
    }
}
