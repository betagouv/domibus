package eu.domibus.core.message.pull;

import com.google.common.collect.Sets;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.core.message.pull.DomainPullFrequencyHelper.DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE_PER_MPC_PREFIX;

/**
 * @author Sebastian-Ion TINCU
 */
@ExtendWith(JMockitExtension.class)
public class DomainPullFrequencyHelperTest {

    @Tested
    private DomainPullFrequencyHelper domainPullFrequencyHelper = new DomainPullFrequencyHelper(new Domain("code", "name"));

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    private String defaultPullFrequency = "13";

    @Test
    public void setMpcNames_DefaultPullFrequency() {
        // GIVEN
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE);
            result = defaultPullFrequency;
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE_PER_MPC_PREFIX + "defaultMPC");
            result = defaultPullFrequency;
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_FREQUENCY_RECOVERY_TIME);
            result = "10";
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_FREQUENCY_ERROR_COUNT);
            result = "10";
        }};

        // WHEN
        domainPullFrequencyHelper.setMpcNames(Sets.newHashSet("defaultMPC"));

        // THEN
        new Verifications() {{
            Map<String, MpcPullFrequency> mpcPullFrequencyMap = (Map<String, MpcPullFrequency>) ReflectionTestUtils.getField(domainPullFrequencyHelper, "mpcPullFrequencyMap");
            Assertions.assertTrue(mpcPullFrequencyMap.containsKey("defaultMPC"), "Should have populated the pull frequency map for the default MPC");

            Integer pullFrequency = (Integer) ReflectionTestUtils.getField(mpcPullFrequencyMap.get("defaultMPC"), "maxRequestsPerMpc");
            Assertions.assertEquals(Integer.valueOf(defaultPullFrequency), pullFrequency, "Should have used the correct custom frequency for the default MPC");
        }};
    }

    @Test
    public void setMpcNames_CustomPullFrequency() {
        // GIVEN
        final String customPullFrequency = "5";
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE);
            result = defaultPullFrequency;
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_SEND_PER_JOB_CYCLE_PER_MPC_PREFIX + "defaultMPC");
            result = customPullFrequency;
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_FREQUENCY_RECOVERY_TIME);
            result = "10";
            domibusPropertyProvider.getProperty(DOMIBUS_PULL_REQUEST_FREQUENCY_ERROR_COUNT);
            result = "10";
        }};

        // WHEN
        domainPullFrequencyHelper.setMpcNames(Sets.newHashSet("defaultMPC"));

        // THEN
        new Verifications() {{
            Map<String, MpcPullFrequency> mpcPullFrequencyMap = (Map<String, MpcPullFrequency>) ReflectionTestUtils.getField(domainPullFrequencyHelper, "mpcPullFrequencyMap");
            Assertions.assertTrue(mpcPullFrequencyMap.containsKey("defaultMPC"), "Should have populated the pull frequency map for the default MPC");

            Integer pullFrequency = (Integer) ReflectionTestUtils.getField(mpcPullFrequencyMap.get("defaultMPC"), "maxRequestsPerMpc");
            Assertions.assertEquals(Integer.valueOf(customPullFrequency), pullFrequency, "Should have used the correct custom frequency for the default MPC");
        }};
    }
}
