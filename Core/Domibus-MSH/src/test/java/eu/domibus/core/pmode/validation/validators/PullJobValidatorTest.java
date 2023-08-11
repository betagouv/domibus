package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.pmode.validation.PModeValidationHelperImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_MSH_PULL_CRON;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author François Gautier
 * @since 5.2
 */
@ExtendWith(JMockitExtension.class)
class PullJobValidatorTest extends AbstractValidatorTest {

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    void validate() throws Exception {
        PullJobValidator validator = new PullJobValidator(new PModeValidationHelperImpl(), domibusPropertyProvider);

        new Expectations(){{
            domibusPropertyProvider.getProperty(DOMIBUS_MSH_PULL_CRON);
            result = "0 */10 * * * ?";
        }};

        Configuration configuration = newConfiguration("PullJobConfiguration.json");
        final List<ValidationIssue> results = validator.validate(configuration);
        assertEquals(1, results.size());
        assertEquals("Leg [leg2] retryTimout [5 min] is inferior to Pull cron job interval [domibus.msh.pull.cron]: [10 min]", results.get(0).getMessage());
    }
}
