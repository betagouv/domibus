package eu.domibus.ext.rest.util;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
        return addCertificateToStore(addEndpoint, true);
    }

    @Override
    public MvcResult deleteCertificateFromStore(String deleteEndpoint) throws Exception {
        return deleteCertificateFromStore(deleteEndpoint, true, "blue_gw");
    }
}
