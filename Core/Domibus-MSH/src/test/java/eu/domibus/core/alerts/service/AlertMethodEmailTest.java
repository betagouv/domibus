package eu.domibus.core.alerts.service;

import eu.domibus.core.alerts.configuration.global.CommonConfigurationManager;
import eu.domibus.core.user.ui.UserDao;
import eu.domibus.core.user.ui.User;
import eu.domibus.core.alerts.MailSender;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.model.service.MailModel;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Set;

@ExtendWith(JMockitExtension.class)
public class AlertMethodEmailTest {

    @Tested
    AlertMethodEmail alertMethodEmail;

    @Injectable
    private AlertService alertService;

    @Injectable
    private MailSender mailSender;

    @Injectable
    private CommonConfigurationManager alertConfigurationManager;

    @Injectable
    private UserDao userDao;

    @Test
    public void sendAlertNoUser() {
        Event event = new Event() {{
            setType(EventType.USER_LOGIN_FAILURE);
        }};
        Alert alert = setUp(event);

        alertMethodEmail.sendAlert(alert);

        new VerificationsInOrder() {{
            MailModel mailModel;
            mailSender.sendMail(mailModel = withCapture(), "office@domibus.eu", "peppol@peppol.eu");
            times = 1;

            userDao.loadUserByUsername(anyString);
            times = 0;
        }};
    }

    @Test
    public void sendAlertUserNotFound() {
        User user = new User() {{
            setUserName("admin");
            setEmail("admin@domibus.eu");
        }};
        Event event = new Event() {{
            setType(EventType.USER_LOGIN_FAILURE);
            addStringKeyValue("USER", "admin");
        }};
        Alert alert = setUp(event);

        new Expectations() {{
            userDao.loadUserByUsername("admin");
            result = null;
        }};

        alertMethodEmail.sendAlert(alert);

        new VerificationsInOrder() {{
            userDao.loadUserByUsername(anyString);
            times = 1;

            mailSender.sendMail((MailModel)any, "office@domibus.eu", "admin@domibus.eu");
            times = 0;
        }};
    }

    @Test
    public void sendAlert() {
        User user = new User() {{
            setUserName("admin");
            setEmail("admin@domibus.eu");
        }};
        Event event = new Event() {{
            setType(EventType.USER_LOGIN_FAILURE);
            addStringKeyValue("USER", "admin");
        }};
        Alert alert = setUp(event);

        new Expectations() {{
            userDao.loadUserByUsername("admin");
            result = user;
        }};

        alertMethodEmail.sendAlert(alert);

        new VerificationsInOrder() {{
            mailSender.sendMail((MailModel)any, "office@domibus.eu", "admin@domibus.eu");
            times = 1;
        }};
    }

    private Alert setUp(Event event) {

        Set<Event> events = new HashSet<Event>();
        events.add(event);

        Alert alert = new Alert() {{
            setEvents(events);
            setAlertType(AlertType.USER_LOGIN_FAILURE);
        }};

        new Expectations() {{
            alertConfigurationManager.getConfiguration().getSendFrom();
            result = "office@domibus.eu";
            alertConfigurationManager.getConfiguration().getSendTo();
            result = "peppol@peppol.eu";

        }};
        return alert;
    }
}
