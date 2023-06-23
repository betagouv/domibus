package eu.domibus.core.ebms3.receiver.policy;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.Messaging;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.receiver.leg.LegConfigurationExtractor;
import eu.domibus.core.ebms3.receiver.leg.ServerInMessageLegConfigurationFactory;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.message.SoapService;
import eu.domibus.core.message.TestMessageValidator;
import eu.domibus.core.message.UserMessageErrorCreator;
import eu.domibus.core.message.UserMessageHandlerService;
import eu.domibus.core.property.DomibusVersionService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

/**
 * @author Catalin Enache, Soumya Chandran
 * @since 4.2
 */
@SuppressWarnings("ConstantConditions")
@ExtendWith(JMockitExtension.class)
public class SetPolicyInServerInterceptorTest {

    @Tested
    SetPolicyInServerInterceptor setPolicyInServerInterceptor;

    @Injectable
    UserMessageHandlerService userMessageHandlerService;

    @Injectable
    SoapService soapService;

    @Injectable
    Ebms3Converter ebms3Converter;

    @Injectable
    protected PolicyService policyService;

    @Injectable
    protected DomibusVersionService domibusVersionService;

    @Injectable
    ServerInMessageLegConfigurationFactory serverInMessageLegConfigurationFactory;

    @Injectable
    UserMessageErrorCreator userMessageErrorCreator;

    @Injectable
    SecurityProfileService securityProfileService;

    @Test
    @Disabled("EDELIVERY-6896")
    public void logIncomingMessaging(final @Injectable SoapMessage soapMessage,
                                     final @Injectable TestMessageValidator testMessageValidator) throws Exception {

        //tested method
        EbMS3Exception ex = EbMS3ExceptionBuilder.getInstance()
                .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                .message("no valid security policy found")
                .refToMessageId("unknown")
                .cause(new NullPointerException())
                .mshRole(MSHRole.RECEIVING)
                .build();
        setPolicyInServerInterceptor.logIncomingMessagingException(soapMessage, ex);

        new Verifications() {{
            soapService.getMessagingAsRAWXml(soapMessage);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void handleMessage(@Injectable SoapMessage message,
                              @Injectable HttpServletResponse response,
                              final @Injectable TestMessageValidator testMessageValidator) throws JAXBException, IOException, EbMS3Exception {

        new Expectations(setPolicyInServerInterceptor) {
            {
                Ebms3Messaging ebms3Messaging = soapService.getMessage(message);
                LegConfigurationExtractor legConfigurationExtractor = serverInMessageLegConfigurationFactory.extractMessageConfiguration(message, ebms3Messaging);
                LegConfiguration legConfiguration = legConfigurationExtractor.extractMessageConfiguration();
                legConfiguration.getSecurity().getProfile();
                result = SecurityProfile.RSA;
                setPolicyInServerInterceptor.saveRawMessageMessageContext(message);
            }
        };

        setPolicyInServerInterceptor.handleMessage(message);

        new Verifications() {{
            soapService.getMessage(message);
            times = 1;
            policyService.parsePolicy("policies" + File.separator + anyString, SecurityProfile.RSA);
            times = 1;
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    void handleMessageThrowsIOException(@Injectable SoapMessage message,
                                        @Injectable HttpServletResponse response,
                                        final @Injectable TestMessageValidator testMessageValidator
    ) throws JAXBException, IOException, EbMS3Exception {


        new Expectations() {{
            soapService.getMessage(message);
            result = new IOException();

        }};

        Assertions.assertThrows(Fault.class, () -> setPolicyInServerInterceptor.handleMessage(message));

        new FullVerifications() {{
            soapService.getMessage(message);
            policyService.parsePolicy("policies" + File.separator + anyString, SecurityProfile.RSA);
            setPolicyInServerInterceptor.setBindingOperation(message);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    void handleMessageEbMS3Exception(@Injectable SoapMessage message,
                                     @Injectable HttpServletResponse response,
                                     @Injectable Messaging messaging,
                                     final @Injectable TestMessageValidator testMessageValidator) throws JAXBException, IOException, EbMS3Exception {

        new Expectations() {{
            soapService.getMessage(message);
            result = EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                    .message("no valid security policy found")
                    .build();
        }};
        Assertions.assertThrows(Fault.class, () -> setPolicyInServerInterceptor.handleMessage(message));

        new Verifications() {{
            soapService.getMessage(message);
            times = 1;
            policyService.parsePolicy("policies" + File.separator + anyString, SecurityProfile.RSA);
            times = 1;
            setPolicyInServerInterceptor.setBindingOperation(message);
            times = 1;
        }};
    }
}
