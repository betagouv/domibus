package eu.domibus.core.pmode.validation;

import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.pmode.validation.validators.LegConfigurationValidator;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(JMockitExtension.class)
public class PModeValidationServiceImplTest {

    PModeValidationServiceImpl pModeValidationService;

    List<PModeValidator> pModeValidatorList = new ArrayList<>();

    @Injectable
    LegConfigurationValidator legConfigurationValidator;

    @BeforeEach
    public void init() {
        pModeValidationService = new PModeValidationServiceImpl();
        pModeValidatorList.add(legConfigurationValidator);
        ReflectionTestUtils.setField(pModeValidationService, "pModeValidatorList", pModeValidatorList);
    }

    @Test
    public void validate_Disabled(@Injectable Configuration configuration) {

        List<ValidationIssue> issues = pModeValidationService.validate(configuration);

        new Verifications() {{
            legConfigurationValidator.validate(configuration);
            times = 1;
        }};

        Assertions.assertEquals(0, issues.size());
    }

    @Test
    void validate_Error(@Injectable Configuration configuration) {

        ValidationIssue issue = new ValidationIssue();
        issue.setLevel(ValidationIssue.Level.ERROR);
        issue.setMessage("Leg configuration is wrong");

        new Expectations() {{
            configuration.preparePersist();

            legConfigurationValidator.validate(configuration);
            result = Collections.singletonList(issue);
        }};

        Assertions.assertThrows(PModeValidationException.class, () -> pModeValidationService.validate(configuration));

        new Verifications() {{
            legConfigurationValidator.validate(configuration);
            times = 1;
        }};
    }


}
