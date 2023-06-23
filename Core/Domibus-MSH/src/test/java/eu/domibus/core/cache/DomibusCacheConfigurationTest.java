package eu.domibus.core.cache;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @author Catalin Enache
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class DomibusCacheConfigurationTest {

    @Tested
    DomibusCacheConfiguration domibusCacheConfiguration;


    @Test
    @Disabled("EDELIVERY-6896")
    public void test_cacheManagerExternalFilePresent() throws  Exception {
        prepareTestEhCacheFiles();
        new Expectations(domibusCacheConfiguration) {{
            domibusCacheConfiguration.externalCacheFileExists();
            result = true;

        }};

        //tested method
        org.springframework.cache.CacheManager cacheManager = domibusCacheConfiguration.cacheManager();

        Assertions.assertNotNull(cacheManager.getCache("policyCacheDefault"));
        Assertions.assertNotNull(cacheManager.getCache("policyCacheExternal"));

    }

    protected void prepareTestEhCacheFiles() {
        ReflectionTestUtils.setField(domibusCacheConfiguration, "defaultEhCacheFile", "config/ehcache/ehcache-default-test.xml");
        ReflectionTestUtils.setField(domibusCacheConfiguration, "externalEhCacheFile", "target/test-classes/conf/domibus/internal/ehcache-test.xml");
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void test_cacheManagerNoExternalFilePresent() throws  Exception {
        prepareTestEhCacheFiles();
        new Expectations(domibusCacheConfiguration) {{
            domibusCacheConfiguration.externalCacheFileExists();
            result = false;
        }};

        //tested method
        org.springframework.cache.CacheManager cacheManager = domibusCacheConfiguration.cacheManager();

        Assertions.assertNotNull(cacheManager.getCache("policyCacheDefault"));
        Assertions.assertNull(cacheManager.getCache("policyCacheExternal"));
    }


    @Test
    @Disabled("EDELIVERY-6896")
    public void test_mergeExternalCacheConfiguration(@Mocked javax.cache.CacheManager defaultCacheManager,
                                                     @Mocked javax.cache.CacheManager externalCacheManager,
                                                     @Mocked CachingProvider cachingProvider) {

        domibusCacheConfiguration.mergeExternalCacheConfiguration(cachingProvider, defaultCacheManager);

        new FullVerifications(domibusCacheConfiguration) {{
            cachingProvider.getCacheManager((URI) any, (ClassLoader) any);

            domibusCacheConfiguration.overridesDefaultCache(defaultCacheManager, externalCacheManager);
        }};
    }

    @Test
    public void test_overridesDefaultCache(@Mocked javax.cache.CacheManager defaultCacheManager,
                                           @Mocked javax.cache.CacheManager externalCacheManager) {
        new Expectations(domibusCacheConfiguration) {{
            externalCacheManager.getCacheNames();
            result = new String[]{"cache1", "cache2"};

            domibusCacheConfiguration.cacheExists(defaultCacheManager, "cache1");;
            result = true;
        }};

        domibusCacheConfiguration.overridesDefaultCache(defaultCacheManager, externalCacheManager);

        new Verifications() {{
            defaultCacheManager.destroyCache("cache1");

            List<String> cacheNamesActual =  new ArrayList<>();
            defaultCacheManager.createCache(withCapture(cacheNamesActual), (Configuration)any);
            Assertions.assertTrue(cacheNamesActual.size() == 2);
        }};
    }

    @Test
    public void test_addPluginsCacheConfiguration(@Mocked javax.cache.CacheManager cacheManager,
                                                  @Mocked javax.cache.CacheManager cacheManagerPlugins,
                                                  @Mocked Resource pluginDefaultFile,
                                                  @Mocked Resource pluginFile,
                                                  @Mocked CachingProvider cachingProvider) {
        final String pluginsConfigLocation = "/data/tomcat/domibus/conf/plugins/config";

        new Expectations(domibusCacheConfiguration) {{
            domibusCacheConfiguration.readPluginEhcacheFiles(anyString);
            result = Collections.singletonList(pluginDefaultFile);

            domibusCacheConfiguration.readPluginEhcacheFiles(anyString);
            result = Collections.singletonList(pluginFile);

            cachingProvider.getCacheManager();
            result = cacheManagerPlugins;

            cacheManagerPlugins.getCacheNames();
            result = new String[]{"cache1", "cache2", "cache3"};

        }};

        domibusCacheConfiguration.addPluginsCacheConfiguration(cachingProvider, cacheManager, pluginsConfigLocation);

        new FullVerifications(domibusCacheConfiguration) {{
            List<Resource> resourceParams = new ArrayList<>();
            domibusCacheConfiguration.readPluginCacheConfig(cachingProvider, cacheManagerPlugins, withCapture(resourceParams));
            Assertions.assertTrue(resourceParams.size() == 2);

            List<String> cacheNamesActual =  new ArrayList<>();
            cacheManager.createCache(withCapture(cacheNamesActual), (Configuration)any);
            Assertions.assertTrue(cacheNamesActual.size() == 3);
        }};
    }

}
