package eu.domibus.plugin.validation;

import eu.domibus.core.plugin.validation.SubmissionValidatorListProviderImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

/**
 * Created by baciuco on 08/08/2016.
 */
@ExtendWith(JMockitExtension.class)
public class SubmissionValidatorListProviderImplTest {

    @Injectable
    ApplicationContext applicationContext;

    @Tested
    SubmissionValidatorListProviderImpl submissionValidatorListProvider;

    @Test
    public void testGetSubmissionValidatorListWithOneBeanFound() throws Exception {
        new Expectations() {{
            applicationContext.getBeanNamesForType(SubmissionValidatorList.class);
            result = new String[]{"wsPlugin", "jmsPlugin", "customPlugin"};
        }};

        submissionValidatorListProvider.getSubmissionValidatorList("customPlugin");

        new Verifications() {{
            String springBean = null;
            applicationContext.getBean(springBean = withCapture(), SubmissionValidatorList.class);
            times = 1;

            Assertions.assertEquals(springBean, "customPlugin");
        }};
    }

    @Test
    public void testGetSubmissionValidatorListWithNoBeanFound() throws Exception {
        new Expectations() {{
            applicationContext.getBeanNamesForType(SubmissionValidatorList.class);
            result = new String[]{"wsPlugin", "jmsPlugin", "customPlugin"};
        }};

        SubmissionValidatorList noPlugin = submissionValidatorListProvider.getSubmissionValidatorList("noPlugin");
        Assertions.assertNull(noPlugin);

        new Verifications() {{
            applicationContext.getBean(anyString, SubmissionValidatorList.class);
            times = 0;
        }};
    }
}
