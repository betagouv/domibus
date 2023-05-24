package eu.domibus.core.crypto;


import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.common.model.configuration.LegConfiguration;
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
public class SecurityProfileService implements eu.domibus.api.pki.SecurityProfileService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SecurityProfileService.class);

    protected final DomibusAlgorithmSuiteLoader domibusAlgorithmSuiteLoader;

    protected final PolicyService policyService;

    protected final PModeProvider pModeProvider;

    protected final MultiDomainCryptoService multiDomainCertificateProvider;

    protected final DomainContextProvider domainContextProvider;

    public SecurityProfileService(DomibusAlgorithmSuiteLoader domibusAlgorithmSuiteLoader, PolicyService policyService, PModeProvider pModeProvider, MultiDomainCryptoService multiDomainCertificateProvider, DomainContextProvider domainContextProvider) {
        this.domibusAlgorithmSuiteLoader = domibusAlgorithmSuiteLoader;
        this.policyService = policyService;
        this.pModeProvider = pModeProvider;
        this.multiDomainCertificateProvider = multiDomainCertificateProvider;
        this.domainContextProvider = domainContextProvider;
    }

    public boolean isSecurityPolicySet(LegConfiguration legConfiguration) {
        Policy policy;
        try {
            policy = policyService.parsePolicy("policies/" + legConfiguration.getSecurity().getPolicy(), legConfiguration.getSecurity().getProfile());
        } catch (final ConfigurationException e) {
            String message = String.format("Error retrieving policy for leg [%s]", legConfiguration.getName());
            throw new ConfigurationException(message);
        }

        return !policyService.isNoSecurityPolicy(policy);
    }

    /**
     * Retrieves the Asymmetric Signature Algorithm corresponding to the security profile, defaulting to RSA_SHA256
     * correspondent if no security profile is defined
     *
     * @param legConfiguration the leg configuration containing the security profile
     * @throws ConfigurationException thrown when the legConfiguration contains an invalid security profile
     * @return the Asymmetric Signature Algorithm
     */
    public String getSecurityAlgorithm(LegConfiguration legConfiguration) throws ConfigurationException {
        if (!isSecurityPolicySet(legConfiguration)) {
            return null;
        }

        SecurityProfile securityProfile = legConfiguration.getSecurity().getProfile();
        if (securityProfile == null) {
            LOG.info("The leg configuration contains no security profile info so the default RSA_SHA256 algorithm is used.");
            securityProfile = SecurityProfile.RSA;
        }
        final AlgorithmSuite.AlgorithmSuiteType algorithmSuiteType = domibusAlgorithmSuiteLoader.getAlgorithmSuiteType(securityProfile);
        return algorithmSuiteType.getAsymmetricSignature();
    }

    public String getAliasForSigning(LegConfiguration legConfiguration, String senderName) {
        return getAliasForSigning(legConfiguration.getSecurity().getProfile(), senderName);
    }

    public String getAliasForSigning(SecurityProfile securityProfile, String senderName) {
        String alias = senderName;
        if (securityProfile != null) {
            alias = senderName + "_" + StringUtils.lowerCase(securityProfile.getProfile()) + "_sign";
        }
        LOG.info("The following alias was determined for signing: [{}]", alias);
        return alias;
    }

    public String getAliasForEncrypting(LegConfiguration legConfiguration, String receiverName) {
        String alias = receiverName;
        SecurityProfile securityProfile = legConfiguration.getSecurity().getProfile();
        if (securityProfile != null) {
            alias = receiverName + "_" + StringUtils.lowerCase(securityProfile.getProfile()) + "_encrypt";
        }
        LOG.info("The following alias was determined for encrypting: [{}]", alias);
        return alias;
    }

    public String getCertificateAliasForPurpose(String partyName, SecurityProfile securityProfile, CertificatePurpose certificatePurpose) {
        switch (certificatePurpose) {
            case SIGN:
                return getAliasForSigning(securityProfile, partyName);
            case ENCRYPT:
                return getAliasForEncrypting(securityProfile, partyName);
            case DECRYPT:
                return getAliasForDecrypting(securityProfile, partyName);
            default:
                throw new DomibusCertificateException("Invalid certificate usage [" + certificatePurpose +"]");
        }
    }

    public String getAliasForEncrypting(SecurityProfile securityProfile, String receiverName) {
        String alias = receiverName;
        if (securityProfile != null) {
            alias = receiverName + "_" + StringUtils.lowerCase(securityProfile.getProfile()) + "_encrypt";
        }
        LOG.info("The following alias was determined for encrypting: [{}]", alias);
        return alias;
    }

    public String getAliasForDecrypting(SecurityProfile securityProfile, String receiverName) {
        String alias = receiverName;
        if (securityProfile != null) {
            alias = receiverName + "_" + StringUtils.lowerCase(securityProfile.getProfile()) + "_decrypt";
        }
        LOG.info("The following alias was determined for decrypting: [{}]", alias);
        return alias;
    }

    public CertificatePurpose extractCertificatePurpose(String alias) {
        return CertificatePurpose.lookupByName(StringUtils.substringAfterLast(alias, "_").toUpperCase());
    }

    public SecurityProfile extractSecurityProfile(String alias) {
        return SecurityProfile.lookupByName(StringUtils.substringAfterLast(StringUtils.substringBeforeLast(alias,"_"), "_").toUpperCase());
    }

    /**
     * Checks if the signing certificate of the acknowledgement message sender is in the TrustStore
     * @param legConfiguration - the legConfiguration
     * @param userMessage - the UserMessage that was sent
     */
    public void checkIfAcknowledgmentSigningCertificateIsInTheTrustStore(final LegConfiguration legConfiguration, UserMessage userMessage) {
        String acknowledgementSenderName;
        try {
            acknowledgementSenderName = pModeProvider.findReceiverParty(userMessage);
        } catch (EbMS3Exception e) {
            String exceptionMessage = String.format("Error while retrieving senderParty from UserMessage: %s", e.getMessage());
            throw new ConfigurationException(exceptionMessage);
        }

        String aliasForSigning = getAliasForSigning(legConfiguration, acknowledgementSenderName);

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
