package eu.domibus.core.spring;

import ch.qos.logback.classic.LoggerContext;
import eu.domibus.core.plugin.classloader.PluginClassLoader;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author François Gautier
 * @since 4.2
 * <p>
 * Methods
 * {@link DomibusContextLoaderListener#shutdownPluginClassLoader}
 * {@link DomibusContextLoaderListener#shutdownLogger}
 * are NOT tested separately because it's hard to partially mocked local method
 * AND super method {@link ContextLoaderListener#contextDestroyed}
 */
@ExtendWith(JMockitExtension.class)
public class DomibusContextLoaderListenerTest {

    DomibusContextLoaderListener domibusContextLoaderListener;

    MockedPluginClassLoader pluginClassLoader;

    @Injectable
    WebApplicationContext context;

    @Injectable
    LoggerContext loggerContext;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        pluginClassLoader = new MockedPluginClassLoader(new HashSet<>(), null);
        domibusContextLoaderListener = new DomibusContextLoaderListener(context, pluginClassLoader);
        // Since we mock the LoggerFactory, we have to mock the DomibusLogger
        new MockUp<LoggerFactory>() {
            @Mock
            ILoggerFactory getILoggerFactory() {
                return loggerContext;
            }
        };
    }

    @Test
    public void contextDestroyed_ok(@Injectable ServletContextEvent servletContextEvent,
                                    @Injectable ContextLoaderListener contextLoaderListener,
                                    @Injectable ServletContext servletContext) {

        new Expectations() {{
            servletContextEvent.getServletContext();
            result = servletContext;
        }};

        domibusContextLoaderListener.contextDestroyed(servletContextEvent);

        Assertions.assertTrue(pluginClassLoader.isCloseBeingCalled());
        new Verifications() {{
            loggerContext.stop();
            times = 1;
        }};
    }

    @Test
    public void contextDestroyed_pluginClassLoaderNull() throws IOException {
        ReflectionTestUtils.setField(domibusContextLoaderListener, "pluginClassLoader", null);

        domibusContextLoaderListener.shutdownPluginClassLoader();

        Assertions.assertFalse(pluginClassLoader.isCloseBeingCalled());
        new Verifications() {{
            pluginClassLoader.close();
            times = 1;
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896 fails when run test class")
    public void contextDestroyed_exception(@Injectable ServletContextEvent servletContextEvent,
                                           @Injectable ContextLoaderListener contextLoaderListener,
                                           @Injectable ServletContext servletContext) {
        pluginClassLoader.throwExceptionOnClose();

        new Expectations() {{
            servletContextEvent.getServletContext();
            result = servletContext;
        }};

        domibusContextLoaderListener.contextDestroyed(servletContextEvent);

        Assertions.assertTrue(pluginClassLoader.isCloseBeingCalled());
        new Verifications() {{
            loggerContext.stop();
            times = 1;
        }};
    }

    /**
     * As JMockit cannot mock the class {@link ClassLoader}
     * see https://github.com/jmockit/jmockit1/blob/master/main/test/mockit/JREMockingTest.java
     * for unmockable classes from the JRE
     */
    static class MockedPluginClassLoader extends PluginClassLoader {
        boolean closeBeingCalled = false;
        boolean throwExceptionOnClose = false;

        public MockedPluginClassLoader(Set<File> files, ClassLoader parent) throws MalformedURLException {
            super(files, parent);
        }

        /**
         * And that, my little children, is how you are doing mocking back in the days
         */
        public void close() throws IOException {
            closeBeingCalled = true;
            if (throwExceptionOnClose) {
                throw new IOException("Test");
            }
        }

        public boolean isCloseBeingCalled() {
            return closeBeingCalled;
        }

        public void throwExceptionOnClose() {
            throwExceptionOnClose = true;
        }
    }
}
