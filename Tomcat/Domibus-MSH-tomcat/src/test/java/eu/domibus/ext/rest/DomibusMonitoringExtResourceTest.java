package eu.domibus.ext.rest;

import eu.domibus.ext.domain.monitoring.MonitoringInfoDTO;
import eu.domibus.ext.exceptions.DomibusMonitoringExtException;
import eu.domibus.ext.rest.error.ExtExceptionHelper;
import eu.domibus.ext.services.DomibusMonitoringExtService;
import mockit.*;
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
public class DomibusMonitoringExtResourceTest {

    @Tested
    DomibusMonitoringExtResource domibusMonitoringExtResource;

    @Injectable
    DomibusMonitoringExtService domibusMonitoringExtService;

    @Injectable
    ExtExceptionHelper extExceptionHelper;

    @Test
    public void getDomibusStatusTest() throws DomibusMonitoringExtException {
        MonitoringInfoDTO monitoringInfoDTO = new MonitoringInfoDTO();
        List<String> filter = new ArrayList<>();
        filter.add("db");

        new Expectations() {{
            domibusMonitoringExtService.getMonitoringDetails(filter);
            result = monitoringInfoDTO;
        }};

        final MonitoringInfoDTO responseList = domibusMonitoringExtResource.getMonitoringDetails(filter);

        Assertions.assertNotNull(responseList);
    }

    @Test
    public void test_handleDomibusMonitoringExtException() {
        //tested method
        DomibusMonitoringExtException e = new DomibusMonitoringExtException(new Throwable());
        domibusMonitoringExtResource.handleDomibusMonitoringExtException(e);

        new FullVerifications() {{
            extExceptionHelper.handleExtException(e);
        }};
    }

}

