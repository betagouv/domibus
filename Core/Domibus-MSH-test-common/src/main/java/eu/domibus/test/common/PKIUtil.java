package eu.domibus.test.common;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Cosmin Baciu on 08-Jul-16.
 */
public class PKIUtil {

    public static final String CERTIFICATE_POLICY_ANY = "2.5.29.32.0";
    public static final String CERTIFICATE_POLICY_QCP_NATURAL = "0.4.0.194112.1.0";
    public static final String CERTIFICATE_POLICY_QCP_LEGAL = "0.4.0.194112.1.1";
    public static final String CERTIFICATE_POLICY_QCP_NATURAL_QSCD = "0.4.0.194112.1.2";
    public static final String CERTIFICATE_POLICY_QCP_LEGAL_QSCD = "0.4.0.194112.1.3";

    public X509CRL createCRL(List<BigInteger> revokedSerialNumbers) throws NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        KeyPair caKeyPair = generateKeyPair();

        X509V2CRLGenerator crlGen = new X509V2CRLGenerator();
        Date now = new Date();
        crlGen.setIssuerDN(new X500Principal("CN=GlobalSign Root CA"));
        crlGen.setThisUpdate(now);
        crlGen.setNextUpdate(new Date(now.getTime() + 60 * 1000));
        crlGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

        if (revokedSerialNumbers != null) {
            for (BigInteger revokedSerialNumber : revokedSerialNumbers) {
                crlGen.addCRLEntry(revokedSerialNumber, now, CRLReason.privilegeWithdrawn);
            }
        }

        crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caKeyPair.getPublic()));
        crlGen.addExtension(X509Extensions.CRLNumber, false, new CRLNumber(BigInteger.valueOf(1)));

        return crlGen.generateX509CRL(caKeyPair.getPrivate(), "BC");
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    public X509Certificate createCertificate(BigInteger serial, Date startDate, Date expiryDate, List<String> crlUrls) throws SignatureException, NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException, CertificateEncodingException {
        KeyPair key = generateKeyPair();

        X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
        generator.setSubjectDN(new X509Name("C=BE, O=GlobalSign nv-sa, OU=Root CA"));
        X500Principal subjectName = new X500Principal("CN=GlobalSign Root CA");
        generator.setIssuerDN(subjectName);
        generator.setSerialNumber(serial);
        generator.setNotBefore(startDate);
        generator.setNotAfter(expiryDate);
        generator.setPublicKey(key.getPublic());
        generator.setSignatureAlgorithm("SHA256WithRSAEncryption");

        if (crlUrls != null) {
            DistributionPoint[] distPoints = createDistributionPoints(crlUrls);
            generator.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(distPoints));
        }

        X509Certificate x509Certificate = generator.generate(key.getPrivate(), "BC");
        return x509Certificate;
    }

    public X509Certificate createCertificate(BigInteger serial, List<String> crlUrls) throws SignatureException, NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException, CertificateEncodingException {
        return createCertificate(serial, new Date(), new Date(), crlUrls);
    }

    public DistributionPoint[] createDistributionPoints(List<String> crlUrls) {
        List<DistributionPoint> result = new ArrayList<>();
        for (String crlUrl : crlUrls) {
            DistributionPointName distPointOne = new DistributionPointName(
                    new GeneralNames(
                            new GeneralName(GeneralName.uniformResourceIdentifier, crlUrl)
                    )
            );
            result.add(new DistributionPoint(distPointOne, null, null));
        }


        return result.toArray(new DistributionPoint[0]);
    }

    public X509Certificate createCertificateWithSubject(BigInteger serial, String certificateSubject, Date notBefore, Date notAfter) {
        try {
            return generateCertificate(certificateSubject, serial,
                    notBefore,
                    notAfter, null, null, null, null, null,
                    "SHA256WithRSAEncryption", null);
        } catch (Exception e) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Could not generate certificate with subject [" + certificateSubject + "]", e);
        }
    }

    public X509Certificate createCertificateWithSubject(BigInteger serial, String certificateSubject) {
        return createCertificateWithSubject(serial, certificateSubject, DateUtils.addDays(Calendar.getInstance().getTime(), -30), DateUtils.addDays(Calendar.getInstance().getTime(), 30));
    }


    public X509Certificate createCertificate(BigInteger serial, List<String> crlUrls, List<String> policies) throws
            NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException {
        return generateCertificate("CN=test,OU=Domibus,O=eDelivery,C=EU", serial,
                DateUtils.addDays(Calendar.getInstance().getTime(), -1),
                DateUtils.addDays(Calendar.getInstance().getTime(), 1), null, null, crlUrls, null, null,
                "SHA256WithRSAEncryption", policies);
    }

    /**
     * Generic method for generating test certificatese
     *
     * @param subjectDn                  - subject certificate
     * @param serial                     - serial number
     * @param notBefore                  - start valid period for certificate
     * @param notAfter-                  end valid period for certificate
     * @param issuerCertificate          - issuer certificate
     * @param issuerPrivateKeyForSigning - certificate signing key
     * @param crlUris                    - list of CRLs
     * @param ocspUri                    - OCSP URO
     * @param keyUsage                   - key usage
     * @param signatureAlgorithm         - signature algorithms
     * @param certificatePolicies        - certificate policy
     * @return
     * @throws IllegalStateException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws OperatorCreationException
     * @throws CertificateException
     */
    public X509Certificate generateCertificate(String subjectDn, BigInteger serial,
                                               Date notBefore, Date notAfter, X509Certificate issuerCertificate, PrivateKey issuerPrivateKeyForSigning,
                                               List<String> crlUris, String ocspUri, KeyUsage keyUsage,
                                               String signatureAlgorithm,
                                               List<String> certificatePolicies
    ) throws IllegalStateException, IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException {
        return generateCertificate(subjectDn, serial, notBefore, notAfter, issuerCertificate, issuerPrivateKeyForSigning, crlUris, ocspUri, keyUsage, signatureAlgorithm, certificatePolicies, false);
    }

    /**
     * Generic method for generating test certificatese
     *
     * @param subjectDn                  - subject certificate
     * @param serial                     - serial number
     * @param notBefore                  - start valid period for certificate
     * @param notAfter-                  end valid period for certificate
     * @param issuerCertificate          - issuer certificate
     * @param issuerPrivateKeyForSigning - certificate signing key
     * @param crlUris                    - list of CRLs
     * @param ocspUri                    - OCSP URO
     * @param keyUsage                   - key usage
     * @param signatureAlgorithm         - signature algorithms
     * @param certificatePolicies        - certificate policy
     * @param isRootCA                   - Add Extension.basicConstraints
     * @return
     * @throws IllegalStateException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws OperatorCreationException
     * @throws CertificateException
     */
    public X509Certificate generateCertificate(String subjectDn, BigInteger serial,
                                               Date notBefore, Date notAfter, X509Certificate issuerCertificate, PrivateKey issuerPrivateKeyForSigning,
                                               List<String> crlUris, String ocspUri, KeyUsage keyUsage,
                                               String signatureAlgorithm,
                                               List<String> certificatePolicies, boolean isRootCA
    ) throws IllegalStateException, IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException {


        KeyPair certificateKey = generateKeyPair();

        X500Name issuerName;
        if (ObjectUtils.isNotEmpty(issuerCertificate)) {
            issuerName = new X500Name(issuerCertificate.getSubjectX500Principal().toString());
        } else {
            issuerName = new X500Name(subjectDn);
        }
        X500Name subjectName = new X500Name(subjectDn);
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(certificateKey.getPublic().getEncoded());
        X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(issuerName, serial,
                notBefore, notAfter, subjectName, publicKeyInfo);

        if (isRootCA) {
            x509v3CertificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        }

        // add CRL DistributionPoints
        if (crlUris != null && !crlUris.isEmpty()) {
            List<DistributionPoint> distributionPoints =
                    crlUris.stream().map(crlUri -> {
                        GeneralName generalName = new GeneralName(GeneralName.uniformResourceIdentifier,
                                new DERIA5String(crlUri));
                        GeneralNames generalNames = new GeneralNames(generalName);
                        DistributionPointName distPointName = new DistributionPointName(generalNames);
                        return new DistributionPoint(distPointName, null, null);
                    }).collect(Collectors.toList());
            DistributionPoint[] crlDistPoints = distributionPoints.toArray(new DistributionPoint[]{});
            CRLDistPoint crlDistPoint = new CRLDistPoint(crlDistPoints);
            x509v3CertificateBuilder.addExtension(Extension.cRLDistributionPoints, false, crlDistPoint);

        }

        // add OCSP URI
        if (StringUtils.isNotBlank(ocspUri)) {
            GeneralName ocspName = new GeneralName(GeneralName.uniformResourceIdentifier, ocspUri);
            AuthorityInformationAccess authorityInformationAccess = new AuthorityInformationAccess(X509ObjectIdentifiers.ocspAccessMethod, ocspName);
            x509v3CertificateBuilder.addExtension(Extension.authorityInfoAccess, false, authorityInformationAccess);
        }

        if (ObjectUtils.isNotEmpty(keyUsage)) {
            x509v3CertificateBuilder.addExtension(Extension.keyUsage, true, keyUsage);
        }
        // add certificate policies
        if (certificatePolicies != null && !certificatePolicies.isEmpty()) {
            List<PolicyInformation> policyInformationList = certificatePolicies.stream().map(certificatePolicy -> {
                ASN1ObjectIdentifier policyObjectIdentifier = new ASN1ObjectIdentifier(certificatePolicy);
                return new PolicyInformation(policyObjectIdentifier);
            }).collect(Collectors.toList());

            x509v3CertificateBuilder.addExtension(Extension.certificatePolicies, false,
                    new DERSequence(policyInformationList.toArray(new PolicyInformation[]{})));

        }

        // generate certificate
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        AsymmetricKeyParameter asymmetricKeyParameter = PrivateKeyFactory.createKey(
                issuerPrivateKeyForSigning == null ? certificateKey.getPrivate().getEncoded() : issuerPrivateKeyForSigning.getEncoded()
        );


        ContentSigner contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(asymmetricKeyParameter);
        X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);

        byte[] encodedCertificate = x509CertificateHolder.getEncoded();

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certificateFactory
                .generateCertificate(new ByteArrayInputStream(encodedCertificate));
        return certificate;
    }

    /**
     * Method generates certificate chain
     *
     * @param subjects                in order from CA to leaf
     * @param certificatePoliciesOids Policy oids fromCA to leaf
     * @param startDate               valid certificates from
     * @param expiryDate              valid certificates to
     * @return
     * @throws Exception
     */
    public static X509Certificate[] createCertificateChain(String[] subjects, List<List<String>> certificatePoliciesOids, Date startDate, Date expiryDate) throws Exception {

        String issuer = null;
        PrivateKey issuerKey = null;
        long iSerial = 10000;
        X509Certificate[] certs = new X509Certificate[subjects.length];

        for (int i = 0; i < subjects.length; i++) {
            String subject = subjects[i];

            List<String> certificatePolicies = certificatePoliciesOids.size() > i ? certificatePoliciesOids.get(i) : Collections.emptyList();
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair key = keyGen.generateKeyPair();

            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(new X500Name(issuer == null ? subject : issuer),
                    BigInteger.valueOf(iSerial++), startDate, expiryDate, new X500Name(subject),
                    SubjectPublicKeyInfo.getInstance(key.getPublic().getEncoded()));

            // set basic basicConstraint  for all except the last leaf certificate
            if (i != subjects.length - 1) {
                certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            }

            // add certificate policies
            if (!certificatePolicies.isEmpty()) {
                List<PolicyInformation> policyInformationList = certificatePolicies.stream().map(certificatePolicy -> {
                    ASN1ObjectIdentifier policyObjectIdentifier = new ASN1ObjectIdentifier(certificatePolicy);
                    return new PolicyInformation(policyObjectIdentifier);
                }).collect(Collectors.toList());

                certBuilder.addExtension(Extension.certificatePolicies, false,
                        new DERSequence(policyInformationList.toArray(new PolicyInformation[]{})));

            }
            ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WITHRSA")
                    .build(issuerKey == null ? key.getPrivate() : issuerKey);

            // add certs in reverse order
            certs[subjects.length - 1 - i] = new JcaX509CertificateConverter().getCertificate(certBuilder.build(sigGen));
            issuer = subject;
            issuerKey = key.getPrivate();

        }
        return certs;
    }

    public static KeyStore createTruststore(X509Certificate... certificates) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
        //initi keystore
        truststore.load(null, null);
        for (X509Certificate certificate : certificates) {
            truststore.setCertificateEntry(UUID.randomUUID().toString(), certificate);
        }
        return truststore;
    }
}
