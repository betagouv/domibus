package eu.domibus.core.spring;

import ch.qos.logback.classic.LoggerContext;
import eu.domibus.core.plugin.classloader.PluginClassLoader;
import eu.domibus.logging.DomibusLogger;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

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

    @Mocked
    WebApplicationContext context;

    @BeforeEach
    public void setUp() throws MalformedURLException {
        pluginClassLoader = new MockedPluginClassLoader(new HashSet<>(), null);
        domibusContextLoaderListener = new DomibusContextLoaderListener(context, pluginClassLoader);
        // Since we mock the LoggerFactory, we have to mock the DomibusLogger
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void contextDestroyed_ok(@Mocked ServletContextEvent servletContextEvent,
                                    @Mocked ContextLoaderListener contextLoaderListener,
                                    @Mocked LoggerFactory loggerFactory,
                                    @Mocked LoggerContext loggerContext,
                                    @Mocked DomibusLogger domibusLogger) {
        ReflectionTestUtils.setField(domibusContextLoaderListener, "LOG", domibusLogger);

        new Expectations() {{

            LoggerFactory.getILoggerFactory();
            result = loggerContext;
        }};

        domibusContextLoaderListener.contextDestroyed(servletContextEvent);

        Assertions.assertTrue(pluginClassLoader.isCloseBeingCalled());
        new VerificationsInOrder() {{
            //super.contextDestroyed
            contextLoaderListener.contextDestroyed(servletContextEvent);
            times = 1;

            domibusLogger.info("Closing PluginClassLoader");
            times = 1;

            domibusLogger.info("Stop ch.qos.logback.classic.LoggerContext");
            times = 1;

            loggerContext.stop();
            times = 1;
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void contextDestroyed_pluginClassLoaderNull(@Mocked LoggerFactory loggerFactory,
                                                       @Mocked DomibusLogger domibusLogger) {
        ReflectionTestUtils.setField(domibusContextLoaderListener, "LOG", domibusLogger);

        ReflectionTestUtils.setField(domibusContextLoaderListener, "pluginClassLoader", null);

        domibusContextLoaderListener.shutdownPluginClassLoader();

        Assertions.assertFalse(pluginClassLoader.isCloseBeingCalled());
        new Verifications() {{
            domibusLogger.info("Closing PluginClassLoader");
            times = 0;
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void contextDestroyed_exception(@Mocked ServletContextEvent servletContextEvent,
                                           @Mocked ContextLoaderListener contextLoaderListener,
                                           @Mocked LoggerFactory loggerFactory,
                                           @Mocked LoggerContext loggerContext,
                                           @Mocked DomibusLogger domibusLogger) {
        ReflectionTestUtils.setField(domibusContextLoaderListener, "LOG", domibusLogger);

        pluginClassLoader.throwExceptionOnClose();

        new Expectations() {{

            LoggerFactory.getILoggerFactory();
            result = loggerContext;

        }};

        domibusContextLoaderListener.contextDestroyed(servletContextEvent);

        Assertions.assertTrue(pluginClassLoader.isCloseBeingCalled());
        new VerificationsInOrder() {{
            //super.contextDestroyed
            contextLoaderListener.contextDestroyed(servletContextEvent);
            times = 1;

            domibusLogger.info("Closing PluginClassLoader");
            times = 1;

            domibusLogger.warn(anyString, (Throwable) any);
            times = 1;

            domibusLogger.info("Stop ch.qos.logback.classic.LoggerContext");
            times = 1;

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
