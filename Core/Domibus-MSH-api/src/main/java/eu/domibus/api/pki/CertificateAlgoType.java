package eu.domibus.api.pki;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Defines the supported certificate algorithm type names
 *
 * @author Lucian FURCA
 * @since 5.2
 */
public enum CertificateAlgoType {
    RSA("RSA"),
    X25519("X25519"),
    X448("X448"),
    ED25519("ED25519"),
    ED448("ED448");

    private static final Map<String, CertificateAlgoType> nameIndex =
            Maps.newHashMapWithExpectedSize(CertificateAlgoType.values().length);
    static {
        for (CertificateAlgoType certificateAlgoType : CertificateAlgoType.values()) {
            nameIndex.put(certificateAlgoType.name(), certificateAlgoType);
        }
    }

    public static CertificateAlgoType lookupByName(String name) {
        return nameIndex.get(name);
    }


    private final String certificateAlgoType;

    CertificateAlgoType(final String certificateAlgoType) {
        this.certificateAlgoType = certificateAlgoType;
    }

    public String getCertificateAlgoType() {
        return this.certificateAlgoType;
    }
}
