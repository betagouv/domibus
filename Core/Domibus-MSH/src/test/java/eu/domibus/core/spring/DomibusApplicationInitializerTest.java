package eu.domibus.core.spring;

import eu.domibus.core.logging.LogbackLoggingConfigurator;
import eu.domibus.core.metrics.DomibusAdminServlet;
import eu.domibus.core.plugin.classloader.PluginClassLoader;
import eu.domibus.core.property.DomibusConfigLocationProvider;
import eu.domibus.core.property.DomibusPropertiesPropertySource;
import eu.domibus.core.property.DomibusPropertyConfiguration;
import eu.domibus.web.spring.DomibusWebConfiguration;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static eu.domibus.core.property.DomibusPropertiesPropertySource.UPDATED_PROPERTIES_NAME;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@SuppressWarnings({"TestMethodWithIncorrectSignature", "unused", "DataFlowIssue"})
@ExtendWith(JMockitExtension.class)
public class DomibusApplicationInitializerTest {

    @Tested
    DomibusApplicationInitializer domibusApplicationInitializer;

    @Test
    public void onStartup(@Injectable ServletContext servletContext,
                          @Mocked DomibusConfigLocationProvider domibusConfigLocationProvider,
                          @Mocked AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext,
                          @Mocked ServletRegistration.Dynamic dispatcher,
                          @Mocked BouncyCastleInitializer bouncyCastleInitializer,
                          @Mocked DispatcherServlet dispatcherServletBased,
                          @Mocked FilterRegistration.Dynamic springSecurityFilterChain,
                          @Mocked ServletRegistration.Dynamic cxfServlet) throws ServletException, IOException {
        String domibusConfigLocation = Paths.get("/home/domibus").normalize().toString();

        new Expectations(domibusApplicationInitializer) {{

            domibusConfigLocationProvider.getDomibusConfigLocation(servletContext);
            result = "/home/domibus";

            AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext1 = new AnnotationConfigWebApplicationContext();

            domibusApplicationInitializer.configureLogging(domibusConfigLocation);
            domibusApplicationInitializer.configureMetrics(servletContext);

            DispatcherServlet dispatcherServlet= new DispatcherServlet(annotationConfigWebApplicationContext1);

            servletContext.addServlet("dispatcher", dispatcherServlet);
            result = dispatcher;

            domibusApplicationInitializer.configurePropertySources((AnnotationConfigWebApplicationContext) any, (String) any);

            servletContext.addFilter("springSecurityFilterChain", DelegatingFilterProxy.class);
            result = springSecurityFilterChain;

            servletContext.addServlet("CXF", CXFServlet.class);

        }};

        domibusApplicationInitializer.onStartup(servletContext);

        new Verifications() {{
            bouncyCastleInitializer.registerBouncyCastle();
            bouncyCastleInitializer.checkStrengthJurisdictionPolicyLevel();

            annotationConfigWebApplicationContext.register(DomibusRootConfiguration.class, DomibusSessionConfiguration.class);
            annotationConfigWebApplicationContext.register(DomibusWebConfiguration.class);

            List<EventListener> list = new ArrayList<>();
            servletContext.addListener(withCapture(list));
            Assertions.assertEquals(2, list.size());
            assertThat(
                    list.stream().map(EventListener::getClass).collect(Collectors.toList()),
                    CoreMatchers.<Class<?>>hasItems(
                            DomibusContextLoaderListener.class,
                            RequestContextListener.class));

            dispatcher.setLoadOnStartup(1);
            dispatcher.addMapping("/");

            servletContext.setSessionTrackingModes(withAny(new HashSet<>()));
            springSecurityFilterChain.addMappingForUrlPatterns(null, false, "/*");

            cxfServlet.setLoadOnStartup(1);
            cxfServlet.addMapping("/services/*");
        }};
    }

