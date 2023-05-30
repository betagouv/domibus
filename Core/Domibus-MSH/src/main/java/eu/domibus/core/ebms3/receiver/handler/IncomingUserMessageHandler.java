package eu.domibus.core.ebms3.receiver.handler;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.mf.Ebms3MessageFragmentType;
import eu.domibus.api.message.validation.UserMessageValidatorSpiService;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MSHRoleEntity;
import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.ws.attachment.AttachmentCleanupService;
import eu.domibus.core.message.UserMessagePayloadService;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.security.AuthorizationServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.List;

/**
 * Handles the incoming AS4 UserMessages
 *
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class IncomingUserMessageHandler extends AbstractIncomingMessageHandler {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(IncomingUserMessageHandler.class);

    @Autowired
    protected AttachmentCleanupService attachmentCleanupService;

    @Autowired
    protected AuthorizationServiceImpl authorizationService;

    @Autowired
    protected UserMessageValidatorSpiService userMessageValidatorSpiService;

    @Autowired
    protected MshRoleDao mshRoleDao;

    @Autowired
    protected UserMessagePayloadService userMessagePayloadService;

    @Override
    protected SOAPMessage processMessage(LegConfiguration legConfiguration, String pmodeKey, SOAPMessage request, Ebms3Messaging ebms3Messaging, boolean testMessage) throws EbMS3Exception, TransformerException, IOException, JAXBException, SOAPException {
        LOG.debug("Processing UserMessage");

        UserMessage userMessage = ebms3Converter.convertFromEbms3(ebms3Messaging.getUserMessage());

        MSHRole mshRole = MSHRole.RECEIVING;
        final MSHRoleEntity mshRoleEntity = mshRoleDao.findOrCreate(mshRole);
        userMessage.setMshRole(mshRoleEntity);

        LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, userMessage.getMessageId());
        LOG.putMDC(DomibusLogger.MDC_MESSAGE_ROLE, mshRole.name());

        Ebms3MessageFragmentType ebms3MessageFragmentType = messageUtil.getMessageFragment(request);
        List<PartInfo> partInfoList = userMessagePayloadService.handlePayloads(request, ebms3Messaging, ebms3MessageFragmentType);
        partInfoList.stream().forEach(partInfo -> partInfo.setUserMessage(userMessage));

        userMessageValidatorSpiService.validate(request, userMessage, partInfoList);

        if (ebms3MessageFragmentType != null) {
            userMessage.setMessageFragment(true);
        }

        SecurityProfile securityProfile = legConfiguration.getSecurity().getProfile();
        authorizationService.authorizeUserMessage(request, userMessage, securityProfile);
        final SOAPMessage response = userMessageHandlerService.handleNewUserMessage(legConfiguration, pmodeKey, request, userMessage, ebms3MessageFragmentType, partInfoList, testMessage);
        attachmentCleanupService.cleanAttachments(request);
        return response;
    }
}
