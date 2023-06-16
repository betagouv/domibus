package eu.domibus.plugin.ws.backend.reliability.strategy;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginRetryStrategySendOnceTest {
    @Tested
    private WSPluginRetryStrategySendOnce retryStrategySendOnce;

    @Test
    public void calculateNextAttempt_SEND_ONCE() {
        Date nextAttempt = retryStrategySendOnce.calculateNextAttempt(null, 1, 2);
        assertNull(nextAttempt);
    }
}
