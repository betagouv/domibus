package eu.domibus.core.clustering;

import eu.domibus.api.cluster.Command;
import eu.domibus.api.cluster.CommandProperty;
import eu.domibus.api.cluster.CommandService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.server.ServerInfoService;
import eu.domibus.ext.services.CommandExtTask;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author idragusa
 * @author Cosmin Baciu
 * @since 4.2
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "DataFlowIssue"})
@ExtendWith(JMockitExtension.class)
public class CommandExecutorServiceImplTest {

    private CommandExecutorServiceImpl commandExecutorService;

    @Injectable
    private CommandService commandService;

    @Injectable
    private ServerInfoService serverInfoService;

    @Injectable
    private DomainTaskExecutor domainTaskExecutor;

    Map<String, String> commandProperties = new HashMap<>();

    @BeforeEach
    void setUp() {
        commandExecutorService = new CommandExecutorServiceImpl(commandService, serverInfoService, new ArrayList<>(), new ArrayList<>());
        commandProperties.put(CommandProperty.ORIGIN_SERVER, "server1");
    }

    @Test
    public void testExecuteCommands(@Mocked Command command1, @Mocked Command command2) {
        String server1 = "server1";
        List<Command> commands = new ArrayList<>();
        commands.add(command1);
        commands.add(command2);


        new Expectations(commandExecutorService) {{
            command1.getCommandName();
            result = Command.RELOAD_PMODE;

            command2.getCommandName();
            result = Command.RELOAD_TRUSTSTORE;

            commandService.findCommandsByServerName(server1);
            result = commands;

        }};

        commandExecutorService.executeCommands(server1);

        new Verifications() {{
            commandExecutorService.executeAndDeleteCommand(command1);
            times = 1;

            commandExecutorService.executeAndDeleteCommand(command2);
            times = 1;
        }};
    }

    @Test
    public void executeCommand(@Injectable Domain domain,
                               @Injectable CommandTask commandTask) {
        String command = "mycommand";

        new Expectations(commandExecutorService) {{
            commandExecutorService.skipCommandSameServer(command, commandProperties);
            result = false;

            commandExecutorService.getCommandTask(command);
            result = commandTask;
        }};

        commandExecutorService.executeCommand(command, commandProperties);

        new Verifications() {{
            commandTask.execute(commandProperties);
            times = 1;

            commandExecutorService.getPluginCommand(command);
            times = 0;
        }};
    }

    @Test
    public void executePluginCommand(@Injectable CommandExtTask commandTask) {
        String command = "mycommand";

        new Expectations(commandExecutorService) {{
            commandExecutorService.skipCommandSameServer(command, commandProperties);
            result = false;

            commandExecutorService.getCommandTask(command);
            result = null;

            commandExecutorService.getPluginCommand(command);
            result = commandTask;
        }};

        commandExecutorService.executeCommand(command, commandProperties);

        new Verifications() {{
            commandTask.execute(commandProperties);
            times = 1;
        }};
    }

    @Test
    public void executeAndDeleteCommand(@Injectable Command commandTask) {
        new Expectations(commandExecutorService) {{
            commandExecutorService.executeCommand(commandTask.getCommandName(), commandTask.getCommandProperties());
        }};

        commandExecutorService.executeAndDeleteCommand(commandTask);

        new Verifications() {{
            commandService.deleteCommand(commandTask.getEntityId());
        }};
    }

    @Test
    public void skipCommandSameServer(@Injectable CommandExtTask commandTask) {
        String command = "mycommand";
        String originServerName = "server1";

        new Expectations() {{
            commandProperties.get(CommandProperty.ORIGIN_SERVER);
            result = originServerName;

            serverInfoService.getServerName();
            result = originServerName;
        }};

        assertTrue(commandExecutorService.skipCommandSameServer(command, commandProperties));
    }

    @Test
    public void skipCommandSameServer_NullCommandProperties(@Injectable CommandExtTask commandTask) {
        String command = "mycommand";

        assertTrue(commandExecutorService.skipCommandSameServer(command, null));
    }

    @Test
    @Disabled
    public void skipCommandSameServer_NullOriginServerProperty(@Injectable CommandExtTask commandTask) {
        String command = "mycommand";
        commandProperties.put(CommandProperty.ORIGIN_SERVER, null);

        assertTrue(commandExecutorService.skipCommandSameServer(command, commandProperties));
    }

    @Test
    public void skipCommandSameServer_BlankOriginServerProperty(@Injectable CommandExtTask commandTask) {
        String command = "mycommand";
        commandProperties.put(CommandProperty.ORIGIN_SERVER, " ");
        assertTrue(commandExecutorService.skipCommandSameServer(command, commandProperties));
    }

    @Test
    public void skipCommandSameServer_DoesNotSkipOnSeparateServer(@Injectable CommandExtTask commandTask) {
        String command = "mycommand";

        new Expectations() {{
            serverInfoService.getServerName();
            result = "server2";
        }};

        assertFalse(commandExecutorService.skipCommandSameServer(command, commandProperties));
    }
}
