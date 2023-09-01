package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.model.Property;
import eu.domibus.api.model.*;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.api.pmode.PModeService;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.configuration.*;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.message.MessageExchangeConfiguration;
import eu.domibus.core.message.dictionary.PartyIdDictionaryService;
import eu.domibus.core.message.dictionary.PartyRoleDictionaryService;
import eu.domibus.core.pmode.provider.CachingPModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.ProcessingType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.naming.InvalidNameException;
import java.security.cert.X509Certificate;
import java.util.*;

import static eu.domibus.api.cache.DomibusLocalCacheService.DYNAMIC_DISCOVERY_ENDPOINT;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/* This class is used for dynamic discovery of the parties participating in a message exchange.
 *
 * Dynamic discovery is activated when the pMode is configured with a dynamic
 * process (PMode.Initiator is not set and/or PMode.Responder is not set)
 *
 * The receiver of the message must be able to accept messages from previously unknown senders.
 * This requires the receiver to have one or more P-Modes configured for all registrations ii has in the SMP.
 * Therefore for each SMP Endpoint registration of the receiver with the type attribute set to 'bdxr-transport-ebms3-as4-v1p0'
 * there MUST exist a P-Mode that can handle a message with the following attributes:
 *      Service = ancestor::ServiceMetadata/ServiceInformation/Processlist/Process/ProcessIdentifier
 *      Service/@type = ancestor::ServiceMetadata/ServiceInformation/Processlist/Process/ProcessIdentifier/@scheme
 *      Action = ancestor::ServiceMetadata/ServiceInformation/DocumentIdentifier
 *
 * The sender must be able to send messages to unknown receivers. This requires that the sender performs a lookup to find
 * out the receivers details (partyId, type, endpoint address, public certificate - to encrypt the message).
 *
 * The sender may not register, it can send a message to a registered receiver even if he (the sender) is not registered.
 * Therefore, on the receiver there is no lookup for the sender. The message is accepted based on the root CA as long as the process matches.
 */

public class DynamicDiscoveryPModeProvider extends CachingPModeProvider {

