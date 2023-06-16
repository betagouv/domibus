package eu.domibus.core.message;

import eu.domibus.common.ErrorCode;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;

import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static eu.domibus.common.ErrorCode.EBMS_0001;

public class UserMessageErrorCreatorTest {

    @Tested
    UserMessageErrorCreator userMessageErrorCreator;

    @Test
    public void createErrorResultTest() {
        EbMS3Exception refToMessageId = EbMS3ExceptionBuilder.getInstance().ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0002).refToMessageId("refToMessageId").build();
        //when
        Assertions.assertNotNull(userMessageErrorCreator.createErrorResult(refToMessageId));

        new FullVerifications() {
        };
    }

}
