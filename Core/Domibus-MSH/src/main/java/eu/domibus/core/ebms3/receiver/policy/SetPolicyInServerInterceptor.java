package eu.domibus.core.ebms3.receiver.policy;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Security;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.receiver.interceptor.CheckEBMSHeaderInterceptor;
import eu.domibus.core.ebms3.receiver.interceptor.SOAPMessageBuilderInterceptor;
import eu.domibus.core.ebms3.receiver.leg.LegConfigurationExtractor;
import eu.domibus.core.ebms3.receiver.leg.ServerInMessageLegConfigurationFactory;
import eu.domibus.core.ebms3.sender.client.DispatchClientDefaultProvider;
import eu.domibus.core.message.TestMessageValidator;
import eu.domibus.core.message.UserMessageErrorCreator;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.converters.ConverterException;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static eu.domibus.api.exceptions.DomibusCoreErrorCode.DOM_006;
import static eu.domibus.messaging.MessageConstants.RAW_MESSAGE_XML;


/**
 * @author Thomas Dussart
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class SetPolicyInServerInterceptor extends SetPolicyInInterceptor {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SetPolicyInServerInterceptor.class);

    protected final ServerInMessageLegConfigurationFactory serverInMessageLegConfigurationFactory;

    protected final TestMessageValidator testMessageValidator;

    protected final Ebms3Converter ebms3Converter;

    protected final UserMessageErrorCreator userMessageErrorCreator;

    protected final SecurityProfileService securityProfileService;

    public SetPolicyInServerInterceptor(ServerInMessageLegConfigurationFactory serverInMessageLegConfigurationFactory,
                                        TestMessageValidator testMessageValidator, Ebms3Converter ebms3Converter,
                                        UserMessageErrorCreator userMessageErrorCreator,
                                        SecurityProfileService securityProfileService) {
        this.serverInMessageLegConfigurationFactory = serverInMessageLegConfigurationFactory;
        this.testMessageValidator = testMessageValidator;
        this.ebms3Converter = ebms3Converter;
        this.userMessageErrorCreator = userMessageErrorCreator;
        this.securityProfileService = securityProfileService;
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        String policyName = null;
        String messageId = null;
        LegConfiguration legConfiguration;
        Ebms3Messaging ebms3Messaging = null;

        try {
            ebms3Messaging = soapService.getMessage(message);
            message.getExchange().put(MessageConstants.EMBS3_MESSAGING_OBJECT, ebms3Messaging);
            message.put(DispatchClientDefaultProvider.MESSAGING_KEY_CONTEXT_PROPERTY, ebms3Messaging);

            LegConfigurationExtractor legConfigurationExtractor = serverInMessageLegConfigurationFactory.extractMessageConfiguration(message, ebms3Messaging);
            if (legConfigurationExtractor == null) return;

            legConfiguration = legConfigurationExtractor.extractMessageConfiguration();
            policyName = legConfiguration.getSecurity().getPolicy();
            Policy policy = policyService.parsePolicy("policies" + File.separator + policyName, legConfiguration.getSecurity().getProfile());

            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_POLICY_INCOMING_USE, policyName);

            message.getExchange().put(PolicyConstants.POLICY_OVERRIDE, policy);
            message.put(PolicyConstants.POLICY_OVERRIDE, policy);
            message.getInterceptorChain().add(new CheckEBMSHeaderInterceptor());
            message.getInterceptorChain().add(new SOAPMessageBuilderInterceptor());

            Security security = legConfiguration.getSecurity();
            String securityAlgorithm = securityProfileService.getSecurityAlgorithm(security.getPolicy(), security.getProfile(), legConfiguration.getName());

            message.put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
            message.getExchange().put(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM, securityAlgorithm);
            LOG.businessInfo(DomibusMessageCode.BUS_SECURITY_ALGORITHM_INCOMING_USE, securityAlgorithm);

            saveRawMessageMessageContext(message);

        } catch (EbMS3Exception ex) {
            setBindingOperation(message);
            LOG.debug("", ex); // Those errors are expected (no PMode found, therefore DEBUG)
            logIncomingMessagingException(message, ex);
            throw new Fault(ex);
        } catch (IOException | JAXBException e) {
            setBindingOperation(message);
            LOG.businessError(DomibusMessageCode.BUS_SECURITY_POLICY_INCOMING_NOT_FOUND, e, policyName); // Those errors are not expected
            throw new Fault(EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                    .message("no valid security policy found")
                    .refToMessageId(ebms3Messaging != null ? messageId : "unknown")
                    .cause(e)
                    .mshRole(MSHRole.RECEIVING)
                    .build());
        }
    }

    protected void saveRawMessageMessageContext(SoapMessage message) throws IOException {
        final InputStream inputStream = message.getContent(InputStream.class);
        if (inputStream instanceof ByteArrayInputStream) {
            LOG.trace("Saving the raw message envelope content (to have the encrypted data section)");
            String rawXMLMessage = IOUtils.toString(inputStream, "UTF-8");
            ((ByteArrayInputStream) inputStream).reset();

            message.getExchange().put(RAW_MESSAGE_XML, rawXMLMessage);
        } else {
            throw new DomibusCoreException(DOM_006, "Could not get the message content since it is not a byteArray stream.");
        }
    }

    protected void logIncomingMessagingException(SoapMessage message, EbMS3Exception ex) {
        if (message == null) {
            LOG.debug("SoapMessage is null");
            return;
        }
        try {
            String xml = soapService.getMessagingAsRAWXml(message);
            LOG.error("EbMS3Exception [{}] caused by incoming message: [{}]", ex, xml);
        } catch (ConverterException | IOException | EbMS3Exception | TransformerException e) {
            LOG.error("Error while getting Soap Envelope", e);
        }
    }

}