    private static final String DYNAMIC_DISCOVERY_CLIENT_SPECIFICATION = DOMIBUS_DYNAMICDISCOVERY_CLIENT_SPECIFICATION;

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DynamicDiscoveryPModeProvider.class);

    @Autowired
    protected MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    protected DomainContextProvider domainProvider;

    @Autowired
    @Qualifier("dynamicDiscoveryServiceOASIS")
    private DynamicDiscoveryService dynamicDiscoveryServiceOASIS;

    @Autowired
    @Qualifier("dynamicDiscoveryServicePEPPOL")
    private DynamicDiscoveryService dynamicDiscoveryServicePEPPOL;

    protected DynamicDiscoveryService dynamicDiscoveryService = null;

    @Autowired
    protected CertificateService certificateService;

    @Autowired
    protected PartyRoleDictionaryService partyRoleDictionaryService;

    @Autowired
    protected PartyIdDictionaryService partyIdDictionaryService;

    @Autowired
    protected DomibusLocalCacheService domibusLocalCacheService;

    @Autowired
    protected SecurityProfileService securityProfileService;

    @Autowired
    protected PModeService pModeService;

    protected Collection<eu.domibus.common.model.configuration.Process> dynamicResponderProcesses;
    protected Collection<eu.domibus.common.model.configuration.Process> dynamicInitiatorProcesses;
    protected Map<String, PartyId> cachedToPartyId = new HashMap<>();

    // default type in eDelivery profile
    protected static final String URN_TYPE_VALUE = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";
    protected static final String DEFAULT_RESPONDER_ROLE = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder";
    protected static final String MSH_ENDPOINT = "msh_endpoint";

    public DynamicDiscoveryPModeProvider(Domain domain) {
        super(domain);
    }

    @Override
    public void init() {
        load();
    }

    @Override
    protected void load() {
        super.load();

        LOG.debug("Initialising the dynamic discovery configuration.");
        cachedToPartyId.clear();
        dynamicResponderProcesses = findDynamicResponderProcesses();
        dynamicInitiatorProcesses = findDynamicSenderProcesses();
        if (DynamicDiscoveryClientSpecification.PEPPOL.getName().equalsIgnoreCase(domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_CLIENT_SPECIFICATION))) {
            dynamicDiscoveryService = dynamicDiscoveryServicePEPPOL;
        } else { // OASIS client is used by default
            dynamicDiscoveryService = dynamicDiscoveryServiceOASIS;
        }
    }

    public Collection<eu.domibus.common.model.configuration.Process> getDynamicProcesses(final MSHRole mshRole) {
        // TODO investigate why the configuration is empty when these lists are initialized in the first place
        if (CollectionUtils.isEmpty(dynamicResponderProcesses) && CollectionUtils.isEmpty(dynamicInitiatorProcesses)) {
            // this is needed when the processes were not initialized in the init()
            LOG.debug("Refreshing the configuration.");
            refresh();
        }

        return MSHRole.SENDING.equals(mshRole) ? dynamicResponderProcesses : dynamicInitiatorProcesses;
    }

    protected Collection<eu.domibus.common.model.configuration.Process> findDynamicResponderProcesses() {
        final Collection<eu.domibus.common.model.configuration.Process> result = new ArrayList<>();
        for (final eu.domibus.common.model.configuration.Process process : this.getConfiguration().getBusinessProcesses().getProcesses()) {
            if (process.isDynamicResponder() && (process.isDynamicInitiator() || process.getInitiatorParties().contains(getConfiguration().getParty()))) {
                if (!process.getInitiatorParties().contains(getConfiguration().getParty())) {
                    throw new ConfigurationException(process + " does not contain self party " + getConfiguration().getParty() + " as an initiator party.");
                }
                LOG.debug("Found dynamic receiver process: " + process.getName());
                result.add(process);
            }
        }
        return result;
    }

    protected Collection<eu.domibus.common.model.configuration.Process> findDynamicSenderProcesses() {
        final Collection<eu.domibus.common.model.configuration.Process> result = new ArrayList<>();
        for (final eu.domibus.common.model.configuration.Process process : this.getConfiguration().getBusinessProcesses().getProcesses()) {
            if (process.isDynamicInitiator() && (process.isDynamicResponder() || process.getResponderParties().contains(getConfiguration().getParty()))) {
                if (!process.getResponderParties().contains(getConfiguration().getParty())) {
                    throw new ConfigurationException(process + " does not contain self party " + getConfiguration().getParty() + " as a responder party.");
                }
                LOG.debug("Found dynamic sender process: " + process.getName());
                result.add(process);
            }
        }
        return result;
    }

    /**
     * Method validates if dynamic discovery is enabled for current domain.
     *
     * @return true if domibus.dynamicdiscovery.useDynamicDiscovery is enabled for the current domain.
     */
    protected boolean useDynamicDiscovery() {
        return domibusPropertyProvider.getBooleanProperty(DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY);
    }

    /* Method finds MessageExchangeConfiguration for given user mesage and role. If property domibus.smlzone
     * is not defined only static search is done else (if static search did not return result) also dynamic discovery is executed.
     */
    @Override
    public MessageExchangeConfiguration findUserMessageExchangeContext(final UserMessage userMessage, final MSHRole mshRole, final boolean isPull, ProcessingType processingType) throws EbMS3Exception {
        try {
            return super.findUserMessageExchangeContext(userMessage, mshRole, isPull, processingType, true);
        } catch (final EbMS3Exception e) {
            if (useDynamicDiscovery()) {
                LOG.info("PmodeKey not found, starting the dynamic discovery process");
                doDynamicDiscovery(userMessage, mshRole);
            } else {
                LOG.debug("PmodeKey not found, dynamic discovery is not enabled! Check parameter [{}] for current domain.", DOMIBUS_SMLZONE);
                throw e;
            }
        }
        LOG.debug("Recalling findUserMessageExchangeContext after the dynamic discovery");
        return super.findUserMessageExchangeContext(userMessage, mshRole, isPull, processingType, false);
    }

    protected void doDynamicDiscovery(final UserMessage userMessage, final MSHRole mshRole) throws EbMS3Exception {
        Collection<eu.domibus.common.model.configuration.Process> candidates = findCandidateProcesses(userMessage, mshRole);

        if (candidates == null || candidates.isEmpty()) {
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                    .message("No matching dynamic discovery processes found for message.")
                    .refToMessageId(userMessage.getMessageId())
                    .build();
        }

        LOG.info("Found [{}] dynamic discovery candidates. MSHRole: [{}]", candidates.size(), mshRole);

        if (MSHRole.RECEIVING.equals(mshRole)) {
            PartyId fromPartyId = getFromPartyId(userMessage);
            Party configurationParty = updateConfigurationParty(fromPartyId.getValue(), fromPartyId.getType(), null);
            updateInitiatorPartiesInPmode(candidates, configurationParty);

        } else {//MSHRole.SENDING

            String cacheKey = getCacheKeyForDynamicDiscovery(userMessage);
            PartyId partyId = cachedToPartyId.get(cacheKey);
            if (partyId != null && domibusLocalCacheService.containsCacheForKey(cacheKey, DYNAMIC_DISCOVERY_ENDPOINT)){
                LOG.debug("Skip ddc lookup and add to UserMessage 'To Party' the cached PartyID object for the key [{}]", cacheKey);
                userMessage.getPartyInfo().getTo().setToPartyId(partyId);
                if (userMessage.getPartyInfo().getTo().getToRole() == null) {
                    String responderRoleValue = dynamicDiscoveryService.getResponderRole();
                    PartyRole partyRole = partyRoleDictionaryService.findOrCreateRole(responderRoleValue);
                    userMessage.getPartyInfo().getTo().setToRole(partyRole);
                }
            } else {
                // do the lookup
                lookupAndUpdateConfigurationForToPartyId(cacheKey, userMessage, candidates);
            }
        }
    }

    /**
     * Method lookups and updates pmode configuration and truststore
     *
     * @param cacheKey    cached key matches the key for lookup data
     * @param userMessage - user message which triggered the dynamic discovery search
     * @param candidates  for dynamic discovery
     * @throws EbMS3Exception
     */
    public void lookupAndUpdateConfigurationForToPartyId(String cacheKey, UserMessage userMessage, Collection<eu.domibus.common.model.configuration.Process> candidates) throws EbMS3Exception {
        EndpointInfo endpointInfo = lookupByFinalRecipient(userMessage);
        LOG.debug("Found endpoint. Configure PMode and truststore!");

        PartyId toPartyId = updateToParty(userMessage, endpointInfo.getCertificate());
        cachedToPartyId.put(cacheKey, toPartyId);

        Party configurationParty = updateConfigurationParty(toPartyId.getValue(), toPartyId.getType(), endpointInfo.getAddress());
        updateResponderPartiesInPmode(candidates, configurationParty);

        Property finalRecipient = getFinalRecipient(userMessage);
        final String finalRecipientValue = finalRecipient.getValue();
        final String receiverURL = endpointInfo.getAddress();
        setReceiverPartyEndpoint(finalRecipientValue, receiverURL);

        addCertificatesReceivedFromSmp(userMessage, endpointInfo);
    }

    private void addCertificatesReceivedFromSmp(UserMessage userMessage, EndpointInfo endpointInfo) throws EbMS3Exception {
        String pModeKey = findUserMessageExchangeContext(userMessage, MSHRole.SENDING).getPmodeKey();
        LOG.debug("PMode found [{}]", pModeKey);
        LegConfiguration legConfiguration = getLegConfiguration(pModeKey);
        LOG.info("Found leg [{}] for PMode key [{}]", legConfiguration.getName(), pModeKey);

        X509Certificate certificate;
        String cn;
        try {
            certificate = endpointInfo.getCertificate();
            //parse certificate for common name = toPartyId
            cn = certificateService.extractCommonName(certificate);
            LOG.debug("Extracted the common name [{}]", cn);
        } catch (final InvalidNameException e) {
            LOG.error("Error while extracting CommonName from certificate", e);
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0003)
                    .message("Error while extracting CommonName from certificate")
                    .refToMessageId(userMessage.getMessageId())
                    .cause(e)
                    .build();
        }
        SecurityProfile securityProfile = legConfiguration.getSecurity().getProfile();

        if (securityProfile != null) {
            addCertificate(cn, securityProfile, CertificatePurpose.SIGN, certificate);
            addCertificate(cn, securityProfile, CertificatePurpose.ENCRYPT, certificate);
        } else {
            //legacy alias
            addCertificate(cn, null, null, certificate);
        }
    }

    private void addCertificate(String cn, SecurityProfile securityProfile, CertificatePurpose certificatePurpose, final X509Certificate certificate) {
        Domain currentDomain = domainProvider.getCurrentDomain();

        String alias = cn;
        if (securityProfile != null) {
            alias = securityProfileService.getCertificateAliasForPurpose(cn, securityProfile, certificatePurpose);
        }

        boolean added = multiDomainCertificateProvider.addCertificate(currentDomain, certificate, alias, true);
        if (added) {
            LOG.debug("Added public certificate [{}] with alias [{}] to the truststore for domain [{}]", certificate, cn, currentDomain);
        }
    }

    /**
     * Method returns cache key for dynamic discovery lookup.
     *
     * @param userMessage
     * @return cache key string with format: #domain + #participantId + #participantIdScheme + #documentId + #processId + #processIdScheme";
     */
    protected String getCacheKeyForDynamicDiscovery(UserMessage userMessage) {
        //"
        Property finalRecipient = getFinalRecipient(userMessage);
        // create key
        //"#domain + #participantId + #participantIdScheme + #documentId + #processId + #processIdScheme";
        String cacheKey = domainProvider.getCurrentDomain().getCode() +
                finalRecipient.getValue() +
                finalRecipient.getType() +
                userMessage.getActionValue() +
                userMessage.getService().getValue() +
                userMessage.getService().getType();
        return cacheKey;
    }

    protected PartyId getFromPartyId(UserMessage userMessage) throws EbMS3Exception {
        PartyId from = null;
        String messageId = getMessageId(userMessage);
        if (userMessage != null &&
                userMessage.getPartyInfo() != null &&
                userMessage.getPartyInfo().getFrom() != null) {
            from = userMessage.getPartyInfo().getFrom().getFromPartyId();
        }
        if (from == null) {
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0003)
                    .message("Invalid From party identifier")
                    .refToMessageId(messageId)
                    .build();
        }

        return from;
    }

    protected String getMessageId(UserMessage userMessage) {
        if (userMessage == null) {
            return null;
        }
        return userMessage.getMessageId();
    }

    protected synchronized Party updateConfigurationParty(String name, String type, String endpoint) {
        LOG.info("Update the configuration party with [{}] [{}] [{}]", name, type, endpoint);
        // update the list of party types
        PartyIdType configurationType = updateConfigurationType(type);

        // search if the party exists in the pMode
        Party configurationParty = null;
        for (final Party party : getConfiguration().getBusinessProcesses().getParties()) {
            if (StringUtils.equalsIgnoreCase(party.getName(), name)) {
                LOG.debug("Party exists in the pmode: " + party.getName());
                configurationParty = party;
                break;
            }
        }

        // remove party if exists to add it with latest values for address and type
        if (configurationParty != null) {
            LOG.debug("Remove party to add with new values " + configurationParty.getName());
            getConfiguration().getBusinessProcesses().removeParty(configurationParty);
        }
        // set the new endpoint if exists, otherwise copy the old one if exists
        String newEndpoint = endpoint;
        if (newEndpoint == null) {
            newEndpoint = MSH_ENDPOINT;
            if (configurationParty != null && configurationParty.getEndpoint() != null) {
                newEndpoint = configurationParty.getEndpoint();
            }
        }

        LOG.debug("New endpoint is [{}]", newEndpoint);
        Party newConfigurationParty = buildNewConfigurationParty(name, configurationType, newEndpoint);
        LOG.debug("Add new configuration party: " + newConfigurationParty.getName());
        getConfiguration().getBusinessProcesses().addParty(newConfigurationParty);

        return newConfigurationParty;
    }

    protected Party buildNewConfigurationParty(String name, PartyIdType configurationType, String endpoint) {
        Party newConfigurationParty = new Party();
        final Identifier partyIdentifier = new Identifier();
        partyIdentifier.setPartyId(name);
        partyIdentifier.setPartyIdType(configurationType);

        newConfigurationParty.setName(partyIdentifier.getPartyId());
        newConfigurationParty.getIdentifiers().add(partyIdentifier);
        newConfigurationParty.setEndpoint(endpoint);
        return newConfigurationParty;
    }

    protected PartyIdType updateConfigurationType(String type) {
        Set<PartyIdType> partyIdTypes = getConfiguration().getBusinessProcesses().getPartyIdTypes();
        if (partyIdTypes == null) {
            LOG.info("Empty partyIdTypes set");
            partyIdTypes = new HashSet<>();
        }

        PartyIdType configurationType = null;
        for (final PartyIdType t : partyIdTypes) {
            if (StringUtils.equalsIgnoreCase(t.getValue(), type)) {
                LOG.debug("PartyIdType exists in the pmode [{}]", type);
                configurationType = t;
            }
        }
        // add to partyIdType list
        if (configurationType == null) {
            LOG.debug("Add new PartyIdType [{}]", type);
            configurationType = new PartyIdType();
            configurationType.setName(type);
            configurationType.setValue(type);
            partyIdTypes.add(configurationType);
            this.getConfiguration().getBusinessProcesses().setPartyIdTypes(partyIdTypes);
        }
        return configurationType;
    }

    protected synchronized void updateResponderPartiesInPmode(Collection<eu.domibus.common.model.configuration.Process> candidates, Party configurationParty) {
        LOG.debug("updateResponderPartiesInPmode with party " + configurationParty.getName());
        for (final Process candidate : candidates) {
            boolean partyFound = false;
            for (final Party party : candidate.getResponderParties()) {
                if (StringUtils.equalsIgnoreCase(configurationParty.getName(), party.getName())) {
                    partyFound = true;
                    LOG.debug("partyFound in candidate: " + candidate.getName());
                    break;
                }
            }
            if (!partyFound) {
                candidate.getResponderParties().add(configurationParty);
            }
        }
    }

    protected synchronized void updateInitiatorPartiesInPmode(Collection<eu.domibus.common.model.configuration.Process> candidates, Party configurationParty) {
        LOG.debug("updateInitiatorPartiesInPmode with party " + configurationParty.getName());
        for (final Process candidate : candidates) {
            boolean partyFound = false;
            for (final Party party : candidate.getInitiatorParties()) {
                if (StringUtils.equalsIgnoreCase(configurationParty.getName(), party.getName())) {
                    partyFound = true;
                    LOG.debug("partyFound in candidate: " + candidate.getName());
                    break;
                }
            }
            if (!partyFound) {
                candidate.getInitiatorParties().add(configurationParty);
            }
        }
    }

    protected PartyId updateToParty(UserMessage userMessage, final X509Certificate certificate) throws EbMS3Exception {
        String cn;
        try {
            //parse certificate for common name = toPartyId
            cn = certificateService.extractCommonName(certificate);
            LOG.debug("Extracted the common name [{}]", cn);
        } catch (final InvalidNameException e) {
            LOG.error("Error while extracting CommonName from certificate", e);
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0003)
                    .message("Error while extracting CommonName from certificate")
                    .refToMessageId(userMessage.getMessageId())
                    .cause(e)
                    .build();
        }
        //set toPartyId in UserMessage
        String type = dynamicDiscoveryService.getPartyIdType();
        LOG.debug("Set DDC value to TO PartyId: Value: [{}], type: [{}].", cn, type);

        // double check not to add empty value as a type
        // because it is invalid by the oasis messaging  xsd
        if (StringUtils.isEmpty(type)) {
            type = null;
        }

        final PartyId receiverParty = partyIdDictionaryService.findOrCreateParty(cn, type);

        userMessage.getPartyInfo().getTo().setToPartyId(receiverParty);
        if (userMessage.getPartyInfo().getTo().getToRole() == null) {
            String responderRoleValue = dynamicDiscoveryService.getResponderRole();
            PartyRole partyRole = partyRoleDictionaryService.findOrCreateRole(responderRoleValue);
            userMessage.getPartyInfo().getTo().setToRole(partyRole);
        }

        return receiverParty;
    }

    protected EndpointInfo lookupByFinalRecipient(UserMessage userMessage) throws EbMS3Exception {
        Property finalRecipient = getFinalRecipient(userMessage);
        if (finalRecipient == null) {
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                    .message("Dynamic discovery processes found for message but finalRecipient information is missing in messageProperties.")
                    .refToMessageId(userMessage.getMessageId())
                    .build();
        }
        LOG.info("Perform lookup by finalRecipient: " + finalRecipient.getName() + " " + finalRecipient.getType() + " " + finalRecipient.getValue());

        //lookup sml/smp - result is cached
        final EndpointInfo endpoint = dynamicDiscoveryService.lookupInformation(domainProvider.getCurrentDomain().getCode(), finalRecipient.getValue(),
                finalRecipient.getType(),
                userMessage.getActionValue(),
                userMessage.getService().getValue(),
                userMessage.getService().getType());

        // The SMP entries missing this info are not for the use of Domibus
        if (endpoint.getAddress() == null || endpoint.getCertificate() == null) {
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0010)
                    .message("Invalid endpoint metadata received from the dynamic discovery process.")
                    .refToMessageId(userMessage.getMessageId())
                    .build();
        }
        LOG.debug("Lookup successful: " + endpoint.getAddress());
        return endpoint;
    }

    /*
     * Check all dynamic processes to find candidates for dynamic discovery lookup.
     */
    protected Collection<eu.domibus.common.model.configuration.Process> findCandidateProcesses(UserMessage userMessage, final MSHRole mshRole) {
        LOG.debug("Finding candidate processes.");
        Collection<eu.domibus.common.model.configuration.Process> candidates = new HashSet<>();
        Collection<eu.domibus.common.model.configuration.Process> processes = getDynamicProcesses(mshRole);

        for (final Process process : processes) {
            if (matchProcess(process, mshRole)) {
                LOG.debug("Process matched: [{}] [{}]", process.getName(), mshRole);
                for (final LegConfiguration legConfiguration : process.getLegs()) {
                    if (StringUtils.equalsIgnoreCase(legConfiguration.getService().getValue(), userMessage.getService().getValue()) &&
                            StringUtils.equalsIgnoreCase(legConfiguration.getAction().getValue(), userMessage.getActionValue())) {
                        LOG.debug("Leg matched, adding process. Leg: " + legConfiguration.getName());
                        candidates.add(process);
                    }
                }
            }
        }

        return candidates;
    }

    /*
     * On the receiving, the initiator is unknown, on the sending side the responder is unknown.
     */
    protected boolean matchProcess(final Process process, MSHRole mshRole) {
        if (MSHRole.RECEIVING.equals(mshRole)) {
            return process.isDynamicInitiator() || process.getInitiatorParties().contains(this.getConfiguration().getParty());
        } else { // MSHRole.SENDING
            return process.isDynamicResponder() || process.getResponderParties().contains(this.getConfiguration().getParty());
        }
    }

    protected Property getFinalRecipient(UserMessage userMessage) {
        if (userMessage.getMessageProperties() == null ||
                userMessage.getMessageProperties().isEmpty()) {
            LOG.warn("Empty property set");
            return null;
        }

        for (final eu.domibus.api.model.Property p : userMessage.getMessageProperties()) {
            if (p.getName() != null && StringUtils.equalsIgnoreCase(p.getName(), MessageConstants.FINAL_RECIPIENT)) {
                return p;
            }
            LOG.debug("Property: " + p.getName());
        }
        return null;
    }
}
