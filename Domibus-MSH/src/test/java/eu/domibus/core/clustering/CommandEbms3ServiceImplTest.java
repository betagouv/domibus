package eu.domibus.core.clustering;

import eu.domibus.api.server.ServerInfoService;
import eu.domibus.core.converter.CommandCoreMapper;
import eu.domibus.ext.services.CommandExtTask;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 4.0.1
 */
@RunWith(JMockit.class)
public class CommandEbms3ServiceImplTest {

    @Tested
    private CommandServiceImpl commandService;

    @Injectable
    protected CommandDao commandDao;

    @Injectable
    protected CommandCoreMapper commandCoreMapper;

    @Injectable
    protected ServerInfoService serverInfoService;

    @Injectable
    protected List<CommandTask> commandTasks;

    @Injectable
    protected List<CommandExtTask> pluginCommands;

    @Test
    public void testCreateClusterCommand() {
        String command = "command1";
        String server = "server1";

        commandService.createClusterCommand(command, server, null);

        new Verifications() {{
            CommandEntity entity = null;
            commandDao.create(entity = withCapture());

            assertEquals(entity.getCommandName(), command);
            assertEquals(entity.getServerName(), server);
        }};
    }

    @Test
    public void testFindCommandsByServerName() {
        String server = "server1";

        commandService.findCommandsByServerName(server);

        new Verifications() {{
            commandDao.findCommandsByServerName(server);
        }};
    }
}