package eu.domibus.core.cache;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.cluster.Command;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
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
public class EvictCachesCommandTaskTest {

    @Tested
    private EvictCachesCommandTask evictCachesCommandTask;

    @Injectable
    protected DomibusLocalCacheService domibusLocalCacheService;

    @Test
    public void canHandle() {
        assertTrue(evictCachesCommandTask.canHandle(Command.EVICT_CACHES));
    }

    @Test
    public void canHandleWithDifferentCommand() {
        assertFalse(evictCachesCommandTask.canHandle("anothercommand"));
    }

    @Test
    public void execute() {

        evictCachesCommandTask.execute(new HashMap<>());

        new FullVerifications() {{
            domibusLocalCacheService.clearAllCaches(false);
        }};
    }
}
