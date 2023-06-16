package eu.domibus.core.status;

import eu.domibus.core.cxf.DomibusBus;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.neethi.AssertionBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JMockitExtension.class)
public class DomibusStatusServiceTest {

    @Injectable
    private DomibusBus busCore;

    @Tested
    private DomibusStatusService domibusStatusService;

    @Test
    public void testReady(@Mocked final org.apache.neethi.PolicyBuilder policyBuilder,
                          @Mocked final AssertionBuilderFactory assertionBuilderFactory) {
        new Expectations() {{
            busCore.getExtension(PolicyBuilder.class);
            this.result = policyBuilder;
            policyBuilder.getAssertionBuilderFactory();
            result = assertionBuilderFactory;
        }};
        assertFalse(domibusStatusService.isNotReady());
    }

    @Test
    public void testNotReady(@Mocked final org.apache.neethi.PolicyBuilder policyBuilder) {
        new Expectations() {{
            busCore.getExtension(PolicyBuilder.class);
            this.result = policyBuilder;
            policyBuilder.getAssertionBuilderFactory();
            result = null;
        }};
        assertTrue(domibusStatusService.isNotReady());
    }

}
