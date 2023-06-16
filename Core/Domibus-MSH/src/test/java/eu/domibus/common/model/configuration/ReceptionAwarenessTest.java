package eu.domibus.common.model.configuration;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.pmode.PModeValidationException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;


import static eu.domibus.core.ebms3.sender.retry.RetryStrategy.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author François Gautier
 * @since 4.2.0
 */
public class ReceptionAwarenessTest {

    private ReceptionAwareness receptionAwareness;

    @BeforeEach
    public void setUp() {
        receptionAwareness = new ReceptionAwareness();
    }


    @Test
    public void init_null() {
        receptionAwareness.init(null);

        assertEquals(SEND_ONCE, receptionAwareness.getStrategy());
        assertEquals(0, receptionAwareness.getRetryTimeout());
        assertEquals(0, receptionAwareness.getRetryCount());
    }

    @Test
    public void init_okEmpty() {
        receptionAwareness.init(null);

        assertEquals(SEND_ONCE, receptionAwareness.getStrategy());
        assertEquals(0, receptionAwareness.getRetryTimeout());
        assertEquals(0, receptionAwareness.getRetryCount());
    }

    @Test
    public void init_ok() {
        receptionAwareness.retryXml = "2;3;CONSTANT";
        receptionAwareness.init(null);

        assertEquals(CONSTANT, receptionAwareness.getStrategy());
        assertEquals(2, receptionAwareness.getRetryTimeout());
        assertEquals(3, receptionAwareness.getRetryCount());
    }

    @Test
    public void init_ExceptionWrongStrategy() {
        receptionAwareness.retryXml = "2;3;TEST";
        try {
            receptionAwareness.init(null);
            Assertions.fail();
        } catch (DomibusCoreException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString(DomibusCoreErrorCode.DOM_003.getErrorCode()));
        }

        assertEquals(SEND_ONCE, receptionAwareness.getStrategy());
        assertEquals(2, receptionAwareness.getRetryTimeout());
        assertEquals(3, receptionAwareness.getRetryCount());
    }

    @Test
    public void init_exception() {
        receptionAwareness.retryXml = "";

        try {
            receptionAwareness.init(null);
            Assertions.fail();
        } catch (DomibusCoreException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString(DomibusCoreErrorCode.DOM_003.getErrorCode()));
        }

        assertEquals(SEND_ONCE, receptionAwareness.getStrategy());
        assertEquals(0, receptionAwareness.getRetryTimeout());
        assertEquals(0, receptionAwareness.getRetryCount());
    }

    @Test
    public void init_exception2() {
        receptionAwareness.retryXml = ";;";

        try {
            receptionAwareness.init(null);
            Assertions.fail();
        } catch (DomibusCoreException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString(DomibusCoreErrorCode.DOM_003.getErrorCode()));
        }

        assertEquals(SEND_ONCE, receptionAwareness.getStrategy());
        assertEquals(0, receptionAwareness.getRetryTimeout());
        assertEquals(0, receptionAwareness.getRetryCount());
    }

    @Test
    public void init_progressive_ok() {
        receptionAwareness.retryXml = "100;2;3;PROGRESSIVE";
        receptionAwareness.init(null);

        assertEquals(PROGRESSIVE, receptionAwareness.getStrategy());
        assertEquals(100, receptionAwareness.getRetryTimeout());
        assertEquals(4, receptionAwareness.getRetryIntervals().size()); // [2, 6, 18, 54]
        assertEquals(2, receptionAwareness.getInitialInterval());
    }

    @Test
    public void init_progressive_err_1() {
        receptionAwareness.retryXml = "100;2;0;PROGRESSIVE";

        PModeValidationException exception = assertThrows(
                PModeValidationException.class,
                () -> { receptionAwareness.init(null); }
        );

        assertEquals("[DOM_003]:PMode validation failed", exception.getMessage());
        assertEquals("multiplyingFactor should be greater than 1 for PROGRESSIVE strategy", exception.getIssues().stream().findFirst().get().getMessage());
    }

    @Test
    public void init_progressive_err_2() {
        receptionAwareness.retryXml = "10;2;1;PROGRESSIVE";

        PModeValidationException exception = assertThrows(
                PModeValidationException.class,
                () -> { receptionAwareness.init(null); }
        );

        assertEquals("[DOM_003]:PMode validation failed", exception.getMessage());
        assertEquals("multiplyingFactor should be greater than 1 for PROGRESSIVE strategy", exception.getIssues().stream().findFirst().get().getMessage());
    }

}
