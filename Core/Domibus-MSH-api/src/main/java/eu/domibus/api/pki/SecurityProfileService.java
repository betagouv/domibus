package eu.domibus.api.pki;

import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;

/**
 * @author Lucian FURCA
 * @since 5.2
 */
public interface SecurityProfileService {
    String getCertificateAliasForPurpose(String partyName, SecurityProfile securityProfile, CertificatePurpose certificatePurpose);
}
