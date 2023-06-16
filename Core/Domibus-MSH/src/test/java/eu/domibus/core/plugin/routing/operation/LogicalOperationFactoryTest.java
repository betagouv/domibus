package eu.domibus.core.plugin.routing.operation;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 3.2.5
 */
@ExtendWith(JMockitExtension.class)
public class LogicalOperationFactoryTest {

    @Tested
    LogicalOperationFactory logicalOperationFactory;


    @Test
    public void testCreateAndOperation() throws Exception {
        final LogicalOperation andOperation = logicalOperationFactory.create(LogicalOperator.AND);
        Assertions.assertTrue(andOperation instanceof AndOperation);
    }

    @Test
    public void testCreateOrOperation() throws Exception {
        final LogicalOperation orOperation = logicalOperationFactory.create(LogicalOperator.OR);
        Assertions.assertTrue(orOperation instanceof OrOperation);
    }
}
