package eu.domibus.core.logging;

import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.core.property.PropertyUtils;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Created by Cosmin Baciu on 12-Oct-16.
 */
@ExtendWith(JMockitExtension.class)
public class LogbackLoggingConfiguratorTest {

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    String domibusConfigLocation = File.separator + "user";

    @Tested
    LogbackLoggingConfigurator logbackLoggingConfigurator;


    @Test
    @Disabled("EDELIVERY-6896")
    public void testConfigureLoggingWithCustomFile() {
        new Expectations(logbackLoggingConfigurator) {{
            logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();
            result = "/user/logback-test.xml";

            System.getProperty(anyString);
            result = "/user/mylogback.xml";
        }};

        logbackLoggingConfigurator.configureLogging();

        new Verifications() {{
            String fileLocation = null;
            logbackLoggingConfigurator.configureLogging(fileLocation = withCapture());
            times = 1;

            Assertions.assertEquals("/user/mylogback.xml", fileLocation);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testConfigureLoggingWithTheDefaultLogbackConfigurationFile() {
        new Expectations(logbackLoggingConfigurator) {{
            logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();
            result = "/user/logback-test.xml";

            System.getProperty(anyString);
            result = null;
        }};

        logbackLoggingConfigurator.configureLogging();

        new Verifications() {{
            String fileLocation = null;
            logbackLoggingConfigurator.configureLogging(fileLocation = withCapture());
            times = 1;

            Assertions.assertEquals("/user/logback-test.xml", fileLocation);
        }};
    }

    @Test
    public void testConfigureLoggingWithEmptyConfigLocation(final @Capturing Logger log) throws Exception {
        logbackLoggingConfigurator.configureLogging(null);

        new Verifications() {{
            log.warn(anyString);
            times = 1;
        }};
    }

    @Test
    public void testConfigureLoggingWithMissingLogFile(@Mocked File file) throws Exception {
        new Expectations(logbackLoggingConfigurator) {{
            new File(anyString).exists();
            result = false;
        }};

        logbackLoggingConfigurator.configureLogging("/user/logback-test.xml");

        new Verifications() {{
            logbackLoggingConfigurator.configureLogback(anyString);
            times = 0;
        }};
    }

    @Test
    public void testConfigureLoggingWithExistingLogFile(@Mocked File file, @Mocked PropertyUtils propertyUtils) {
        new Expectations(logbackLoggingConfigurator) {{
            PropertyUtils.getPropertyValue(anyString, (Optional<Path>) any);
            result = "";

            new File(anyString).exists();
            result = true;

            logbackLoggingConfigurator.configureLogback(anyString);
            result = null;
        }};

        logbackLoggingConfigurator.configureLogging("/user/logback-test.xml");

        new Verifications() {{
            logbackLoggingConfigurator.configureLogback(anyString);
            times = 1;
        }};
    }

    @Test
    public void testGetDefaultLogbackConfigurationFileWithConfiguredDomibusLocation() throws Exception {
        String defaultLogbackConfigurationFile = logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();

        Assertions.assertEquals(File.separator + "user" + File.separator + "logback.xml", defaultLogbackConfigurationFile);
    }

    @Test
    public void testGetDefaultLogbackFilePathWithMissingDomibusLocation(final @Capturing Logger log) throws Exception {
        logbackLoggingConfigurator.domibusConfigLocation = null;
        logbackLoggingConfigurator.getDefaultLogbackConfigurationFile();

        new Verifications() {{
            log.error(anyString);
            times = 1;
        }};
    }

}
