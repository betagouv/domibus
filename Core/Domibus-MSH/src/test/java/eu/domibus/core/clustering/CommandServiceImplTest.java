package eu.domibus.core.clustering;

import eu.domibus.api.server.ServerInfoService;
import eu.domibus.core.converter.CommandCoreMapper;
import eu.domibus.ext.services.CommandExtTask;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 4.0.1
 */
@ExtendWith(JMockitExtension.class)
public class CommandServiceImplTest {

    @Tested
    private CommandServiceImpl commandService;

    @Injectable
    private CommandDao commandDao;

    @Injectable
    private CommandCoreMapper commandCoreMapper;

    @Injectable
    private ServerInfoService serverInfoService;


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
