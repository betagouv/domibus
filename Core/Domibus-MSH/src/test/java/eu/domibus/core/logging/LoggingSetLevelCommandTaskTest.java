package eu.domibus.core.logging;

import eu.domibus.api.cluster.Command;
import eu.domibus.api.cluster.CommandProperty;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class LoggingSetLevelCommandTaskTest {

    @Tested
    LoggingSetLevelCommandTask loggingSetLevelCommandTask;

    @Injectable
    protected LoggingService loggingService;

    @Test
    public void canHandle() {
        assertTrue(loggingSetLevelCommandTask.canHandle(Command.LOGGING_SET_LEVEL));
    }

    @Test
    public void canHandleWithDifferentCommand() {
        assertFalse(loggingSetLevelCommandTask.canHandle("anothercommand"));
    }

    @Test
    public void execute() {
        final Map<String, String> commandProperties = new HashMap<>();
        final String level = "DEBUG";
        final String name = "eu.domibus";
        commandProperties.put(CommandProperty.LOG_LEVEL, level);
        commandProperties.put(CommandProperty.LOG_NAME, name);

        loggingSetLevelCommandTask.execute(commandProperties);

        new Verifications() {{
            final String nameActual, levelActual;
            loggingService.setLoggingLevel(nameActual = withCapture(), levelActual = withCapture());
            Assertions.assertEquals(level, levelActual);
            Assertions.assertEquals(name, nameActual);
        }};


    }
}
