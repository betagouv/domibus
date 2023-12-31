package eu.domibus.core.logging;

import eu.domibus.api.cluster.Command;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
public class LoggingResetCommandTaskTest {

    @Tested
    LoggingResetCommandTask loggingResetCommandTask;

    @Injectable
    protected LoggingService loggingService;

    @Test
    public void canHandle() {
        assertTrue(loggingResetCommandTask.canHandle(Command.LOGGING_RESET));
    }

    @Test
    public void canHandleWithDifferentCommand() {
        assertFalse(loggingResetCommandTask.canHandle("anothercommand"));
    }

    @Test
    public void execute() {
        loggingResetCommandTask.execute(null);

        new Verifications() {{
            loggingService.resetLogging();
        }};
    }
}
