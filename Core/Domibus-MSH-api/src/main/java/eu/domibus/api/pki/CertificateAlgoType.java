package eu.domibus.api.pki;

/**
 * Defines the supported certificate algorithm type names and provides the algo id based on the algo name
 *
 * @author Lucian FURCA
 * @since 5.2
 */
public enum CertificateAlgoType {
    RSA("RSA"),
    X25519("1.3.101.110"),//NOSONAR
    X448("1.3.101.111"),//NOSONAR
    ED25519("1.3.101.112"),//NOSONAR
    ED448("1.3.101.113");//NOSONAR

    private final String certificateAlgoType;

    CertificateAlgoType(final String certificateAlgoType) {
        this.certificateAlgoType = certificateAlgoType;
    }

    /**
     * Retrieves the algorithm id (e.g. the curve name for an ECC certificate: 1.3.101.110) based on the standard algorithm name(e.g. X25519)
     * @param algoName - the algorithm standard name(e.g. X25519)
     */

    public static CertificateAlgoType lookupAlgoIdByAlgoName(String algoName) {
        return CertificateAlgoType.valueOf(algoName);
    }

    public String getCertificateAlgoType() {
        return this.certificateAlgoType;
    }
}
