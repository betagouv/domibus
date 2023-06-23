package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.common.model.configuration.Configuration;
import mockit.FullVerifications;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author François Gautier
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PropertiesValidatorTest extends AbstractValidatorTest {
    @Tested
    PropertiesValidator validator;

    @Test
    public void validate() throws IOException {
        Configuration configuration = newConfiguration("TestConfiguration.json");
        final List<ValidationIssue> issues = validator.validate(configuration);
        assertEquals(1, issues.size());
        assertThat(issues.get(0).getMessage(), is("PropertyRef [propertyRefNotDefined] is not defined in properties"));
        assertThat(issues.get(0).getLevel(), is(ValidationIssue.Level.ERROR));
        new FullVerifications() {};
    }
}
