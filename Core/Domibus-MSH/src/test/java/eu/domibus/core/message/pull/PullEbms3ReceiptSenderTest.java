package eu.domibus.core.message.pull;

import eu.domibus.api.ebms3.model.Ebms3Error;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3SignalMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.SoapUtil;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.neethi.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * @author idragusa
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class PullEbms3ReceiptSenderTest {

    @Injectable
    private MSHDispatcher mshDispatcher;

    @Injectable
    protected MessageUtil messageUtil;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Tested
    private PullReceiptSender pullReceiptSender;

    @Autowired
    protected SoapUtil soapUtil;

    static MessageFactory messageFactory = null;

    @BeforeAll
    public static void init() throws SOAPException {
        messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
    }

    @Test
    public void sendReceiptOKTest(@Mocked SOAPMessage soapMessage, @Mocked String endpoint, @Mocked Policy policy, @Mocked LegConfiguration legConfiguration, @Mocked String pModeKey, @Mocked String messsageId, @Mocked String domainCode) throws EbMS3Exception {
        new Expectations() {{
            mshDispatcher.dispatch(soapMessage, endpoint, policy, legConfiguration, pModeKey);
            result = null; // expected response for a pull receipt is null
        }};

        pullReceiptSender.sendReceipt(soapMessage, endpoint, policy, legConfiguration, pModeKey, messsageId, domainCode);

        new Verifications() {{
            domainContextProvider.setCurrentDomain(domainCode);
            times = 1;
        }};
    }

    @Test
    void sendReceiptNotSignalTest(@Mocked SOAPMessage soapMessage, @Mocked String endpoint, @Mocked Policy policy, @Mocked LegConfiguration legConfiguration, @Mocked String pModeKey,
                                         @Mocked String domainCode) throws Exception {
        final String messsageId = "123123123123@domibus.eu";
        Ebms3SignalMessage signalMessage = new Ebms3SignalMessage();
        Ebms3Error error = new Ebms3Error();
        error.setErrorCode(ErrorCode.EBMS_0001.getErrorCodeName());
        error.setErrorDetail("Some details about the test error");
        error.setRefToMessageInError(messsageId);
        signalMessage.getError().add(error);
        Ebms3Messaging messaging = new Ebms3Messaging();
        messaging.setSignalMessage(signalMessage);

        new Expectations() {{
            mshDispatcher.dispatch(soapMessage, endpoint, policy, legConfiguration, pModeKey);
            result = messageFactory.createMessage();

            messageUtil.getMessage((SOAPMessage) any);
            result = messaging;
        }};

        Assertions.assertThrows(EbMS3Exception.class,
                () -> pullReceiptSender.sendReceipt(soapMessage, endpoint, policy, legConfiguration, pModeKey, messsageId, domainCode));

        new Verifications() {{
            domainContextProvider.setCurrentDomain(domainCode);
            times = 1;
        }};
    }

    @Test
    public void sendReceiptNullSignalTest(@Mocked SOAPMessage soapMessage, @Mocked String endpoint, @Mocked Policy policy, @Mocked LegConfiguration legConfiguration, @Mocked String pModeKey, @Mocked String messsageId,
                                          @Mocked String domainCode) throws Exception {
        Ebms3Messaging messaging = new Ebms3Messaging();
        messaging.setSignalMessage(null); // test it doesn't crash when null

        new Expectations() {{
            mshDispatcher.dispatch(soapMessage, endpoint, policy, legConfiguration, pModeKey);
            result = messageFactory.createMessage();

            messageUtil.getMessage((SOAPMessage) any);
            result = messaging;
        }};

        pullReceiptSender.sendReceipt(soapMessage, endpoint, policy, legConfiguration, pModeKey, messsageId, domainCode);

        new Verifications() {{
            domainContextProvider.setCurrentDomain(domainCode);
            times = 1;
        }};
    }

}
