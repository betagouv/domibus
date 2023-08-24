package eu.domibus.jms.weblogic;

import eu.domibus.jms.spi.helper.PriorityJmsTemplate;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

@ExtendWith(JMockitExtension.class)
public class DomibusJMSWebLogicConfigurationTest {

    @Tested
    DomibusJMSWebLogicConfiguration domibusJMSWebLogicConfiguration;

    @Test
    public void jmsSender(@Injectable ConnectionFactory connectionFactory,
                          @Mocked PriorityJmsTemplate jmsTemplate) {
        domibusJMSWebLogicConfiguration.jmsSender(connectionFactory);

        new Verifications() {{
            jmsTemplate.setSessionTransacted(true);
            jmsTemplate.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
            jmsTemplate.setConnectionFactory(connectionFactory);
        }};
    }
}
