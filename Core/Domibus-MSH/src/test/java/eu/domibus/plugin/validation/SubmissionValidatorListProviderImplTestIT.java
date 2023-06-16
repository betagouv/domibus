package eu.domibus.plugin.validation;

import eu.domibus.core.plugin.validation.SubmissionValidatorListProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Created by baciuco on 08/08/2016.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:spring/submissionValidatorListProviderContext.xml")
public class SubmissionValidatorListProviderImplTestIT {

    @Autowired
    SubmissionValidatorListProvider submissionValidatorListProvider;

    @Test
    public void testGetSubmissionValidatorListWithOneMatch() throws Exception {
        SubmissionValidatorList wsSubmissionValidatorList = submissionValidatorListProvider.getSubmissionValidatorList("ws");
        Assertions.assertNotNull(wsSubmissionValidatorList);

    }

    @Test
    public void testGetSubmissionValidatorListWithNoMatch() throws Exception {
        SubmissionValidatorList wsSubmissionValidatorList = submissionValidatorListProvider.getSubmissionValidatorList("noExistingPlugin");
        Assertions.assertNull(wsSubmissionValidatorList);

    }

    @Test
    void testGetSubmissionValidatorListWithMultipleMatches() throws Exception {
        Assertions.assertThrows(SubmissionValidationException.class, () -> submissionValidatorListProvider.getSubmissionValidatorList("jms"));

    }
}
