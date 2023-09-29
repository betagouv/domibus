package eu.domibus.core.util;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.messaging.MessageConstants;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.jms.Message;
import java.util.Random;
import java.util.UUID;

/**
 * @author François Gautier
 * @since 5.0
 */
public class JmsUtilTest {

    public static final String DEFAULT = "default";
    @Tested
    protected JmsUtil jmsUtil;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    private Message message;
    private String valueString;
    private Long valueLong;

    @BeforeEach
    public void setUp() throws Exception {
        valueString = UUID.randomUUID().toString();
        valueLong = new Random().nextLong();
        message = new ActiveMQTextMessage();
        message.setStringProperty("Long", "" + valueLong);
        message.setStringProperty("NotALong", "");
        message.setStringProperty("String", valueString);
        message.setStringProperty(MessageConstants.DOMAIN, DEFAULT);
    }

    @Test
    public void getStringProperty() {
        String result = jmsUtil.getStringPropertySafely(message, "String");
        Assertions.assertEquals(valueString, result);
    }

    @Test
    public void getStringProperty_notFound() {
        String result = jmsUtil.getStringPropertySafely(message, "NotFound");
        Assertions.assertNull(result);
    }

    @Test
    public void getLongProperty() {
        Long result = jmsUtil.getLongPropertySafely(message, "Long");
        Assertions.assertEquals(valueLong, result);
    }

    @Test
    public void getLongProperty_notFound() {
        Long result = jmsUtil.getLongPropertySafely(message, "NotFound");
        Assertions.assertNull(result);
    }

    @Test
    public void getLongProperty_notALong() {
        Long result = jmsUtil.getLongPropertySafely(message, "NotALong");
        Assertions.assertNull(result);
    }

    @Test
    public void SetDomain() {
        jmsUtil.setCurrentDomainFromMessage(message);

        new FullVerifications(){{
            domainContextProvider.setCurrentDomain(DEFAULT);
        }};
    }

    @Test
    public void SetDomain_noDomain() {
        jmsUtil.setCurrentDomainFromMessage(new ActiveMQTextMessage());

        new FullVerifications(){{
            domainContextProvider.clearCurrentDomain();
        }};
    }
}
