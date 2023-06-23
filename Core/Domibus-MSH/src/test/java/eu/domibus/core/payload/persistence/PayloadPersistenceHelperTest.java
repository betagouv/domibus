package eu.domibus.core.payload.persistence;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.api.model.PartInfo;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Catalin Enache
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PayloadPersistenceHelperTest {

    @Tested
    PayloadPersistenceHelper payloadPersistenceHelper;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Test
    public void testValidatePayloadSize_PayloadSizeGreater_ExpectedException(
            final @Mocked LegConfiguration legConfiguration,
            final @Mocked PartInfo partInfo) {
        final int partInfoLength = 100;
        final int payloadProfileMaxSize = 40;
        final String payloadProfileName = "testProfile";
        new Expectations() {{
            legConfiguration.getPayloadProfile().getName();
            result = payloadProfileName;

            legConfiguration.getPayloadProfile().getMaxSize();
            result = payloadProfileMaxSize;

            partInfo.getLength();
            result = partInfoLength;
        }};

        try {
            payloadPersistenceHelper.validatePayloadSize(legConfiguration, partInfo.getLength());
            Assertions.fail("exception expected");
        } catch (InvalidPayloadSizeException e) {
            Assertions.assertEquals("[DOM_007]:Payload size [" + partInfoLength + "] is greater than the maximum value defined [" + payloadProfileMaxSize + "] for profile [" + payloadProfileName + "]",
                    e.getMessage());
        }
    }
}
