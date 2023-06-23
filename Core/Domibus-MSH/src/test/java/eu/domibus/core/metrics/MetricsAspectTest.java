package eu.domibus.core.metrics;

import com.codahale.metrics.MetricRegistry;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class MetricsAspectTest {

    @Injectable
    protected MetricRegistry metricRegistry;

    @Tested
    private MetricsAspect metricsAspect;


    @Test
    public void surroundWithATimer(@Mocked ProceedingJoinPoint pjp,@Mocked  Timer timer,@Mocked com.codahale.metrics.Timer.Context methodTimer) throws Throwable {
        String metricName = "MetricName";
        Class<String> testClass = String.class;
        new Expectations(){{
            timer.value();
            result= metricName;
            timer.clazz();
            result=testClass;
            metricRegistry.timer("java.lang.String."+metricName+".timer").time();
            result=methodTimer;
        }};
        metricsAspect.surroundWithATimer(pjp,timer);
        new Verifications(){{
            pjp.proceed();
            methodTimer.stop();
        }};
    }
    @Test
    public void surroundWithATimerInPlugin(@Mocked ProceedingJoinPoint pjp,@Mocked  eu.domibus.ext.domain.metrics.Timer timer,@Mocked com.codahale.metrics.Timer.Context methodTimer) throws Throwable {
        String metricName = "MetricName";
        Class<String> testClass = String.class;
        new Expectations(){{
            timer.value();
            result= metricName;
            timer.clazz();
            result=testClass;
            metricRegistry.timer("java.lang.String."+metricName+".timer").time();
            result=methodTimer;
        }};
        metricsAspect.surroundWithATimer(pjp,timer);
        new Verifications(){{
            pjp.proceed();
            methodTimer.stop();
        }};
    }

    @Test
    public void surroundWithACounter(@Mocked ProceedingJoinPoint pjp,@Mocked  Counter timer,@Mocked com.codahale.metrics.Counter counter) throws Throwable {
        String metricName = "MetricName";
        Class<String> testClass = String.class;
        new Expectations(){{
            timer.value();
            result= metricName;
            timer.clazz();
            result=testClass;
            com.codahale.metrics.Counter counter = metricRegistry.counter("java.lang.String." + metricName + ".counter");
            result=counter;
        }};
        metricsAspect.surroundWithACounter(pjp,timer);
        new Verifications(){{
            counter.inc();
            pjp.proceed();
            counter.dec();
        }};
    }

    @Test
    public void surroundWithACounterInPlugin(@Mocked ProceedingJoinPoint pjp,@Mocked  eu.domibus.ext.domain.metrics.Counter timer,@Mocked com.codahale.metrics.Counter counter) throws Throwable {
        String metricName = "MetricName";
        Class<String> testClass = String.class;
        new Expectations(){{
            timer.value();
            result= metricName;
            timer.clazz();
            result=testClass;
            com.codahale.metrics.Counter counter = metricRegistry.counter("java.lang.String." + metricName + ".counter");
            result=counter;
        }};
        metricsAspect.surroundWithACounter(pjp,timer);
        new Verifications(){{
            counter.inc();
            pjp.proceed();
            counter.dec();
        }};
    }




}
