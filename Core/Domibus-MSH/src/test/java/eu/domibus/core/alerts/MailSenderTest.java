package eu.domibus.core.alerts;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.model.service.MailModel;
import eu.domibus.core.alerts.configuration.common.AlertConfigurationService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class MailSenderTest {

    @Tested
    private MailSender mailSender;

    @Injectable
    private Configuration freemarkerConfig;

    @Injectable
    private JavaMailSenderImpl javaMailSender;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected DomainContextProvider domainProvider;

    @Injectable
    private AlertConfigurationService alertConfigurationService;

    final String smtpUrl = "smtpUrl";
    final Integer port = 25;
    final String user = "user";
    final String password = "password";
    final String dynamicPropertyName = "domibus.alert.mail.smtp.port";
    final String dynamicSmtpPort = "450";
    final String timeoutPropertyName = DOMIBUS_ALERT_MAIL_SMTP_TIMEOUT;
    final int timeout = 5000;
    Set<String> dynamicPropertySet = new HashSet<>();

    @Test
    public void initMailSender_disabled1() {
        new Expectations(mailSender) {{

            mailSender.isAlertModuleEnabled();
            result = false;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MAIL_SENDING_ACTIVE);
            result = true;
        }};

        mailSender.initMailSender();

        new FullVerifications() {
        };
    }

    @Test
    public void initMailSender_disabled2() {

        new Expectations(mailSender) {{

            mailSender.isAlertModuleEnabled();
            result = true;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MAIL_SENDING_ACTIVE);
            result = false;

        }};

        mailSender.initMailSender();

        new FullVerifications() {
        };
    }

    @Test
    public void initMailSender_enabled() {
        Properties javaMailProperties = new Properties();

        dynamicPropertySet.add(dynamicPropertyName);
        dynamicPropertySet.add(timeoutPropertyName);

        new Expectations(mailSender) {{

            mailSender.isAlertModuleEnabled();
            result = true;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_MAIL_SENDING_ACTIVE);
            result = true;


            domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_MAIL_SMTP_TIMEOUT);
            result = timeout;

            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_URL);
            result = smtpUrl;

            domibusPropertyProvider.getIntegerProperty(DOMIBUS_ALERT_SENDER_SMTP_PORT);
            result = port;

            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_USER);
            result = user;

            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_PASSWORD);
            result = password;

            javaMailSender.getJavaMailProperties();
            result = javaMailProperties;

            domibusPropertyProvider.filterPropertyNames((Predicate<String>) any);
            result = dynamicPropertySet;

            domibusPropertyProvider.getProperty("domibus.alert.mail.smtp.timeout");
            result = timeout;

            domibusPropertyProvider.getProperty("domibus.alert.mail.smtp.port");
            result = dynamicSmtpPort;
        }};

        mailSender.initMailSender();

        new Verifications() {{
            javaMailSender.setHost(smtpUrl);
            times = 1;
            javaMailSender.setPort(port);
            times = 1;
            javaMailSender.setUsername(user);
            times = 1;
            javaMailSender.setPassword(password);
            times = 1;

            javaMailProperties.put("mail.smtp.timeout", Integer.toString(timeout));
            javaMailProperties.put("mail.smtp.port", dynamicSmtpPort);
        }};
    }

    @Test
    public void sendMailIllegalAddresses_to(@Injectable MailModel<Map<String, String>> model) {

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> mailSender.sendMail(model, "", "   "));

        new FullVerifications() {
        };
    }

    @Test
    public void sendMailIllegalAddresses_from(@Injectable MailModel<Map<String, String>> model) {

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> mailSender.sendMail(model, "", "test"));

        new FullVerifications() {
        };
    }

    @Test
    void sendMail_DomibusPropertyException(@Injectable MailModel<Map<String, String>> model) {

        new Expectations(mailSender) {{
            mailSender.initMailSender();
            times = 1;
            result = new Exception("TEST");
        }};
        Assertions.assertThrows(DomibusPropertyException.class,
                () -> mailSender.sendMail(model, "from", "to"));

        new FullVerifications() {
        };
    }

    @Test
    void sendMail_AlertDispatchException(
            @Injectable MailModel<Map<String, String>> model,
            @Injectable MimeMessage mimeMessage) throws MessagingException {

        new Expectations(mailSender) {{
            mailSender.initMailSender();
            times = 1;

            javaMailSender.createMimeMessage();
            result = mimeMessage;

            mailSender.getMimeMessageHelper(mimeMessage);
            result = new IOException("TEST");
        }};

        Assertions.assertThrows(AlertDispatchException.class,
                () -> mailSender.sendMail(model, "from", "to"));

        new FullVerifications() {
        };
    }

    @Disabled
    @SuppressWarnings("AccessStaticViaInstance")
    @Test
    public void sendMail_oneRecipient(@Injectable final Properties javaMailProperties,
                                      @Injectable MailModel<Map<String, String>> model,
                                      @Injectable MimeMessage mimeMessage,
                                      @Injectable MimeMessageHelper mimeMessageHelper,
                                      @Injectable Template template,
                                      @Injectable FreeMarkerTemplateUtils freeMarkerTemplateUtils) throws IOException, TemplateException, MessagingException {
        String to = "to";
        String from = "from";
        String html = "html";

        Object model1 = new Object();

        new Expectations(mailSender) {{
            mailSender.initMailSender();
            times = 1;

            javaMailSender.createMimeMessage();
            result = mimeMessage;

            mailSender.getMimeMessageHelper(mimeMessage);
            result = mimeMessageHelper;

            freemarkerConfig.getTemplate(model.getTemplatePath());
            result = template;

            model.getModel();
            result = model1;

            freeMarkerTemplateUtils.processTemplateIntoString(template, model1);
            result = html;

            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setText(html, true);
            mimeMessageHelper.setSubject(model.getSubject());
            mimeMessageHelper.setFrom(from);

            javaMailSender.send(mimeMessage);
            times = 1;
        }};
        mailSender.sendMail(model, from, to);

        new FullVerifications() {
        };
    }

    @Disabled
    @SuppressWarnings("AccessStaticViaInstance")
    @Test
    public void sendMail_multipleRecipients(@Injectable final Properties javaMailProperties,
                                            @Injectable MailModel<Map<String, String>> model,
                                            @Injectable MimeMessage mimeMessage,
                                            @Injectable MimeMessageHelper mimeMessageHelper,
                                            @Injectable Template template,
                                            @Injectable FreeMarkerTemplateUtils freeMarkerTemplateUtils) throws MessagingException, IOException, TemplateException {
        String to = "to;to";
        String from = "from";
        String html = "html";

        Object model1 = new Object();

        new Expectations(mailSender) {{
            mailSender.initMailSender();
            times = 1;

            javaMailSender.createMimeMessage();
            result = mimeMessage;

            mailSender.getMimeMessageHelper(mimeMessage);
            result = mimeMessageHelper;

            freemarkerConfig.getTemplate(model.getTemplatePath());
            result = template;

            model.getModel();
            result = model1;

            freeMarkerTemplateUtils.processTemplateIntoString(template, model1);
            result = html;

            mimeMessageHelper.setBcc(to.split(";"));
            mimeMessageHelper.setText(html, true);
            mimeMessageHelper.setSubject(model.getSubject());
            mimeMessageHelper.setFrom(from);

            javaMailSender.send(mimeMessage);
            times = 1;
        }};
        mailSender.sendMail(model, from, to);

        new FullVerifications() {
        };
    }

    @Test
    public void validateMandatoryProperties_ok() {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_URL);
            result = "url";
        }};
        mailSender.getMandatoryUrl();
    }

    @Test
    void validateMandatoryProperties_nok_url() {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_URL);
            result = "";
        }};

        Assertions.assertThrows(IllegalStateException.class,
                () -> mailSender.getMandatoryUrl());

        new FullVerifications() {
        };
    }

    @Test
    void validateMandatoryProperties_nok_url2() {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_ALERT_SENDER_SMTP_URL);
            result = null;
        }};
        Assertions.assertThrows(IllegalStateException.class,
                () -> mailSender.getMandatoryUrl());
        new FullVerifications() {
        };
    }

    @Test
    public void reset() {

        ReflectionTestUtils.setField(mailSender, "mailSenderInitiated", true);
        Assertions.assertTrue((Boolean) ReflectionTestUtils.getField(mailSender, "mailSenderInitiated"));

        mailSender.reset();

        Assertions.assertFalse((Boolean) ReflectionTestUtils.getField(mailSender, "mailSenderInitiated"));
    }
}
