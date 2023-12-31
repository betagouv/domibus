package eu.domibus.ext.delegate.services.message;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.util.AOPUtil;
import eu.domibus.ext.domain.MessageAcknowledgementDTO;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.MessageAcknowledgeExtException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class MessageAcknowledgeServiceInterceptorTest {

    @Tested
    MessageAcknowledgeServiceInterceptor messageAcknowledgeServiceInterceptor;

    @Injectable
    AOPUtil aopUtil;

    @Test
    public void testIntercept(@Injectable final ProceedingJoinPoint joinPoint) throws Throwable {
        final MessageAcknowledgementDTO messageAcknowledgementDTO = new MessageAcknowledgementDTO();

        new Expectations() {{
            joinPoint.proceed();
            result = messageAcknowledgementDTO;
        }};

        final Object targetResult = messageAcknowledgeServiceInterceptor.intercept(joinPoint);
        Assertions.assertEquals(messageAcknowledgementDTO, targetResult);
    }

    @Test
    public void testInterceptWhenExtExceptionIsRaised(@Injectable final ProceedingJoinPoint joinPoint) throws Throwable {
        final MessageAcknowledgeExtException messageAcknowledgeException = new MessageAcknowledgeExtException(DomibusErrorCode.DOM_001, "test");
        new Expectations() {{
            joinPoint.proceed();
            result = messageAcknowledgeException;
        }};

        try {
            messageAcknowledgeServiceInterceptor.intercept(joinPoint);
        } catch (MessageAcknowledgeExtException e) {
            //the thrown exception object must be the same as the original exception raised(no mapping is needed)
            Assertions.assertTrue(messageAcknowledgeException == e);
        }
    }

    @Test
    public void testInterceptWhenCoreExceptionIsRaised(@Injectable final ProceedingJoinPoint joinPoint) throws Throwable {
        final eu.domibus.api.message.acknowledge.MessageAcknowledgeException messageAcknowledgeException = new eu.domibus.api.message.acknowledge.MessageAcknowledgeException(DomibusCoreErrorCode.DOM_001, "test");
        new Expectations() {{
            joinPoint.proceed();
            result = messageAcknowledgeException;
        }};

        try {
            messageAcknowledgeServiceInterceptor.intercept(joinPoint);
        } catch (MessageAcknowledgeExtException e) {
            //the thrown exception object must be the same as the original exception raised(no mapping is needed)
            Assertions.assertTrue(e.getCause() == messageAcknowledgeException);
        }
    }
}
