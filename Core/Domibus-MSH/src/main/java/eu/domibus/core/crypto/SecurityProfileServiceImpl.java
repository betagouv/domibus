package eu.domibus.core.crypto;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.ws.algorithm.DomibusAlgorithmSuiteLoader;
import eu.domibus.core.ebms3.ws.policy.PolicyService;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.neethi.Policy;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.springframework.stereotype.Service;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

/**
 * Provides services needed by the Security Profiles feature
 * @author Lucian FURCA
 * @since 5.1
 */
@Service
public class SecurityProfileServiceImpl implements SecurityProfileService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SecurityProfileServiceImpl.class);

    protected final DomibusAlgorithmSuiteLoader domibusAlgorithmSuiteLoader;

    protected final PolicyService policyService;

    protected final PModeProvider pModeProvider;

    protected final MultiDomainCryptoService multiDomainCertificateProvider;

    protected final DomainContextProvider domainContextProvider;

    public SecurityProfileServiceImpl(DomibusAlgorithmSuiteLoader domibusAlgorithmSuiteLoader,
                                      PolicyService policyService,
                                      PModeProvider pModeProvider,
                                      MultiDomainCryptoService multiDomainCertificateProvider,
                                      DomainContextProvider domainContextProvider) {
        this.domibusAlgorithmSuiteLoader = domibusAlgorithmSuiteLoader;
        this.policyService = policyService;
        this.pModeProvider = pModeProvider;
        this.multiDomainCertificateProvider = multiDomainCertificateProvider;
        this.domainContextProvider = domainContextProvider;
    }

    public boolean isSecurityPolicySet(String policyFromSecurity, SecurityProfile securityProfile, String legName) throws PModeException {
        Policy policy;
        try {
            policy = policyService.parsePolicy("policies/" + policyFromSecurity, securityProfile);
        } catch (final ConfigurationException e) {
            String message = String.format("Error retrieving policy for leg [%s]", legName);
            throw new PModeException(DomibusCoreErrorCode.DOM_002, message);
        }

        return !policyService.isNoSecurityPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public String getSecurityAlgorithm(String policyFromSecurity, SecurityProfile securityProfile, String legName) throws PModeException {
        if (!isSecurityPolicySet(policyFromSecurity, securityProfile, legName)) {
            return null;
        }

        if (securityProfile == null) {
            LOG.info("The leg configuration contains no security profile info so the default RSA_SHA256 algorithm is used.");
            securityProfile = SecurityProfile.RSA;
        }
        final AlgorithmSuite.AlgorithmSuiteType algorithmSuiteType = domibusAlgorithmSuiteLoader.getAlgorithmSuiteType(securityProfile);
        return algorithmSuiteType.getAsymmetricSignature();
    }

    /**
     * {@inheritDoc}
     */
    public String getCertificateAliasForPurpose(String partyName, SecurityProfile securityProfile, CertificatePurpose certificatePurpose) {
        switch (certificatePurpose) {
            case SIGN:
            case ENCRYPT:
            case DECRYPT:
                return getSecurityProfileAlias(partyName, securityProfile, certificatePurpose);
            default:
                throw new DomibusCertificateException("Invalid certificate usage [" + certificatePurpose +"]");
        }
    }

    private String getSecurityProfileAlias(String partyName, SecurityProfile securityProfile, CertificatePurpose certificatePurpose) {
        String alias = partyName;
        if (securityProfile != null) {
            alias = partyName + "_" + StringUtils.lowerCase(securityProfile.getProfile()) + "_" + certificatePurpose.getCertificatePurpose().toLowerCase();
        }
        LOG.info("The following alias was determined for [{}]ing: [{}]", certificatePurpose.getCertificatePurpose().toLowerCase(), alias);
        return alias;
    }

    public CertificatePurpose extractCertificatePurpose(String alias) {
        return CertificatePurpose.lookupByName(StringUtils.substringAfterLast(alias, "_").toUpperCase());
    }

    public SecurityProfile extractSecurityProfile(String alias) {
        return SecurityProfile.lookupByName(StringUtils.substringAfterLast(StringUtils.substringBeforeLast(alias,"_"), "_").toUpperCase());
    }

    /**
     * {@inheritDoc}
     */
    public void checkIfAcknowledgmentSigningCertificateIsInTheTrustStore(final SecurityProfile securityProfile, UserMessage userMessage) {
        String acknowledgementSenderName;
        try {
            acknowledgementSenderName = pModeProvider.findReceiverParty(userMessage);
        } catch (EbMS3Exception e) {
            String exceptionMessage = String.format("Error while retrieving senderParty from UserMessage: %s", e.getMessage());
            throw new ConfigurationException(exceptionMessage);
        }

        String aliasForSigning = getCertificateAliasForPurpose(acknowledgementSenderName, securityProfile, CertificatePurpose.SIGN);

        try {
            X509Certificate cert = multiDomainCertificateProvider.getCertificateFromTruststore(domainContextProvider.getCurrentDomain(), aliasForSigning);
            if (cert == null) {
                String exceptionMessage = String.format("Signing certificate for sender [%s] could not be found in the TrustStore", acknowledgementSenderName);
                throw new eu.domibus.api.security.CertificateException(DomibusCoreErrorCode.DOM_005, exceptionMessage);
            }
        } catch (KeyStoreException e) {
            String exceptionMessage = String.format("Failed to get signing certificate for sender [%s] from truststore: %s", acknowledgementSenderName, e.getMessage());
            throw new eu.domibus.api.security.CertificateException(DomibusCoreErrorCode.DOM_005, exceptionMessage);
        }
    }
}
