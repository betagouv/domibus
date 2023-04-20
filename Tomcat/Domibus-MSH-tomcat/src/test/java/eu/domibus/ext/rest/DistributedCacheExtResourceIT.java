package eu.domibus.ext.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.domibus.AbstractIT;
import eu.domibus.ext.domain.CacheEntryDTO;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The complete rest endpoint integration tests
 */
@Transactional
public class DistributedCacheExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(DistributedCacheExtResourceIT.class);

    // The endpoints to test
    public static final String TEST_ENDPOINT_RESOURCE = "/ext/distributed-cache";
    public static final String TEST_ENDPOINT_CACHES = TEST_ENDPOINT_RESOURCE + "/caches";
    public static final String TEST_ENDPOINT_NAMES = TEST_ENDPOINT_CACHES + "/names";
    public static final String TEST_ENDPOINT_ENTRIES = TEST_ENDPOINT_RESOURCE + "/caches/{cacheName}";
    public static final String TEST_ENDPOINT_ENTRIES_EVICT = TEST_ENDPOINT_ENTRIES + "/{entryKey}";


    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webAppContext;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
    }

    @Test
    @Transactional
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
    @Transactional
    public void testGetEntries() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_ENTRIES, "domibusPropertyMetadata")
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
    @Transactional
    public void getEntry() throws Exception {
        // when
        MvcResult result = mockMvc.perform(get(TEST_ENDPOINT_ENTRIES_EVICT, "domibusPropertyMetadata", "domibus.deployment.clustered")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
    }

    @Test
    @Transactional
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
    @Transactional
    public void testEvictEntry() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_ENTRIES_EVICT, "domibusPropertyMetadata", "domibus.deployment.clustered")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @Transactional
    public void testEvictEntry_error_cacheNotFound() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_ENTRIES_EVICT, "cacheNotFound", "domibus.deployment.clustered")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @Transactional
    public void testEvictEntry_error_entryNotFound() throws Exception {
        // when
        mockMvc.perform(delete(TEST_ENDPOINT_ENTRIES_EVICT, "domibusPropertyMetadata", "entryNotFound")
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @Transactional
    public void addEntry() throws Exception {
        CacheEntryDTO cacheEntryDTO = new CacheEntryDTO();
        cacheEntryDTO.setKey("newKey");
        cacheEntryDTO.setValue("newValue");
        // when
        mockMvc.perform(post(TEST_ENDPOINT_ENTRIES, "domibusPropertyMetadata")
                        .content(asJsonString(cacheEntryDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(httpBasic(TEST_PLUGIN_USERNAME, TEST_PLUGIN_PASSWORD))
                        .with(csrf()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

}
