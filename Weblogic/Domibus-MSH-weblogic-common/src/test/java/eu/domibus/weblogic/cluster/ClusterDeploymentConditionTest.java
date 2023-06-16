package eu.domibus.weblogic.cluster;

import eu.domibus.api.cluster.ClusterDeploymentCondition;
import eu.domibus.api.property.DomibusConfigurationService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.0.1
 */
@ExtendWith(JMockitExtension.class)
public class ClusterDeploymentConditionTest {

    @Tested
    ClusterDeploymentCondition clusterDeploymentCondition;


    @Test
    public void testClusterDeploymentFalse(@Injectable ConditionContext context, @Injectable AnnotatedTypeMetadata metadata) {
        new Expectations() {{
            context.getEnvironment().getProperty(DomibusConfigurationService.CLUSTER_DEPLOYMENT);
            result = false;
        }};

        Assertions.assertFalse(clusterDeploymentCondition.matches(context, metadata));
    }

    @Test
    public void testClusterDeploymentTrue(@Injectable ConditionContext context, @Injectable AnnotatedTypeMetadata metadata) {
        new Expectations() {{
            context.getEnvironment().getProperty(DomibusConfigurationService.CLUSTER_DEPLOYMENT);
            result = true;
        }};

        assertTrue(clusterDeploymentCondition.matches(context, metadata));
    }
}
