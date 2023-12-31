package eu.domibus.ext.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.domibus.AbstractIT;
import eu.domibus.ext.domain.CacheEntryDTO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The complete rest endpoint integration tests
 */
@Transactional
@TestPropertySource(properties = {"domibus.deployment.clustered=true"})
@Disabled("EDELIVERY-6896")
public class DistributedCacheExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(DistributedCacheExtResourceIT.class);

    // The endpoints to test
    public static final String TEST_ENDPOINT_RESOURCE = "/ext/distributed-cache";
    public static final String TEST_ENDPOINT_CACHES = TEST_ENDPOINT_RESOURCE + "/caches";
    public static final String TEST_ENDPOINT_NAMES = TEST_ENDPOINT_CACHES + "/names";
    public static final String TEST_ENDPOINT_ENTRIES = TEST_ENDPOINT_RESOURCE + "/caches/{cacheName}";
    public static final String TEST_ENDPOINT_ENTRIES_EVICT = TEST_ENDPOINT_ENTRIES + "/{entryKey}";
    public static final String DOMIBUS_PROPERTY_METADATA = "domibusPropertyMetadata";
    public static final String DOMIBUS_DEPLOYMENT_CLUSTERED = "domibus.deployment.clustered";


    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testGetCachesName() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_NAMES)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<String> cachesName = objectMapper.readValue(content, new TypeReference<List<String>>() {
        });

        assertEquals(33, cachesName.size());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testGetEntries() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_ENTRIES, DOMIBUS_PROPERTY_METADATA)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // then
        String content = result.getResponse().getContentAsString();
        List<CacheEntryDTO> entries = objectMapper.readValue(content, new TypeReference<List<CacheEntryDTO>>() {});

        assertNotNull(entries);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getEntry() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_ENTRIES_EVICT, DOMIBUS_PROPERTY_METADATA, DOMIBUS_DEPLOYMENT_CLUSTERED)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
    }

    @Test
    public void createCache() throws Exception {
        DistributedCacheCreateRequestDto distributedCacheCreateRequestDto = new DistributedCacheCreateRequestDto();
        distributedCacheCreateRequestDto.setCacheName("newCache");
        distributedCacheCreateRequestDto.setCacheSize(1);
        distributedCacheCreateRequestDto.setTimeToLiveSeconds(1);
        distributedCacheCreateRequestDto.setMaxIdleSeconds(1);
        distributedCacheCreateRequestDto.setNearCacheSize(1);
        distributedCacheCreateRequestDto.setNearCacheTimeToLiveSeconds(1);
        distributedCacheCreateRequestDto.setNearCacheMaxIdleSeconds(1);

        // when
        mockMvc.perform(post(TEST_ENDPOINT_CACHES)
                        .content(asJsonString(distributedCacheCreateRequestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testEvictEntry() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_ENTRIES_EVICT, DOMIBUS_PROPERTY_METADATA, DOMIBUS_DEPLOYMENT_CLUSTERED)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    public void testEvictEntry_error_cacheNotFound() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_ENTRIES_EVICT, "cacheNotFound", DOMIBUS_DEPLOYMENT_CLUSTERED)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testEvictEntry_error_entryNotFound() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_ENTRIES_EVICT, DOMIBUS_PROPERTY_METADATA, "entryNotFound")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void addEntry() throws Exception {
        CacheEntryDTO cacheEntryDTO = new CacheEntryDTO();
        cacheEntryDTO.setKey("newKey");
        cacheEntryDTO.setValue("newValue");
        // when
        mockMvc.perform(post(TEST_ENDPOINT_ENTRIES, DOMIBUS_PROPERTY_METADATA)
                        .content(asJsonString(cacheEntryDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

}
