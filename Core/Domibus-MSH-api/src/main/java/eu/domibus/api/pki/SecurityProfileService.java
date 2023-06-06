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

    /**
     * Creates the certificate alias in the form: partyName_securityProfile_certificatePurpose if security profiles are configured.
     * If no security profiles are configured then the alias consists only of the partyName, this being considered as a legacy alias
     *
     * @param partyName the party name
     * @param securityProfile the configured security profile, or null if no security profile is configured
     * @param certificatePurpose can be SIGN, ENCRYPT, DECRYPT
     * @return the alias created as described above
     */
    String getCertificateAliasForPurpose(String partyName, SecurityProfile securityProfile, CertificatePurpose certificatePurpose);

    CertificatePurpose extractCertificatePurpose(String alias);

    SecurityProfile extractSecurityProfile(String alias);

    /**
     * Checks if the signing certificate of the acknowledgement message sender is in the TrustStore
     * @param securityProfile - the SecurityProfile
     * @param userMessage - the UserMessage that was sent
     */
    void checkIfAcknowledgmentSigningCertificateIsInTheTrustStore(final SecurityProfile securityProfile, UserMessage userMessage);
}
