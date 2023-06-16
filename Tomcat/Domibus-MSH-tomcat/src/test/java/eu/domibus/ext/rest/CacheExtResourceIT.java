package eu.domibus.ext.rest;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MpcEntity;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.core.message.dictionary.MpcDao;
import eu.domibus.ext.services.CacheExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.hamcrest.CoreMatchers;
import org.hibernate.SessionFactory;

import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * @author François Gautier
 * @since 5.0
 */
@EnableMethodSecurity
public class CacheExtResourceIT extends AbstractIT {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusEArchiveExtResourceIT.class);
    public static final String NOT_FOUND = "not_found";


    @Autowired
    private CacheExtResource cacheExtResource;

    @Autowired
    private CacheExtService cacheExtService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private DomainService domainService;
    @Autowired
    private MpcDao mpcDao;
    @Autowired
    protected LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean;

    private MockMvc mockMvc;
    private org.hibernate.Cache secondLevelCache;
    private MpcEntity dummyMpc;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cacheExtResource).build();

        secondLevelCache = localContainerEntityManagerFactoryBean
                .getNativeEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getCache();

        //set one value in cache
        domainService.getDomain(NOT_FOUND);
        //check value is present in cache
        checkStillInCache();

        //set one value in 2L cache
        dummyMpc = mpcDao.findOrCreateMpc("DUMMY_MPC");
    }

    protected void setAuth() {
    }

    @AfterEach
    public void tearDown() throws Exception {
        cacheExtService.evict2LCaches();
        cacheExtService.evictCaches();
    }

    private boolean specificMpc2LIsCached(long mpc) {
        return secondLevelCache.contains(MpcEntity.class, mpc);
    }

    private Cache.ValueWrapper getSpecificDomainCached(String domainName) {
        Cache domainByCode = cacheManager.getCache("domainByCode");
        if (domainByCode == null) {
            return null;
        }
        return domainByCode.get(domainName);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void deleteCache_noUser() throws Exception {

        Assertions.assertThrows(AuthenticationCredentialsNotFoundException.class, () -> mockMvc.perform(delete("/ext/cache")));

        checkStillInCache();
    }

    private void checkStillInCache() {
        Cache.ValueWrapper wrapper = getSpecificDomainCached(NOT_FOUND);
        Assertions.assertNotNull(wrapper);
    }

    private void checkNothingInCache() {
        Cache.ValueWrapper domainCached = getSpecificDomainCached(NOT_FOUND);
        Assertions.assertNull(domainCached);
    }

    @Test
    @WithMockUser
    @Disabled("EDELIVERY-6896")
    public void deleteCache_notAdmin() throws Exception {

        Assertions.assertThrows(AccessDeniedException.class, () -> mockMvc.perform(delete("/ext/cache")));

        checkStillInCache();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteCache_admin() throws Exception {
        mockMvc.perform(delete("/ext/cache"));

        checkNothingInCache();
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void delete2LCache_noUser() throws Exception {

        Assertions.assertThrows(AuthenticationCredentialsNotFoundException.class, () -> mockMvc.perform(delete("/ext/2LCache")));

        checkStillIn2LCache();
    }

    private void checkStillIn2LCache() {
        boolean isCached = specificMpc2LIsCached(dummyMpc.getEntityId());
        Assertions.assertTrue(isCached);
    }

    private void checkNothingIn2LCache() {
        Assertions.assertFalse(specificMpc2LIsCached(dummyMpc.getEntityId()));
    }

    @Test
    @WithMockUser
    @Disabled("EDELIVERY-6896")
    public void delete2LCache_notAdmin() throws Exception {
        Assertions.assertThrows(AccessDeniedException.class, () -> mockMvc.perform(delete("/ext/2LCache")));

        checkStillIn2LCache();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void delete2LCache_admin() throws Exception {
        mockMvc.perform(delete("/ext/2LCache"));

        checkNothingIn2LCache();
    }

}
