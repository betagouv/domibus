package eu.domibus.jms.weblogic;

import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@ExtendWith(JMockitExtension.class)
public class DomibusJMSWebLogicConfigurationTest {

    @Tested
    DomibusJMSWebLogicConfiguration domibusJMSWebLogicConfiguration;

    @Test
    @Disabled("EDELIVERY-6896")
    public void jmsSender(@Injectable ConnectionFactory connectionFactory,
                          @Mocked JmsTemplate jmsTemplate) {
        domibusJMSWebLogicConfiguration.jmsSender(connectionFactory);

        new Verifications() {{
            jmsTemplate.setSessionTransacted(true);
            jmsTemplate.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
            jmsTemplate.setConnectionFactory(connectionFactory);
        }};
    }
}
