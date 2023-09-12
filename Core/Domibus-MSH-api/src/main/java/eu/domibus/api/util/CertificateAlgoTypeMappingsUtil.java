package eu.domibus.api.util;

import eu.domibus.api.pki.CertificateAlgoType;

import java.util.EnumMap;

/**
 * Utility class for retrieving the algorithm id (e.g. the curve names for ECC) based on the standard algorithm name
 *
 * @author Lucian FURCA
 * @since 5.2
 */
public class CertificateAlgoTypeMappingsUtil {
    private static final EnumMap<CertificateAlgoType, String> algoMappings = new EnumMap<>(CertificateAlgoType.class);

    static {
        algoMappings.put(CertificateAlgoType.RSA,"RSA");
        algoMappings.put(CertificateAlgoType.X25519,"1.3.101.110");
        algoMappings.put(CertificateAlgoType.X448,"1.3.101.111");
        algoMappings.put(CertificateAlgoType.ED25519,"1.3.101.112");
        algoMappings.put(CertificateAlgoType.ED448,"1.3.101.113");
    }

    public static String getAlgoIdMapping(CertificateAlgoType algoType) {
        return algoMappings.get(algoType);
    }
}
