package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author musatmi
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class RolesValidatorTest extends AbstractValidatorTest {

    @Injectable
    PModeValidationHelper pModeValidationHelper;

    @Test
    public void validate() throws Exception {
        RolesValidator validator = new RolesValidator(pModeValidationHelper);

        Configuration configuration = newConfiguration("RolesConfiguration.json");
        final List<ValidationIssue> results = validator.validate(configuration);
        assertTrue(results.size() == 2);
        assertEquals("For the business process [TestProcess], the initiator role name and the responder role name are identical [eCODEXRole]", results.get(0).getMessage());
        assertEquals("For the business process [TestProcess], the initiator role value and the responder role value are identical [GW]", results.get(1).getMessage());
    }
}
