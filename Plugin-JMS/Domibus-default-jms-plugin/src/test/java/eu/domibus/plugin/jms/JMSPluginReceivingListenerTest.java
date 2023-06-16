package eu.domibus.plugin.jms;

import eu.domibus.ext.services.AuthenticationExtService;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomibusPropertyExtService;
import eu.domibus.logging.DomibusLogger;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class JMSPluginReceivingListenerTest {

    @Injectable
    protected JMSPluginImpl backendJMS;

    @Injectable
    protected DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    protected DomainContextExtService domainContextExtService;

    @Injectable
    protected AuthenticationExtService authenticationExtService;

    @Tested
    JMSPluginReceivingListener JMSPluginReceivingListener;

    @Test
    public void receiveMessage(@Injectable MapMessage map, @Mocked DomibusLogger LOG) {
        new Expectations(JMSPluginReceivingListener) {{
            authenticationExtService.isUnsecureLoginAllowed();
            result = false;

            JMSPluginReceivingListener.authenticate(map);
        }};
        JMSPluginReceivingListener.receiveMessage(map);

        new VerificationsInOrder() {{
            LOG.clearCustomKeys();
            JMSPluginReceivingListener.authenticate(map);
            backendJMS.receiveMessage(map);
        }};
    }

    @Test
    public void authenticate(@Injectable MapMessage map, @Mocked DomibusLogger LOG) throws JMSException {
        String username = "cosmin";
        String password = "mypass";
        new Expectations() {{
            map.getStringProperty(JMSMessageConstants.USERNAME);
            result = username;

            map.getStringProperty(JMSMessageConstants.PASSWORD);
            result = password;
        }};

        JMSPluginReceivingListener.authenticate(map);

        new FullVerifications() {{
            authenticationExtService.basicAuthenticate(username, password);
        }};
    }

    @Test
    void authenticateWithMissingUsername(@Injectable MapMessage map, @Mocked DomibusLogger LOG) throws JMSException {
        new Expectations() {{
            map.getStringProperty(JMSMessageConstants.USERNAME);
            result = null;
            map.getStringProperty(JMSMessageConstants.PASSWORD);
            result = "null";
        }};

        Assertions.assertThrows(DefaultJmsPluginException.class, () -> JMSPluginReceivingListener.authenticate(map));

        new Verifications() {{
            authenticationExtService.basicAuthenticate(anyString, anyString);
            times = 0;
        }};
    }

    @Test
    void authenticateWithMissingPassword(@Injectable MapMessage map, @Mocked DomibusLogger LOG) throws JMSException {
        String username = "cosmin";
        String password = null;
        new Expectations() {{
            map.getStringProperty(JMSMessageConstants.USERNAME);
            result = username;

            map.getStringProperty(JMSMessageConstants.PASSWORD);
            result = password;
        }};

        Assertions.assertThrows(DefaultJmsPluginException.class, () -> JMSPluginReceivingListener.authenticate(map));

        new Verifications() {{
            authenticationExtService.basicAuthenticate(anyString, anyString);
            times = 0;
        }};
    }
}
