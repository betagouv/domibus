package eu.domibus.api.pki;

import eu.domibus.api.model.UserMessage;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;

/**
 * Provides services needed by the Security Profiles feature
 *
 * @author Lucian FURCA
 * @since 5.2
 */
public interface SecurityProfileService {
    boolean isSecurityPolicySet(String policyFromSecurityProfile, SecurityProfile securityProfile, String legName) throws PModeException;

    /**
     * Retrieves the Asymmetric Signature Algorithm corresponding to the security profile, defaulting to RSA_SHA256
     * correspondent if no security profile is defined
     *
     * @param policyFromSecurity the policy name from the Security entity
     * @param securityProfile the security profile
     * @param legName the configured leg name
     * @throws PModeException thrown when the legConfiguration contains an invalid security profile
     * @return the Asymmetric Signature Algorithm
     */
    String getSecurityAlgorithm(String policyFromSecurity, SecurityProfile securityProfile, String legName) throws PModeException;

    String getAliasForSigning(SecurityProfile securityProfile, String senderName);

    String getCertificateAliasForPurpose(String partyName, SecurityProfile securityProfile, CertificatePurpose certificatePurpose);

    String getAliasForEncrypting(SecurityProfile securityProfile, String receiverName);

    String getAliasForDecrypting(SecurityProfile securityProfile, String receiverName);

    CertificatePurpose extractCertificatePurpose(String alias);

    SecurityProfile extractSecurityProfile(String alias);

    /**
     * Checks if the signing certificate of the acknowledgement message sender is in the TrustStore
     * @param securityProfile - the SecurityProfile
     * @param userMessage - the UserMessage that was sent
     */
    void checkIfAcknowledgmentSigningCertificateIsInTheTrustStore(final SecurityProfile securityProfile, UserMessage userMessage);
}
