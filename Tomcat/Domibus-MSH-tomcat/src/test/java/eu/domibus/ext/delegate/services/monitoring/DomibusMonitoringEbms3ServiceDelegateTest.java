package eu.domibus.ext.delegate.services.monitoring;

import eu.domibus.api.monitoring.DomibusMonitoringService;
import eu.domibus.api.monitoring.domain.DataBaseInfo;
import eu.domibus.api.monitoring.domain.JmsBrokerInfo;
import eu.domibus.api.monitoring.domain.MonitoringInfo;
import eu.domibus.api.monitoring.domain.MonitoringStatus;
import eu.domibus.ext.delegate.mapper.MonitoringExtMapper;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.DomibusMonitoringExtException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DomibusMonitoringEbms3ServiceDelegateTest {

    @Tested
    DomibusMonitoringServiceDelegate domibusMonitoringServiceDelegate;

    @Injectable
    DomibusMonitoringService domibusMonitoringService;

    @Injectable
    MonitoringExtMapper monitoringExtMapper;

    @Test
    public void getDomibusStatusTest() {
        DataBaseInfo dataBaseInfo = new DataBaseInfo();
        dataBaseInfo.setName("Database");
        dataBaseInfo.setStatus(MonitoringStatus.NORMAL);
        JmsBrokerInfo jmsBrokerInfo = new JmsBrokerInfo();
        jmsBrokerInfo.setName("JMS Broker");
        jmsBrokerInfo.setStatus(MonitoringStatus.NORMAL);
        MonitoringInfo monitoringInfo = new MonitoringInfo();
        List<String> filter = new ArrayList<>();
        filter.add("db");

        new Expectations() {{
            domibusMonitoringService.getMonitoringDetails(filter);
            result = monitoringInfo;
        }};

        // When
        domibusMonitoringServiceDelegate.getMonitoringDetails(filter);

        // Then
        new Verifications() {{
            domibusMonitoringService.getMonitoringDetails(filter);
            monitoringExtMapper.monitoringInfoToMonitoringInfoDTO(monitoringInfo);
        }};

    }

    @Test
    public void testDomibusMonitoringExtException() {
        // Given
        final DomibusMonitoringExtException domibusMonitoringExtException = new DomibusMonitoringExtException(DomibusErrorCode.DOM_001, "test");
        List<String> filter = new ArrayList<>();
        filter.add("db");
        new Expectations() {{
            domibusMonitoringService.getMonitoringDetails(filter);
            result = domibusMonitoringExtException;
        }};

        // When
        try {
            domibusMonitoringServiceDelegate.getMonitoringDetails(filter);
            Assertions.fail();
        } catch (DomibusMonitoringExtException e) {
            // Then
            Assertions.assertSame(domibusMonitoringExtException, e);
        }
    }
}
