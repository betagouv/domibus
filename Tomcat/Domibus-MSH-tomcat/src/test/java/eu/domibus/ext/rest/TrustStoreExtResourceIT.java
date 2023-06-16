package eu.domibus.ext.rest;

import eu.domibus.ext.rest.util.LegacyRestUtil;
import eu.domibus.ext.rest.util.RestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author François Gautier
 * @since 5.0
 *
 * Pay attention with using gateway_truststore.jks because it will be overridden by 'uploadTruststore'
 */
@EnableMethodSecurity
public class TrustStoreExtResourceIT extends TrustStoreExtResourceBaseIT {
    @Autowired
    private TruststoreExtResource truststoreExtResource;
    public static final String TEST_ENDPOINT_RESOURCE = "/ext/truststore";
    public static final String TEST_ENDPOINT_DOWNLOAD = TEST_ENDPOINT_RESOURCE + "/download";
    public static final String TEST_ENDPOINT_ADD = TEST_ENDPOINT_RESOURCE + "/entries";
    public static final String TEST_ENDPOINT_ADD_WITH_SECURITY_PROFILES = TEST_ENDPOINT_RESOURCE + "/certificates";
    public static final String TEST_ENDPOINT_DELETE = TEST_ENDPOINT_RESOURCE + "/entries/{alias}";
    public static final String TEST_ENDPOINT_DELETE_WITH_SECURITY_PROFILES = TEST_ENDPOINT_RESOURCE + "/certificates/{partyName:.+}";

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(truststoreExtResource).build();
        restUtil = new RestUtil(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD, mockMvc);
        legacyRestUtil = new LegacyRestUtil(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD, mockMvc);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void uploadTrustStore() throws Exception {
        //given
        uploadTrustStore("keystores/default.jks", "default.jks", TEST_ENDPOINT_RESOURCE);

        //when
        MvcResult result = uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks", TEST_ENDPOINT_RESOURCE);

        //then
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Truststore file has been successfully replaced.", content);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void downloadTrustStore() throws Exception {
        //given
        uploadTrustStore("keystores/default.jks", "default.jks", TEST_ENDPOINT_RESOURCE);

        //when
        MvcResult result = downloadTrustStore(TEST_ENDPOINT_DOWNLOAD);

        // then
        String content = result.getResponse().getContentAsString();
        Assertions.assertNotNull(content);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void add() throws Exception {
        //given
        uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks", TEST_ENDPOINT_RESOURCE);

        //when
        MvcResult result = legacyRestUtil.addCertificateToStore(TEST_ENDPOINT_ADD);

        //then
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [red_gw] has been successfully added to the truststore.", content);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void addWithSecurityProfiles() throws Exception {
        //given
        uploadTrustStore("keystores/gateway_truststore2.jks", "gateway_truststore2.jks", TEST_ENDPOINT_RESOURCE);

        //when
        MvcResult result = restUtil.addCertificateToStore(TEST_ENDPOINT_ADD_WITH_SECURITY_PROFILES);

        //then
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [red_gw_rsa_decrypt] has been successfully added to the truststore.", content);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void delete() throws Exception {
        //given
        uploadTrustStore("keystores/default.jks", "default.jks", TEST_ENDPOINT_RESOURCE);

        //when
        MvcResult result = legacyRestUtil.deleteCertificateFromStore(TEST_ENDPOINT_DELETE);

        //then
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [blue_gw] has been successfully removed from the truststore.", content);
    }

    @Test
    @Transactional
    @WithMockUser(username = TEST_PLUGIN_USERNAME, roles = {"ADMIN"})
    public void deleteWithSecurityProfiles() throws Exception {
        //given
        uploadTrustStore("keystores/default.jks", "default.jks", TEST_ENDPOINT_RESOURCE);
        restUtil.addCertificateToStore(TEST_ENDPOINT_ADD_WITH_SECURITY_PROFILES);

        //when
        MvcResult result = restUtil.deleteCertificateFromStore(TEST_ENDPOINT_DELETE_WITH_SECURITY_PROFILES);

        //then
        String content = result.getResponse().getContentAsString();
        Assertions.assertEquals("Certificate [red_gw_rsa_decrypt] has been successfully removed from the truststore.", content);
    }
}
