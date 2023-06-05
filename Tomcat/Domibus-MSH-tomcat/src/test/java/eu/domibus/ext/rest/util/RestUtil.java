package eu.domibus.ext.rest.util;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        return addCertificateToStore(addEndpoint, false);
    }

    @Override
    public MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception {
        return deleteCertificateFromStore(deleteEndpoint, false, "red_gw");
    }
}
