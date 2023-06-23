package eu.domibus.core.message.splitandjoin;

import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Splitting;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)

public class SplitAndJoinConfigurationServiceTest {

    @Tested
    SplitAndJoinConfigurationService splitAndJoinConfigurationService;

    @Test
    public void mayUseSplitAndJoin(@Injectable LegConfiguration legConfiguration, @Injectable Splitting splitting) {
        new Expectations() {{
            legConfiguration.getSplitting();
            result = splitting;
        }};

        Assertions.assertTrue(splitAndJoinConfigurationService.mayUseSplitAndJoin(legConfiguration));
    }

}