    @Test
    public void onStartup_exception(@Injectable ServletContext servletContext,
                                    @Mocked DomibusConfigLocationProvider domibusConfigLocationProvider,
                                    @Mocked AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext) throws IOException {
        String domibusConfigLocation = Paths.get("/home/domibus").normalize().toString();

        new Expectations(domibusApplicationInitializer) {{
            domibusConfigLocationProvider.getDomibusConfigLocation(servletContext);
            result = domibusConfigLocation;

            domibusApplicationInitializer.configureLogging(Paths.get(domibusConfigLocation).normalize().toString());

            domibusApplicationInitializer.configurePropertySources((AnnotationConfigWebApplicationContext) any, (String) any);
            result = new IOException("ERROR");

        }};

        try {
            domibusApplicationInitializer.onStartup(servletContext);
            Assertions.fail();
        } catch (ServletException e) {
            assertThat(e.getCause(), CoreMatchers.instanceOf(IOException.class));
        }

        new Verifications() {{

            annotationConfigWebApplicationContext.register(DomibusRootConfiguration.class, DomibusSessionConfiguration.class);
            annotationConfigWebApplicationContext.setClassLoader((PluginClassLoader)any);
        }};
    }

    @Test
    public void configureMetrics(@Mocked ServletContext servletContext,
                                 @Injectable ServletRegistration.Dynamic servlet) {
        new Expectations() {{
            servletContext.addServlet("metrics", DomibusAdminServlet.class);
            result = servlet;
        }};

        domibusApplicationInitializer.configureMetrics(servletContext);

        new Verifications() {{
            List<EventListener> list = new ArrayList<>();
            servletContext.addListener(withCapture(list));
            Assertions.assertEquals(2, list.size());

            servlet.addMapping("/metrics/*");
            times = 1;
        }};
    }

    @Test
    public void createPluginClassLoader() throws IOException {
        String domibusConfigLocation = "/home/domibus";

        File pluginsLocation = new File(domibusConfigLocation + DomibusApplicationInitializer.PLUGINS_LOCATION);
        File extensionsLocation = new File(domibusConfigLocation + DomibusApplicationInitializer.EXTENSIONS_LOCATION);

        try (PluginClassLoader pluginClassLoader = domibusApplicationInitializer.createPluginClassLoader(domibusConfigLocation)) {

            Assertions.assertTrue(pluginClassLoader.getFiles().contains(pluginsLocation));
            Assertions.assertTrue(pluginClassLoader.getFiles().contains(extensionsLocation));
        }
    }

    @Test
    public void configureLogging(@Mocked LogbackLoggingConfigurator logbackLoggingConfigurator) {
        String domibusConfigLocation = "/home/domibus";

        domibusApplicationInitializer.configureLogging(domibusConfigLocation);

        new Verifications() {{
            new LogbackLoggingConfigurator(domibusConfigLocation);
            times = 1;

            logbackLoggingConfigurator.configureLogging();
            times = 1;
        }};
    }

    @Disabled
    @Test
    public void configurePropertySources(@Injectable AnnotationConfigWebApplicationContext rootContext,
                                         @Injectable ConfigurableEnvironment configurableEnvironment,
                                         @Injectable MutablePropertySources propertySources,
                                         @Injectable MapPropertySource domibusConfigLocationSource,
                                         @Injectable DomibusPropertiesPropertySource domibusPropertiesPropertySource,
                                         @Injectable DomibusPropertiesPropertySource updatedDomibusPropertiesPropertySource) throws IOException {
        String domibusConfigLocation = Paths.get("/home/domibus").normalize().toString();

        new Expectations(domibusApplicationInitializer) {{
            rootContext.getEnvironment();
            result = configurableEnvironment;

            configurableEnvironment.getPropertySources();
            result = propertySources;

            domibusApplicationInitializer.createDomibusConfigLocationSource(domibusConfigLocation);
            result = domibusConfigLocationSource;

            domibusApplicationInitializer.createUpdatedDomibusPropertiesSource();
            result = updatedDomibusPropertiesPropertySource;

            updatedDomibusPropertiesPropertySource.getName();
            result = UPDATED_PROPERTIES_NAME;

            domibusApplicationInitializer.createDomibusPropertiesPropertySource(domibusConfigLocation);
            result = domibusPropertiesPropertySource;
        }};

        domibusApplicationInitializer.configurePropertySources(rootContext, domibusConfigLocation);

        new VerificationsInOrder() {{
            propertySources.addFirst(updatedDomibusPropertiesPropertySource);
            times = 1;
            propertySources.addAfter(UPDATED_PROPERTIES_NAME, domibusConfigLocationSource);
            times = 1;
            propertySources.addLast(domibusPropertiesPropertySource);
            times = 1;
            propertySources.stream();
            times = 1;
        }};
    }

