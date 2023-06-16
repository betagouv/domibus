package eu.domibus.core.clustering;

import eu.domibus.api.cluster.CommandExecutorService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.messaging.MessageConstants;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class ControllerListenerServiceTest {

    @Tested
    private ControllerListenerService controllerListenerService;

    @Injectable
    private CommandExecutorService commandExecutorService;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Test
    public void testHandleMessageDomainWhenNoDomainWasProvided(@Mocked Message message) {
        boolean handled = controllerListenerService.handleMessageDomain(message);
        Assertions.assertTrue(handled);
        new Verifications() {{
            domainContextProvider.clearCurrentDomain();
        }};
    }

    @Test
    public void testHandleMessageDomainWhenADomainWasProvided(@Mocked Message message) throws JMSException {
        String domainCode = "domain1";
        Domain domain = new Domain(domainCode, domainCode);
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = domainCode;
            domainService.getDomain(domainCode);
            result = domain;
        }};
        boolean handled = controllerListenerService.handleMessageDomain(message);
        Assertions.assertTrue(handled);
        new Verifications() {{
            domainContextProvider.setCurrentDomainWithValidation(domainCode);
        }};
    }

    @Test
    public void testHandleMessageDomainWhenInvalidDomainWasProvided(@Mocked Message message) throws JMSException {
        String domainCode = "domain1";
        new Expectations() {{
            message.getStringProperty(MessageConstants.DOMAIN);
            result = domainCode;
            domainService.getDomain(domainCode);
            result = null;
        }};
        boolean handled = controllerListenerService.handleMessageDomain(message);
        Assertions.assertFalse(handled);
        new Verifications() {{
            domainContextProvider.setCurrentDomain(domainCode); times=0;
        }};
    }

}
