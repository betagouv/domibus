package eu.domibus.core.plugin.classloader;

import com.google.common.collect.Sets;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by Cosmin Baciu on 6/15/2016.
 */
@ExtendWith(JMockitExtension.class)
public class PluginClassLoaderTest {

    @Injectable
    File pluginsDir;

    @Test
    @Disabled("EDELIVERY-6896")
    public void testDiscoverPlugins() throws Exception {
        final File plugin1JarFile = new File("c:/plugin1.jar");
        final File plugin2JarFile = new File("c:/plugin2.jar");
        new Expectations() {{
            pluginsDir.listFiles(withAny(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return false;
                }
            }));
            result = new File[]{plugin1JarFile, plugin2JarFile};
        }};


        PluginClassLoader pluginClassLoader = new PluginClassLoader(Sets.newHashSet(pluginsDir), PluginClassLoaderTest.class.getClassLoader());
        URL[] urls = pluginClassLoader.getURLs();
        assertNotNull(urls);
        assertEquals(urls.length, 2);

        List<URL> discoveredPluginsURL = Arrays.asList(urls);
        discoveredPluginsURL.contains(plugin1JarFile.toURI().toURL());
        discoveredPluginsURL.contains(plugin2JarFile.toURI().toURL());
    }
}