    @Test
    public void createDomibusPropertiesPropertySource(@Mocked DomibusPropertyConfiguration domibusPropertyConfiguration,
                                                      @Injectable PropertiesFactoryBean propertiesFactoryBean,
                                                      @Injectable DomibusPropertiesPropertySource domibusPropertiesPropertySource) throws IOException {
        String domibusConfigLocation = "/home/domibus";

        Properties properties = new Properties();
        new Expectations() {{
            domibusPropertyConfiguration.domibusProperties(domibusConfigLocation);
            result = propertiesFactoryBean;

            propertiesFactoryBean.getObject();
            result = properties;
        }};


        domibusApplicationInitializer.createDomibusPropertiesPropertySource(domibusConfigLocation);

        new Verifications() {{
            propertiesFactoryBean.setSingleton(false);

            new DomibusPropertiesPropertySource(DomibusPropertiesPropertySource.NAME, properties);
            times = 1;
        }};
    }

    @Test
    public void createDomibusConfigLocationSource() {
        String domibusConfigLocation = "/home/domibus";

        MapPropertySource domibusConfigLocationSource = domibusApplicationInitializer.createDomibusConfigLocationSource(domibusConfigLocation);
        Assertions.assertEquals("domibusConfigLocationSource", domibusConfigLocationSource.getName());
    }

    @Test
    public void createUpdatedDomibusPropertiesSource() {
        MapPropertySource propertySource = domibusApplicationInitializer.createUpdatedDomibusPropertiesSource();
        Assertions.assertEquals(UPDATED_PROPERTIES_NAME, propertySource.getName());
    }

    @Test
    public void testOrderOfPropertySources(@Injectable AnnotationConfigWebApplicationContext rootContext,
                                           @Injectable ConfigurableEnvironment configurableEnvironment,
                                           @Injectable MapPropertySource domibusConfigLocationSource,
                                           @Injectable DomibusPropertiesPropertySource domibusPropertiesPropertySource) throws IOException {
        String domibusConfigLocation = "/home/domibus";
        MutablePropertySources propertySources = new MutablePropertySources();
        DomibusPropertiesPropertySource updatedDomibusPropertiesPropertySource = new DomibusPropertiesPropertySource(UPDATED_PROPERTIES_NAME, new Properties());
        new Expectations(domibusApplicationInitializer) {{
            rootContext.getEnvironment();
            result = configurableEnvironment;

            configurableEnvironment.getPropertySources();
            result = propertySources;

            domibusApplicationInitializer.createDomibusConfigLocationSource(domibusConfigLocation);
            result = domibusConfigLocationSource;

            domibusApplicationInitializer.createDomibusPropertiesPropertySource(domibusConfigLocation);
            result = domibusPropertiesPropertySource;

            domibusApplicationInitializer.createUpdatedDomibusPropertiesSource();
            result = updatedDomibusPropertiesPropertySource;
        }};

        domibusApplicationInitializer.configurePropertySources(rootContext, domibusConfigLocation);

        Assertions.assertEquals(0, propertySources.precedenceOf(updatedDomibusPropertiesPropertySource));
        Assertions.assertEquals(1, propertySources.precedenceOf(domibusConfigLocationSource));
        Assertions.assertEquals(propertySources.size() - 1, propertySources.precedenceOf(domibusPropertiesPropertySource));
    }

}
