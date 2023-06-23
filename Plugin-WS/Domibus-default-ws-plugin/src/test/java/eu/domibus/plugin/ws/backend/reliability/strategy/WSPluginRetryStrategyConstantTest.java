package eu.domibus.plugin.ws.backend.reliability.strategy;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.util.Date;

import static java.time.LocalDateTime.of;
import static java.time.ZoneId.systemDefault;
import static java.util.Date.from;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginRetryStrategyConstantTest {

    @Tested
    private WSPluginRetryStrategyConstant retryStrategyConstant;

    @Test
    public void calculateNextAttempt_CONSTANT_noDateReceived() {
        Date nextAttempt = retryStrategyConstant.calculateNextAttempt(null, 1, 2);
        assertNull(nextAttempt);
    }

    @Test
    public void calculateNextAttempt_CONSTANT_attemptsNegative() {
        Date nextAttempt = retryStrategyConstant.calculateNextAttempt(new Date(), -1, 2);
        assertNull(nextAttempt);
    }

    @Test
    public void calculateNextAttempt_CONSTANT_timeoutNegative() {
        Date nextAttempt = retryStrategyConstant.calculateNextAttempt(new Date(), 1, -2);
        assertNull(nextAttempt);
    }

    @Test
    public void calculateNextAttempt_CONSTANT_ok() {
        ZonedDateTime received = of(3020, 12, 31, 12, 0).atZone(systemDefault());
        Date expected = from(received.plusMinutes(1).toInstant());
        Date nextAttempt = retryStrategyConstant.calculateNextAttempt(from(received.toInstant()), 10, 10);
        assertEquals(expected, nextAttempt);
    }
}
