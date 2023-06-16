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
public class OrOperationTest {

    @Tested
    OrOperation orOperation;

    @Test
    public void testGetResultWithTrueAndFalse() throws Exception {
        orOperation.addIntermediateResult(true);
        orOperation.addIntermediateResult(false);

        assertTrue(orOperation.getResult());
    }

    @Test
    public void testGetResultWithFalseAndTrue() throws Exception {
        orOperation.addIntermediateResult(false);
        orOperation.addIntermediateResult(true);

        assertTrue(orOperation.getResult());
    }

    @Test
    public void testGetResultWithTrueAndTrue() throws Exception {
        orOperation.addIntermediateResult(true);
        orOperation.addIntermediateResult(true);

        assertTrue(orOperation.getResult());
    }

    @Test
    public void testGetResultWithFalseAndFalse() throws Exception {
        orOperation.addIntermediateResult(false);
        orOperation.addIntermediateResult(false);

        assertFalse(orOperation.getResult());
    }

    @Test
    public void testGetResultWithShortCircuit() throws Exception {
        orOperation.addIntermediateResult(true);
        assertTrue(orOperation.canShortCircuitOperation());
        orOperation.addIntermediateResult(false);
        assertTrue(orOperation.canShortCircuitOperation());
        orOperation.addIntermediateResult(true);
        assertTrue(orOperation.getResult());
    }
}
