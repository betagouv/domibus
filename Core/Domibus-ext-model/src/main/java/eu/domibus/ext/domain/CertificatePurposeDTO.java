package eu.domibus.ext.domain;

/**
 * @author Lucian Furca
 * @since 5.2
 */
public enum CertificatePurposeDTO {
    SIGN("SIGN"),
    ENCRYPT("ENCRYPT"),
    DECRYPT("DECRYPT");

    private final String certificatePurpose;

    CertificatePurposeDTO(final String certificatePurpose) {
        this.certificatePurpose = certificatePurpose;
    }

    public String getCertificatePurpose() {
        return this.certificatePurpose;
    }
}
