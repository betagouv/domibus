package eu.domibus.core.cache;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.cluster.Command;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class Evict2LCachesCommandTaskTest {

    @Tested
    private Evict2LCachesCommandTask evict2LCachesCommandTask;

    @Injectable
    protected DomibusLocalCacheService domibusLocalCacheService;

    @Test
    public void canHandle() {
        assertTrue(evict2LCachesCommandTask.canHandle(Command.EVICT_2LC_CACHES));
    }

    @Test
    public void canHandleWithDifferentCommand() {
        assertFalse(evict2LCachesCommandTask.canHandle("anothercommand"));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void execute(@Injectable Map<String, String> properties) {
        evict2LCachesCommandTask.execute(properties);

        new FullVerifications() {{
            domibusLocalCacheService.clear2LCCaches(false);
        }};
    }
}
