package eu.domibus.core.plugin.routing.operation;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 3.2.5
 */
@ExtendWith(JMockitExtension.class)
public class AndOperationTest {

    @Tested
    AndOperation andOperation;

    @Test
    public void testGetResultWithTrueAndFalse() throws Exception {
        andOperation.addIntermediateResult(true);
        andOperation.addIntermediateResult(false);

        assertFalse(andOperation.getResult());
    }

    @Test
    public void testGetResultWithFalseAndTrue() throws Exception {
        andOperation.addIntermediateResult(false);
        andOperation.addIntermediateResult(true);

        assertFalse(andOperation.getResult());
    }

    @Test
    public void testGetResultWithTrueAndTrue() throws Exception {
        andOperation.addIntermediateResult(true);
        andOperation.addIntermediateResult(true);

        assertTrue(andOperation.getResult());
    }

    @Test
    public void testGetResultWithShortCircuit() throws Exception {
        andOperation.addIntermediateResult(true);
        assertFalse(andOperation.canShortCircuitOperation());
        andOperation.addIntermediateResult(false);
        assertTrue(andOperation.canShortCircuitOperation());
        andOperation.addIntermediateResult(true);
        assertFalse(andOperation.getResult());
    }
}
