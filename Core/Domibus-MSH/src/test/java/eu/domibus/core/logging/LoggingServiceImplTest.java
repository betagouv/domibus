package eu.domibus.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import eu.domibus.api.cluster.SignalService;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.core.converter.DomibusCoreMapper;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static eu.domibus.core.logging.LoggingServiceImpl.PREFIX_CLASS_;

/**
 * @author Catalin Enache
 * @since 4.1
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "unused", "UnusedAssignment"})
@ExtendWith(JMockitExtension.class)
public class LoggingServiceImplTest {

    @Injectable
    protected DomibusCoreMapper coreMapper;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    protected SignalService signalService;

    @Tested
    LoggingServiceImpl loggingService;

    @Test
    public void testSetLoggingLevel_LevelNotNull_LoggerLevelSet() {
        final String name = "eu.domibus";
        final String level = "INFO";

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        new Expectations(loggingService) {{
            loggingService.toLevel(level);
            result = Level.DEBUG;
        }};

        //tested method
        loggingService.setLoggingLevel(name, level);

        Assertions.assertEquals(Level.DEBUG, loggerContext.getLogger(name).getLevel());
    }

    @Test
    public void testSetLoggingLevel_LevelIsRoot_LoggerLevelSet() {
        final String name = "root";
        final String level = "INFO";

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        new Expectations(loggingService) {{
            loggingService.toLevel(level);
            result = Level.INFO;
        }};

        //tested method
        loggingService.setLoggingLevel(name, level);

        Assertions.assertEquals(Level.INFO, loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getLevel());
    }

    @Test
    public void testSetLoggingLevel_LevelNotRecognized_ExceptionThrown() {
        final String name = "eu.domibus";
        final String level = "BLA";

        try {
            //tested method
            loggingService.setLoggingLevel(name, level);
            Assertions.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assertions.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
            Assertions.assertTrue(le.getMessage().contains("Not a known log level"));
        }
    }


    @Test
    public void testSignalSetLoggingLevel_NoException_MessageSent() {
        final String name = "eu.domibus";
        final String level = "INFO";


        //tested method
        loggingService.signalSetLoggingLevel(name, level);

        new Verifications() {{
            String actualName, actualLevel;
            signalService.signalLoggingSetLevel(actualName = withCapture(), actualLevel = withCapture());
            Assertions.assertEquals(name, actualName);
            Assertions.assertEquals(level, actualLevel);
        }};
    }


    @Test
    public void testSignalSetLoggingLevel_ExceptionThrown_MessageNotSent() {
        final String name = "eu.domibus";
        final String level = "INFO";

        new Expectations(loggingService) {{
            signalService.signalLoggingSetLevel(name, level);
            result = new LoggingException("Error while sending topic message for setting logging level");
        }};

        try {
            //tested method
            loggingService.signalSetLoggingLevel(name, level);
            Assertions.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assertions.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
            Assertions.assertTrue(le.getMessage().contains("Error while sending topic message for setting logging level"));
        }
    }

    @Test
    public void testGetLoggingLevel_LoggerNameExact_ListReturned() {
        final String name = "eu.domibus";
        final boolean showClasses = false;

        //tested method
        List<LoggingEntry> loggingEntries = loggingService.getLoggingLevel(name, showClasses);

        Assertions.assertTrue(CollectionUtils.isNotEmpty(loggingEntries));
        Assertions.assertTrue(loggingEntries.get(0).getName().startsWith(name));
    }

    @Test
    public void testGetLoggingLevel_LoggerNameContainsWith_ListReturned() {
        final String name = "omibu";
        final boolean showClasses = false;

        //tested method
        List<LoggingEntry> loggingEntries = loggingService.getLoggingLevel(name, showClasses);

        Assertions.assertTrue(CollectionUtils.isNotEmpty(loggingEntries));
        Assertions.assertTrue(loggingEntries.get(0).getName().contains("domibus"));
    }

    @Test
    public void testGetLoggingLevel_ClassNotPresentInList() {

        List<LoggingEntry> loggingEntries = loggingService.getLoggingLevel("eu.domibus", true);
        List<LoggingEntry> loggingEntries2 = loggingService.getLoggingLevel("class", true);

        Predicate<LoggingEntry> findEntriesBeginWithClassPredicate = loggingEntry -> loggingEntry.getName().startsWith(PREFIX_CLASS_);
        Assertions.assertEquals(0, loggingEntries.stream().filter(findEntriesBeginWithClassPredicate).count(), "No logger entries should start with 'class '.");
        Assertions.assertEquals(0, loggingEntries2.stream().filter(findEntriesBeginWithClassPredicate).count(), "No logger entries should start with 'class '.");
    }

    @Test
    public void testResetLogging(final @Mocked LogbackLoggingConfigurator logbackLoggingConfigurator) {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        String domibusConfigLocation = "/home";//TODO

        new Expectations() {{
            domibusConfigurationService.getConfigLocation();
            result = domibusConfigLocation;

            LogbackLoggingConfigurator log = new LogbackLoggingConfigurator(domibusConfigLocation);

            log.getLoggingConfigurationFile();
            result = Objects.requireNonNull(this.getClass().getResource("/logback-test.xml")).getPath();
        }};

        //tested method
        loggingService.resetLogging();
        Assertions.assertEquals(Level.WARN, context.getLogger("org.apache.cxf").getLevel());

    }


    @Test
    public void testSignalResetLogging_NoException_MessageSent() {

        //tested method
        loggingService.signalResetLogging();

        new Verifications() {{
            signalService.signalLoggingReset();
        }};
    }

    @Test
    public void testSignalLoggingReset_ExceptionThrown_MessageNotSent() {

        new Expectations() {{
            signalService.signalLoggingReset();
            result = new LoggingException("Error while sending topic message for logging reset");
        }};

        try {
            //tested method
            loggingService.signalResetLogging();
            Assertions.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assertions.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
            Assertions.assertTrue(le.getMessage().contains("Error while sending topic message for logging reset"));
        }
    }

    @Test
    public void testToLevel() {
        String level = "ALL";
        Assertions.assertEquals(Level.ALL, loggingService.toLevel(level));

        level = "TRACE";
        Assertions.assertEquals(Level.TRACE, loggingService.toLevel(level));

        level = "DEBUG";
        Assertions.assertEquals(Level.DEBUG, loggingService.toLevel(level));

        level = "INFO";
        Assertions.assertEquals(Level.INFO, loggingService.toLevel(level));

        level = "ERROR";
        Assertions.assertEquals(Level.ERROR, loggingService.toLevel(level));

        level = "ALL";
        Assertions.assertEquals(Level.ALL, loggingService.toLevel(level));

        try {
            loggingService.toLevel(null);
            Assertions.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assertions.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
        }

        try {
            loggingService.toLevel("BLABLA");
            Assertions.fail("LoggingException expected");
        } catch (LoggingException le) {
            Assertions.assertEquals(DomibusCoreErrorCode.DOM_001, le.getError());
        }

    }

    /**
     * Testing that presence of inner class loggers will be caught for main class and will not impact other loggers at packge level.
     * If packge is supplied - expect true
     * if main class with child loggers due to inner class is supplied - expect false
     */
    @Test
    public void addChildLoggers_PresenceOfInnerClassReturnFalse(@Injectable Logger packageLogger, @Injectable Logger mainClassLogger, @Injectable Logger innerClassLogger) {

        final List<Logger> innerClassChildLoggers = new ArrayList<>();

        final List<Logger> mainClassChildLoggers = new ArrayList<>();
        mainClassChildLoggers.add(innerClassLogger);

        final List<Logger> packageChildLoggers = new ArrayList<>();
        packageChildLoggers.add(mainClassLogger);
        packageChildLoggers.addAll(mainClassChildLoggers);

        final String packageLoggerName = "org.springframework.security.config.annotation.authentication.configuration";
        final String mainClassLoggerName = "org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration";
        final String innerClassLoggerName = "org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration$EnableGlobalAuthenticationAutowiredConfigurer";

        new MockUp<FieldUtils>() {
            @Mock
            public Object readField(Object target, String fieldName, boolean forceAccess)  {
                if ("childrenList".equalsIgnoreCase(fieldName) && (target instanceof Logger)) {
                    if (packageLoggerName.equalsIgnoreCase(((Logger) target).getName())) {
                        return packageChildLoggers;
                    }
                    if (mainClassLoggerName.equalsIgnoreCase(((Logger) target).getName())) {
                        return mainClassChildLoggers;
                    }
                    if (innerClassLoggerName.equalsIgnoreCase(((Logger) target).getName())) {
                        return innerClassChildLoggers;
                    }
                }
                return null;
            }
        };

        new Expectations() {{
            packageLogger.getName();
            result = packageLoggerName;

            mainClassLogger.getName();
            result = mainClassLoggerName;

            innerClassLogger.getName();
            result = innerClassLoggerName;
        }};

        Assertions.assertFalse(loggingService.addChildLoggers(mainClassLogger, false), "Main Class having child loggers due to inner class should return false");
        Assertions.assertTrue(loggingService.addChildLoggers(packageLogger, false), "Package having child loggers due to main classes should return true");
        Assertions.assertFalse(loggingService.addChildLoggers(innerClassLogger, false), "Inner Class having no child loggers should return false");
        Assertions.assertTrue(loggingService.addChildLoggers(innerClassLogger, true), "ShowClasses being enabled should always return true");
    }
}
