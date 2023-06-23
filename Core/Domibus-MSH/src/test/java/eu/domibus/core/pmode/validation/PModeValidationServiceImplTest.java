package eu.domibus.core.pmode.validation;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.pmode.validation.validators.LegConfigurationValidator;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class PModeValidationServiceImplTest {

    @Tested
    PModeValidationServiceImpl pModeValidationService;

    @Injectable
    List<PModeValidator> pModeValidatorList = new ArrayList<PModeValidator>();

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    LegConfigurationValidator legConfigurationValidator;

    @BeforeEach
    public void init() {
        pModeValidatorList.add(legConfigurationValidator);
    }

    @Test
    public void validate_Disabled(@Mocked Configuration configuration) {

        List<ValidationIssue> issues = pModeValidationService.validate(configuration);

        new Verifications() {{
            legConfigurationValidator.validate(configuration);
            times = 1;
        }};

        Assertions.assertTrue(issues.size() == 0);
    }

    @Test
    void validate_Error(@Mocked Configuration configuration) {

        ValidationIssue issue = new ValidationIssue();
        issue.setLevel(ValidationIssue.Level.ERROR);
        issue.setMessage("Leg configuration is wrong");

        new Expectations() {{
            configuration.preparePersist();

            legConfigurationValidator.validate(configuration);
            result = Arrays.asList(issue);
        }};

        Assertions.assertThrows(PModeValidationException.class, () -> pModeValidationService.validate(configuration));

        new Verifications() {{
            legConfigurationValidator.validate(configuration);
            times = 1;
        }};
    }


}
