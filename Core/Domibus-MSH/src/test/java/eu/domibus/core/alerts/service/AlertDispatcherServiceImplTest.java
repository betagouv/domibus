package eu.domibus.core.alerts.service;

import eu.domibus.core.alerts.dao.AlertDao;
import eu.domibus.core.alerts.model.common.AlertStatus;
import eu.domibus.core.alerts.model.service.Alert;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class AlertDispatcherServiceImplTest {

    @Tested
    private AlertDispatcherServiceImpl alertDispatcherService;

    @Injectable
    private AlertService alertService;

    @Injectable
    private AlertDao alertDao;

    @Injectable
    protected AlertMethodFactory alertMethodFactory;

    @Test
    public void dispatch(@Mocked final Alert alert) {

        alertDispatcherService.dispatch(alert);

        new VerificationsInOrder(){{
            alert.setAlertStatus(AlertStatus.FAILED);
            times=1;

            alertMethodFactory.getAlertMethod().sendAlert(alert);
            times=1;

            alert.setAlertStatus(AlertStatus.SUCCESS);times=1;
            alertService.handleAlertStatus(alert);times=1;
        }};
    }

    @Test
    void dispatchWithError(@Mocked final Alert alert) {
        new Expectations() {{
            alertMethodFactory.getAlertMethod().sendAlert(alert);
            result = new RuntimeException("Error sending alert");
        }};
        Assertions.assertThrows(RuntimeException. class,() -> alertDispatcherService.dispatch(alert));

        new VerificationsInOrder(){{
            alert.setAlertStatus(AlertStatus.FAILED);
            times=1;

            alertMethodFactory.getAlertMethod().sendAlert(alert);
            times=1;

            alert.setAlertStatus(AlertStatus.SUCCESS);times=0;
            alertService.handleAlertStatus(alert);times=1;
        }};
    }
    
    @Test
    public void dispatchMissingAlert(@Mocked final Alert alert) {
        new Expectations() {{
            alert.getAttempts();
            result = 0;
            alert.getMaxAttempts();
            result = 5;
            alertDao.read(anyLong);
            result = null;
        }};
        alertDispatcherService.dispatch(alert);

        new Verifications() {{
            alertService.enqueueAlert(alert); times = 1;
            alertService.handleAlertStatus(alert); times = 0;
        }};
    }

}
