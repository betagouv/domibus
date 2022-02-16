package eu.domibus.core.pmode.provider.dynamicdiscovery;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.proxy.ProxyUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.api.pki.CertificateService;
import no.difi.vefa.peppol.common.lang.EndpointNotFoundException;
import no.difi.vefa.peppol.common.lang.PeppolLoadingException;
import no.difi.vefa.peppol.common.lang.PeppolParsingException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.LookupClientBuilder;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.lookup.locator.BusdoxLocator;
import no.difi.vefa.peppol.mode.Mode;
import no.difi.vefa.peppol.security.lang.PeppolSecurityException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.KeyStore;

import static eu.domibus.core.cache.DomibusCacheService.DYNAMIC_DISCOVERY_ENDPOINT;

/**
 * Service to query the SMP to extract the required information about the unknown receiver AP.
 * The SMP Lookup is done using an SMP Client software, with the following input:
 * The End Receiver Participant ID (C4)
 * The Document ID
 * The Process ID
 * <p>
 * Upon a successful lookup, the result contains the endpoint address and also othe public certificate of the receiver.
 */
@Service
@Qualifier("dynamicDiscoveryServicePEPPOL")
public class DynamicDiscoveryServicePEPPOL implements DynamicDiscoveryService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DynamicDiscoveryServicePEPPOL.class);

    private static final String RESPONDER_ROLE = "urn:fdc:peppol.eu:2017:roles:ap:as4";

    private static final String PARTY_ID_TYPE = "urn:fdc:peppol.eu:2017:identifiers:ap";

    public static final String SCHEME_DELIMITER = "::";

    private final DomibusPropertyProvider domibusPropertyProvider;

    private final MultiDomainCryptoService multiDomainCertificateProvider;

    private final DomainContextProvider domainProvider;

    private final ProxyUtil proxyUtil;

    private final CertificateService certificateService;

    private final DomibusHttpRoutePlanner domibusHttpRoutePlanner;

    private final ObjectProvider<DomibusCertificateValidator> domibusCertificateValidators;

    private final ObjectProvider<BusdoxLocator> busdoxLocators;

    private final ObjectProvider<DomibusApacheFetcher> domibusApacheFetchers;

    private final ObjectProvider<EndpointInfo> endpointInfos;

    public DynamicDiscoveryServicePEPPOL(DomibusPropertyProvider domibusPropertyProvider,
                                         MultiDomainCryptoService multiDomainCertificateProvider,
                                         DomainContextProvider domainProvider,
                                         ProxyUtil proxyUtil,
                                         CertificateService certificateService,
                                         DomibusHttpRoutePlanner domibusHttpRoutePlanner,
                                         ObjectProvider<DomibusCertificateValidator> domibusCertificateValidators,
                                         ObjectProvider<BusdoxLocator> busdoxLocators,
                                         ObjectProvider<DomibusApacheFetcher> domibusApacheFetchers,
                                         ObjectProvider<EndpointInfo> endpointInfos) {
        this.domibusPropertyProvider = domibusPropertyProvider;

        this.multiDomainCertificateProvider = multiDomainCertificateProvider;
        this.domainProvider = domainProvider;
        this.proxyUtil = proxyUtil;
        this.certificateService = certificateService;
        this.domibusHttpRoutePlanner = domibusHttpRoutePlanner;
        this.domibusCertificateValidators = domibusCertificateValidators;
        this.busdoxLocators = busdoxLocators;
        this.domibusApacheFetchers = domibusApacheFetchers;
        this.endpointInfos = endpointInfos;
    }

    @Cacheable(value = DYNAMIC_DISCOVERY_ENDPOINT, key = "#domain + #participantId + #participantIdScheme + #documentId + #processId + #processIdScheme")
    public EndpointInfo lookupInformation(final String domain, final String participantId, final String participantIdScheme, final String documentId, final String processId, final String processIdScheme) {

        LOG.info("[PEPPOL SMP] Do the lookup by: [{}] [{}] [{}] [{}] [{}]", participantId, participantIdScheme, documentId, processId, processIdScheme);
        final String smlInfo = domibusPropertyProvider.getProperty(SMLZONE_KEY);
        if (StringUtils.isBlank(smlInfo)) {
            throw new ConfigurationException("SML Zone missing. Configure in domibus-configuration.xml");
        }
        String mode = domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_MODE);
        if (StringUtils.isBlank(mode)) {
            mode = Mode.TEST;
        }

        final String certRegex = domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_CERT_PEPPOL_REGEX);
        if (StringUtils.isBlank(certRegex)) {
            LOG.warn("The value for property [{}] is empty.", DYNAMIC_DISCOVERY_CERT_PEPPOL_REGEX);
        }

        final String allowedCertificatePolicyId = domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_CERT_POLICY);
        if (StringUtils.isBlank(allowedCertificatePolicyId)) {
            LOG.debug("The value for property [{}] is empty.",DYNAMIC_DISCOVERY_CERT_POLICY );
        }

        LOG.debug("Load truststore for the smpClient");
        KeyStore trustStore = multiDomainCertificateProvider.getTrustStore(domainProvider.getCurrentDomain());

        try {
            // create certificate validator
            DomibusCertificateValidator domibusSMPCertificateValidator = domibusCertificateValidators.getObject(certificateService, trustStore, certRegex,  allowedCertificatePolicyId);

            final LookupClientBuilder lookupClientBuilder = LookupClientBuilder.forMode(mode);
            lookupClientBuilder.locator(busdoxLocators.getObject(smlInfo));
            lookupClientBuilder.fetcher(domibusApacheFetchers.getObject(Mode.of(mode), proxyUtil, domibusHttpRoutePlanner));
            lookupClientBuilder.certificateValidator(domibusSMPCertificateValidator);
            final LookupClient smpClient = lookupClientBuilder.build();
            final ParticipantIdentifier participantIdentifier = ParticipantIdentifier.of(participantId, Scheme.of(participantIdScheme));
            final DocumentTypeIdentifier documentIdentifier = getDocumentTypeIdentifier(documentId);

            final ProcessIdentifier processIdentifier = getProcessIdentifier(processId);
            LOG.debug("Getting the ServiceMetadata");
            final ServiceMetadata sm = smpClient.getServiceMetadata(participantIdentifier, documentIdentifier);

            String transportProfileAS4 = domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_TRANSPORTPROFILEAS4);
            LOG.debug("Getting the Endpoint from ServiceMetadata with transportprofile [{}]", transportProfileAS4);
            final Endpoint endpoint = sm.getEndpoint(processIdentifier, TransportProfile.of(transportProfileAS4));

            if (endpoint == null || endpoint.getAddress() == null) {
                throw new ConfigurationException("Received incomplete metadata from the SMP for documentId " + documentId + " processId " + processId);
            }
            return endpointInfos.getObject(endpoint.getAddress().toString(), endpoint.getCertificate());
        } catch (final PeppolParsingException | PeppolLoadingException | PeppolSecurityException | LookupException | EndpointNotFoundException | IllegalStateException e) {
            String msg = "Could not fetch metadata from SMP for documentId " + documentId + " processId " + processId;
            // log error, because cause in ConfigurationException is consumed..
            LOG.error(msg, e);
            throw new ConfigurationException(msg, e);
        }
    }

    protected DocumentTypeIdentifier getDocumentTypeIdentifier(String documentId) throws PeppolParsingException {
        DocumentTypeIdentifier result = null;
        if (StringUtils.contains(documentId, DocumentTypeIdentifier.DEFAULT_SCHEME.getIdentifier())) {
            LOG.debug("Getting DocumentTypeIdentifier by parsing the document Id [{}]", documentId);
            result = DocumentTypeIdentifier.parse(documentId);
        } else {
            LOG.debug("Getting DocumentTypeIdentifier for the document Id [{}]", documentId);
            result = DocumentTypeIdentifier.of(documentId);
        }
        return result;
    }

    protected ProcessIdentifier getProcessIdentifier(String processId) throws PeppolParsingException {
        ProcessIdentifier result = null;
        if (StringUtils.contains(processId, SCHEME_DELIMITER)) {
            LOG.debug("Getting ProcessIdentifier by parsing the process Id [{}]", processId);
            result = ProcessIdentifier.parse(processId);
        } else {
            LOG.debug("Getting ProcessIdentifier for process Id [{}]", processId);
            result = ProcessIdentifier.of(processId);
        }
        return result;
    }

    @Override
    public String getPartyIdType() {
        String propVal = domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_PARTYID_TYPE);
        // if is null - this means property is commented-out and default value must be set.
        // else if is empty - property is set in domibus.properties as empty string and the right value for the
        // ebMS 3.0  PartyId/@type is null value!
        if (propVal==null) {
            propVal = PARTY_ID_TYPE;
        } else if (StringUtils.isEmpty(propVal)) {
            propVal = null;
        }
        return propVal;
    }

    @Override
    public String getResponderRole() {
        String propVal = domibusPropertyProvider.getProperty(DYNAMIC_DISCOVERY_PARTYID_RESPONDER_ROLE);
        if (StringUtils.isEmpty(propVal)) {
            propVal = RESPONDER_ROLE;
        }
        return propVal;
    }

}