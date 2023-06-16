package eu.domibus.ext.delegate.services.monitoring;

import eu.domibus.api.monitoring.DomibusMonitoringService;
import eu.domibus.api.util.AOPUtil;
import eu.domibus.ext.domain.monitoring.MonitoringInfoDTO;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.DomibusMonitoringExtException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DomibusMonitoringEbms3ServiceInterceptorTest {

    @Tested
    DomibusMonitoringServiceInterceptor domibusMonitoringServiceInterceptor;

    @Injectable
    DomibusMonitoringService domibusMonitoringService;

    @Injectable
    AOPUtil aopUtil;

    @Test
    public void testIntercept(@Injectable final ProceedingJoinPoint joinPoint) throws Throwable {
        // Given
        final MonitoringInfoDTO monitoringInfoDTO = new MonitoringInfoDTO();

        new Expectations() {{
            joinPoint.proceed();
            result = monitoringInfoDTO;
        }};

        // When
        final Object interceptedResult = domibusMonitoringServiceInterceptor.intercept(joinPoint);

        // Then
        Assertions.assertEquals(monitoringInfoDTO, interceptedResult);
    }

    @Test
    public void testInterceptWhenExtExceptionIsRaised(@Injectable final ProceedingJoinPoint joinPoint) throws Throwable {
        // Given
        final DomibusMonitoringExtException domibusMonitoringExtException = new DomibusMonitoringExtException(DomibusErrorCode.DOM_001, "test");

        new Expectations() {{
            joinPoint.proceed();
            result = domibusMonitoringExtException;
        }};

        // When
        try {
            domibusMonitoringServiceInterceptor.intercept(joinPoint);
        } catch (DomibusMonitoringExtException e) {
            // Then
            Assertions.assertTrue(domibusMonitoringExtException == e);
            return;
        }
        Assertions.fail();
    }

    @Test
    public void testInterceptWhenCoreExceptionIsRaised(@Injectable final ProceedingJoinPoint joinPoint) throws Throwable {
        // Given
        final DomibusMonitoringExtException domibusMonitoringExtException = new DomibusMonitoringExtException(DomibusErrorCode.DOM_001, "test");

        new Expectations() {{
            joinPoint.proceed();
            result = domibusMonitoringExtException;
        }};

        // When
        try {
            domibusMonitoringServiceInterceptor.intercept(joinPoint);
        } catch (DomibusMonitoringExtException e) {
            Assertions.assertTrue(domibusMonitoringExtException == e);
            return;
        }
        Assertions.fail();
    }
}
