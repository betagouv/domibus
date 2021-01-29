package eu.domibus.core.certificate;

import com.google.common.collect.Lists;
import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.core.alerts.configuration.certificate.expired.ExpiredCertificateConfigurationManager;
import eu.domibus.core.alerts.configuration.certificate.expired.ExpiredCertificateModuleConfiguration;
import eu.domibus.core.alerts.configuration.certificate.imminent.ImminentExpirationCertificateConfigurationManager;
import eu.domibus.core.alerts.configuration.certificate.imminent.ImminentExpirationCertificateModuleConfiguration;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.core.certificate.crl.CRLService;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.util.backup.BackupService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemObjectGenerator;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.joda.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_CERTIFICATE_REVOCATION_OFFSET;
import static eu.domibus.logging.DomibusMessageCode.SEC_CERTIFICATE_REVOKED;
import static eu.domibus.logging.DomibusMessageCode.SEC_CERTIFICATE_SOON_REVOKED;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@Service
public class CertificateServiceImpl implements CertificateService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(CertificateServiceImpl.class);

    public static final String REVOCATION_TRIGGER_OFFSET_PROPERTY = DOMIBUS_CERTIFICATE_REVOCATION_OFFSET;

    private final CRLService crlService;

    private final DomibusPropertyProvider domibusPropertyProvider;

    private final CertificateDao certificateDao;

    private final EventService eventService;

    private final PModeProvider pModeProvider;

    private final ImminentExpirationCertificateConfigurationManager imminentExpirationCertificateConfigurationManager;

    private final ExpiredCertificateConfigurationManager expiredCertificateConfigurationManager;

    private final BackupService backupService;

    public CertificateServiceImpl(CRLService crlService, DomibusPropertyProvider domibusPropertyProvider,
                                  CertificateDao certificateDao, EventService eventService, PModeProvider pModeProvider,
                                  ImminentExpirationCertificateConfigurationManager imminentExpirationCertificateConfigurationManager,
                                  ExpiredCertificateConfigurationManager expiredCertificateConfigurationManager, BackupService backupService) {
        this.crlService = crlService;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.certificateDao = certificateDao;
        this.eventService = eventService;
        this.pModeProvider = pModeProvider;
        this.imminentExpirationCertificateConfigurationManager = imminentExpirationCertificateConfigurationManager;
        this.expiredCertificateConfigurationManager = expiredCertificateConfigurationManager;
        this.backupService = backupService;
    }

    @Override
    public boolean isCertificateChainValid(List<? extends java.security.cert.Certificate> certificateChain) {
        for (java.security.cert.Certificate certificate : certificateChain) {
            boolean certificateValid = isCertificateValid((X509Certificate) certificate);
            if (!certificateValid) {
                LOG.warn("Sender certificate not valid [{}]", certificate);
                return false;
            }
            LOG.debug("Sender certificate valid [{}]", certificate);
        }
        return true;
    }

    @Override
    public boolean isCertificateChainValid(KeyStore trustStore, String alias) throws DomibusCertificateException {
        X509Certificate[] certificateChain = null;
        try {
            certificateChain = getCertificateChain(trustStore, alias);
        } catch (KeyStoreException e) {
            throw new DomibusCertificateException("Error getting the certificate chain from the truststore for [" + alias + "]", e);
        }
        if (certificateChain == null || certificateChain.length == 0 || certificateChain[0] == null) {
            throw new DomibusCertificateException("Could not find alias in the truststore [" + alias + "]");
        }

        for (X509Certificate certificate : certificateChain) {
            boolean certificateValid = isCertificateValid(certificate);
            if (!certificateValid) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isCertificateValid(X509Certificate cert) throws DomibusCertificateException {
        boolean isValid = checkValidity(cert);
        if (!isValid) {
            LOG.warn("Certificate is not valid:[{}] ", cert);
            return false;
        }
        try {
            return !crlService.isCertificateRevoked(cert);
        } catch (Exception e) {
            throw new DomibusCertificateException(e);
        }
    }

    @Override
    public String extractCommonName(final X509Certificate certificate) throws InvalidNameException {

        final String dn = certificate.getSubjectDN().getName();
        LOG.debug("DN is:[{}]", dn);
        final LdapName ln = new LdapName(dn);
        for (final Rdn rdn : ln.getRdns()) {
            if (StringUtils.equalsIgnoreCase(rdn.getType(), "CN")) {
                LOG.debug("CN is: " + rdn.getValue());
                return rdn.getValue().toString();
            }
        }
        throw new IllegalArgumentException("The certificate does not contain a common name (CN): " + certificate.getSubjectDN().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TrustStoreEntry> getTrustStoreEntries(final KeyStore trustStore) {
        try {
            List<TrustStoreEntry> trustStoreEntries = new ArrayList<>();
            final Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
                TrustStoreEntry trustStoreEntry = createTrustStoreEntry(alias, certificate);
                trustStoreEntries.add(trustStoreEntry);
            }
            return trustStoreEntries;
        } catch (KeyStoreException e) {
            LOG.warn(e.getMessage(), e);
            return Lists.newArrayList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveCertificateAndLogRevocation(final KeyStore trustStore, final KeyStore keyStore) {
        saveCertificateData(trustStore, keyStore);
        logCertificateRevocationWarning();
    }

    @Override
    public void validateLoadOperation(ByteArrayInputStream newTrustStoreBytes, String password, String type) {
        try {
            KeyStore tempTrustStore = KeyStore.getInstance(type);
            tempTrustStore.load(newTrustStoreBytes, password.toCharArray());
            newTrustStoreBytes.reset();
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
            throw new DomibusCertificateException("Could not load key store: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendCertificateAlerts() {
        sendCertificateImminentExpirationAlerts();
        sendCertificateExpiredAlerts();
    }

    @Override
    public X509Certificate loadCertificateFromString(String content) {
        if (content == null) {
            throw new DomibusCertificateException("Certificate content cannot be null.");
        }

        CertificateFactory certFactory;
        X509Certificate cert;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new DomibusCertificateException("Could not initialize certificate factory", e);
        }

        try (InputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            InputStream resultStream = contentStream;
            if (!isPemFormat(content)) {
                resultStream = Base64.getMimeDecoder().wrap(resultStream);
            }
            cert = (X509Certificate) certFactory.generateCertificate(resultStream);
        } catch (IOException | CertificateException e) {
            throw new DomibusCertificateException("Could not generate certificate", e);
        }
        return cert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serializeCertificateChainIntoPemFormat(List<? extends java.security.cert.Certificate> certificates) {
        StringWriter sw = new StringWriter();
        for (java.security.cert.Certificate certificate : certificates) {
            try (PemWriter pw = new PemWriter(sw)) {
                PemObjectGenerator gen = new JcaMiscPEMGenerator(certificate);
                pw.writeObject(gen);
            } catch (IOException e) {
                throw new DomibusCertificateException(String.format("Error while serializing certificates:[%s]", certificate.getType()), e);
            }
        }
        final String certificateChainValue = sw.toString();
        LOG.debug("Serialized certificates:[{}]", certificateChainValue);
        return certificateChainValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<X509Certificate> deserializeCertificateChainFromPemFormat(String chain) {
        List<X509Certificate> certificates = new ArrayList<>();
        try (PemReader reader = new PemReader(new StringReader(chain))) {
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            PemObject pemObject;
            while ((pemObject = reader.readPemObject()) != null) {
                if (pemObject.getType().equals("CERTIFICATE")) {
                    java.security.cert.Certificate c = cf.generateCertificate(new ByteArrayInputStream(pemObject.getContent()));
                    final X509Certificate certificate = (X509Certificate) c;
                    LOG.debug("Deserialized certificate:[{}]", certificate.getSubjectDN());
                    certificates.add(certificate);
                } else {
                    throw new DomibusCertificateException("Unknown type " + pemObject.getType());
                }
            }

        } catch (IOException | CertificateException e) {
            throw new DomibusCertificateException("Error while instantiating certificates from pem", e);
        }
        return certificates;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.security.cert.Certificate extractLeafCertificateFromChain(List<? extends java.security.cert.Certificate> certificates) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Extracting leaf certificate from chain");
            for (java.security.cert.Certificate certificate : certificates) {
                LOG.trace("Certificate:[{}]", certificate);
            }
        }
        Set<String> issuerSet = new HashSet<>();
        Map<String, X509Certificate> subjectMap = new HashMap<>();
        for (java.security.cert.Certificate certificate : certificates) {
            X509Certificate x509Certificate = (X509Certificate) certificate;
            final String subjectName = x509Certificate.getSubjectDN().getName();
            subjectMap.put(subjectName, x509Certificate);
            final String issuerName = x509Certificate.getIssuerDN().getName();
            issuerSet.add(issuerName);
            LOG.debug("Certificate subject:[{}] issuer:[{}]", subjectName, issuerName);
        }

        final Set<String> allSubject = subjectMap.keySet();
        //There should always be one more subject more than issuers. Indeed the root CA has the same value as issuer and subject.
        allSubject.removeAll(issuerSet);
        //the unique entry in the set is the leaf.
        if (allSubject.size() == 1) {
            final String leafSubjet = allSubject.iterator().next();
            LOG.debug("Not an issuer:[{}]", leafSubjet);
            return subjectMap.get(leafSubjet);
        }
        if (certificates.size() == 1) {
            LOG.trace("In case of unique self-signed certificate, the issuer and the subject are the same: returning it.");
            return certificates.get(0);
        }
        LOG.error("Certificate exchange type is X_509_PKIPATHV_1 but no leaf certificate has been found");
        return null;
    }

    @Override
    public byte[] getTruststoreContent(String location) {
        File file = createFileWithLocation(location);
        Path path = Paths.get(file.getAbsolutePath());
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new DomibusCertificateException("Could not read truststore from [" + location + "]");
        }
    }

    @Override
    public TrustStoreEntry convertCertificateContent(String certificateContent) {
        X509Certificate cert = loadCertificateFromString(certificateContent);
        return createTrustStoreEntry(null, cert);
    }

    @Override
    public TrustStoreEntry createTrustStoreEntry(X509Certificate cert, String alias) throws KeyStoreException {
        LOG.debug("Create TrustStore Entry for [{}] = [{}] ", alias, cert);
        return createTrustStoreEntry(alias, cert);
    }

    @Override
    public synchronized void replaceTrustStore(String fileName, byte[] fileContent, String filePassword,
                                               String trustType, String trustLocation, String trustPassword) {
        validateTruststoreType(trustType, fileName);
        replaceTrustStore(fileContent, filePassword, trustType, trustLocation, trustPassword);
    }

    @Override
    public void replaceTrustStore(byte[] fileContent, String filePassword, String trustType, String trustLocation, String trustPassword) throws CryptoException {
        LOG.debug("Replacing the existing trust store file [{}] with the provided one.", trustLocation);

        KeyStore truststore = loadTrustStore(fileContent, filePassword);
        try (ByteArrayOutputStream oldTrustStoreBytes = new ByteArrayOutputStream()) {
            truststore.store(oldTrustStoreBytes, trustPassword.toCharArray());
            try (ByteArrayInputStream newTrustStoreBytes = new ByteArrayInputStream(fileContent)) {
                validateLoadOperation(newTrustStoreBytes, filePassword, trustType);
                truststore.load(newTrustStoreBytes, filePassword.toCharArray());
                LOG.debug("Truststore successfully loaded");
                persistTrustStore(truststore, trustPassword, trustLocation);
                LOG.debug("Truststore successfully persisted");
            } catch (CertificateException | NoSuchAlgorithmException | IOException | CryptoException e) {
                LOG.error("Could not replace truststore", e);
                try {
                    truststore.load(oldTrustStoreBytes.toInputStream(), trustPassword.toCharArray());
                } catch (CertificateException | NoSuchAlgorithmException | IOException exc) {
                    throw new CryptoException("Could not replace truststore and old truststore was not reverted properly. Please correct the error before continuing.", exc);
                }
                throw new CryptoException(e);
            }
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException exc) {
            throw new CryptoException("Could not replace truststore", exc);
        }
    }

    @Override
    public KeyStore getTrustStore(String trustStoreLocation, String trustStorePassword) {
        try (InputStream contentStream = Files.newInputStream(Paths.get(trustStoreLocation))) {
            return loadTrustStore(contentStream, trustStorePassword);
        } catch (Exception ex) {
            throw new CryptoException("Exception loading truststore.", ex);
        }
    }

    @Override
    public List<TrustStoreEntry> getTrustStoreEntries(String trustStoreLocation, String trustStorePassword) {
        final KeyStore store = getTrustStore(trustStoreLocation, trustStorePassword);
        return getTrustStoreEntries(store);
    }

    @Override
    public synchronized boolean addCertificate(String trustStorePassword, String trustStoreLocation, byte[] certificateContent, String alias, boolean overwrite) {
        X509Certificate certificate = loadCertificateFromString(new String(certificateContent));
        List<CertificateEntry> certificates = Arrays.asList(new CertificateEntry(alias, certificate));

        KeyStore trustStore = getTrustStore(trustStoreLocation, trustStorePassword);

        return doAddCertificates(trustStore, trustStorePassword, trustStoreLocation, certificates, overwrite);
    }

    @Override
    public boolean addCertificates(KeyStore trustStore, String trustStorePassword, String trustStoreLocation, List<CertificateEntry> certificates, boolean overwrite) {
        return doAddCertificates(trustStore, trustStorePassword, trustStoreLocation, certificates, overwrite);
    }

    @Override
    public boolean removeCertificate(String trustStorePassword, String trustStoreLocation, String alias) {
        KeyStore trustStore = getTrustStore(trustStoreLocation, trustStorePassword);
        List<String> aliases = Arrays.asList(alias);
        return doRemoveCertificates(trustStore, trustStorePassword, trustStoreLocation, aliases);
    }

    @Override
    public boolean removeCertificates(KeyStore trustStore, String trustStorePassword, String trustStoreLocation, List<String> aliases) {
        return doRemoveCertificates(trustStore, trustStorePassword, trustStoreLocation, aliases);
    }

    @Override
    public void validateTruststoreType(String storeType, String storeFileName) {
        String fileType = FilenameUtils.getExtension(storeFileName).toLowerCase();
        switch (storeType.toLowerCase()) {
            case "pkcs12":
                if (Arrays.asList("p12", "pfx").contains(fileType)) {
                    return;
                }
            case "jks":
                if (Arrays.asList("jks").contains(fileType)) {
                    return;
                }
        }
        throw new InvalidParameterException("Store file type (" + fileType + ") should match the configured truststore type (" + storeType + ").");
    }

    protected boolean doAddCertificates(KeyStore trustStore, String trustStorePassword, String trustStoreLocation,
                                        List<CertificateEntry> certificates, boolean overwrite) {
        boolean addedAtLeastOne = false;
        for (CertificateEntry certificateEntry : certificates) {
            boolean added = doAddCertificate(trustStore, certificateEntry.getCertificate(), certificateEntry.getAlias(), overwrite);
            addedAtLeastOne = addedAtLeastOne || added;
        }
        if (addedAtLeastOne) {
            persistTrustStore(trustStore, trustStorePassword, trustStoreLocation);
        }
        return addedAtLeastOne;
    }

    private boolean doRemoveCertificates(KeyStore trustStore, String trustStorePassword, String trustStoreLocation, List<String> aliases) {
        boolean removedAtLeastOne = false;
        for (String alias : aliases) {
            boolean removed = doRemoveCertificate(trustStore, alias);
            removedAtLeastOne = removedAtLeastOne || removed;
        }
        if (removedAtLeastOne) {
            persistTrustStore(trustStore, trustStorePassword, trustStoreLocation);
        }
        return removedAtLeastOne;
    }

    protected boolean doAddCertificate(KeyStore truststore, X509Certificate certificate, String alias, boolean overwrite) {
        boolean containsAlias;
        try {
            containsAlias = truststore.containsAlias(alias);
        } catch (final KeyStoreException e) {
            throw new CryptoException("Error while trying to get the alias from the truststore. This should never happen", e);
        }
        if (containsAlias && !overwrite) {
            return false;
        }
        try {
            if (containsAlias) {
                truststore.deleteEntry(alias);
            }
            truststore.setCertificateEntry(alias, certificate);
            return true;
        } catch (final KeyStoreException e) {
            throw new ConfigurationException(e);
        }
    }

    protected synchronized boolean doRemoveCertificate(KeyStore truststore, String alias) {
        boolean containsAlias;
        try {
            containsAlias = truststore.containsAlias(alias);
        } catch (final KeyStoreException e) {
            throw new CryptoException("Error while trying to get the alias from the truststore. This should never happen", e);
        }
        if (!containsAlias) {
            return false;
        }
        try {
            truststore.deleteEntry(alias);
            return true;
        } catch (final KeyStoreException e) {
            throw new ConfigurationException(e);
        }
    }

    protected KeyStore loadTrustStore(byte[] content, String password) {
        try (InputStream contentStream = new ByteArrayInputStream(content)) {
            return loadTrustStore(contentStream, password);
        } catch (Exception ex) {
            throw new ConfigurationException("Exception loading truststore.", ex);
        }
    }

    protected KeyStore loadTrustStore(InputStream contentStream, String password) {
        KeyStore truststore = null;
        try {
            truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore.load(contentStream, password.toCharArray());
        } catch (Exception ex) {
            throw new ConfigurationException("Exception loading truststore.", ex);
        } finally {
            if (contentStream != null) {
                closeStream(contentStream);
            }
        }
        return truststore;
    }

    protected synchronized void persistTrustStore(KeyStore truststore, String password, String trustStoreLocation) throws CryptoException {
        LOG.debug("TrustStoreLocation is: [{}]", trustStoreLocation);
        File trustStoreFile = createFileWithLocation(trustStoreLocation);
        if (!trustStoreFile.getParentFile().exists()) {
            LOG.debug("Creating directory [" + trustStoreFile.getParentFile() + "]");
            try {
                FileUtils.forceMkdir(trustStoreFile.getParentFile());
            } catch (IOException e) {
                throw new CryptoException("Could not create parent directory for truststore", e);
            }
        }
        // keep old truststore in case it needs to be restored, truststore_name.backup-yyyy-MM-dd_HH_mm_ss.SSS
        backupTrustStore(trustStoreFile);

        LOG.debug("TrustStoreFile is: [{}]", trustStoreFile.getAbsolutePath());
        try (FileOutputStream fileOutputStream = new FileOutputStream(trustStoreFile)) {
            truststore.store(fileOutputStream, password.toCharArray());
        } catch (FileNotFoundException ex) {
            LOG.error("Could not persist truststore:", ex);
            //we address this exception separately
            //we swallow it here because it contains information we do not want to display to the client: the full internal file path of the truststore.
            throw new CryptoException("Could not persist truststore: Is the truststore readonly?");
        } catch (NoSuchAlgorithmException | IOException | CertificateException | KeyStoreException e) {
            throw new CryptoException("Could not persist truststore:", e);
        }
    }

    protected void backupTrustStore(File trustStoreFile) throws CryptoException {
        if (trustStoreFile == null || StringUtils.isEmpty(trustStoreFile.getAbsolutePath())) {
            LOG.warn("Truststore file was null, nothing to backup!");
            return;
        }
        if (!trustStoreFile.exists()) {
            LOG.warn("Truststore file [{}] does not exist, nothing to backup!", trustStoreFile);
            return;
        }

        try {
            backupService.backupFile(trustStoreFile);
        } catch (IOException e) {
            throw new CryptoException("Could not create backup file for truststore", e);
        }
    }

    protected void closeStream(Closeable stream) {
        try {
            LOG.debug("Closing output stream [{}].", stream);
            stream.close();
        } catch (IOException e) {
            LOG.error("Could not close [{}]", stream, e);
        }
    }

    /**
     * Create or update all keystore certificates in the db.
     *
     * @param trustStore the trust store
     * @param keyStore   the key store
     */
    protected void saveCertificateData(KeyStore trustStore, KeyStore keyStore) {
        List<eu.domibus.core.certificate.Certificate> certificates = groupAllKeystoreCertificates(trustStore, keyStore);
        certificateDao.removeUnusedCertificates(certificates);
        for (eu.domibus.core.certificate.Certificate certificate : certificates) {
            certificateDao.saveOrUpdate(certificate);
        }
    }

    /**
     * Load expired and soon expired certificates, add a warning of error log, and save a flag saying the system already
     * notified it for the day.
     */
    protected void logCertificateRevocationWarning() {
        List<eu.domibus.core.certificate.Certificate> unNotifiedSoonRevoked = certificateDao.getUnNotifiedSoonRevoked();
        for (eu.domibus.core.certificate.Certificate certificate : unNotifiedSoonRevoked) {
            LOG.securityWarn(SEC_CERTIFICATE_SOON_REVOKED, certificate.getCertificateType(), certificate.getAlias(), certificate.getNotAfter());
            certificateDao.updateRevocation(certificate);
        }

        List<eu.domibus.core.certificate.Certificate> unNotifiedRevoked = certificateDao.getUnNotifiedRevoked();
        for (eu.domibus.core.certificate.Certificate certificate : unNotifiedRevoked) {
            LOG.securityError(SEC_CERTIFICATE_REVOKED, certificate.getCertificateType(), certificate.getAlias(), certificate.getNotAfter());
            certificateDao.updateRevocation(certificate);
        }
    }

    /**
     * Group keystore and trustStore certificates in a list.
     *
     * @param trustStore the trust store
     * @param keyStore   the key store
     * @return a list of certificate.
     */
    protected List<eu.domibus.core.certificate.Certificate> groupAllKeystoreCertificates(KeyStore trustStore, KeyStore keyStore) {
        List<eu.domibus.core.certificate.Certificate> allCertificates = new ArrayList<>();
        allCertificates.addAll(loadAndEnrichCertificateFromKeystore(trustStore, CertificateType.PUBLIC));
        allCertificates.addAll(loadAndEnrichCertificateFromKeystore(keyStore, CertificateType.PRIVATE));
        return Collections.unmodifiableList(allCertificates);
    }

    /**
     * Load certificate from a keystore and enrich them with status and type.
     *
     * @param keyStore        the store where to retrieve the certificates.
     * @param certificateType the type of the certificate (Public/Private)
     * @return the list of certificates.
     */
    private List<eu.domibus.core.certificate.Certificate> loadAndEnrichCertificateFromKeystore(KeyStore keyStore, CertificateType certificateType) {
        List<eu.domibus.core.certificate.Certificate> certificates = new ArrayList<>();
        if (keyStore != null) {
            certificates = extractCertificateFromKeyStore(
                    keyStore);
            for (eu.domibus.core.certificate.Certificate certificate : certificates) {
                certificate.setCertificateType(certificateType);
                CertificateStatus certificateStatus = getCertificateStatus(certificate.getNotAfter());
                certificate.setCertificateStatus(certificateStatus);
            }
        }
        return certificates;
    }

    /**
     * Process the certificate status base on its expiration date.
     *
     * @param notAfter the expiration date of the certificate.
     * @return the certificate status.
     */
    protected CertificateStatus getCertificateStatus(Date notAfter) {
        int revocationOffsetInDays = domibusPropertyProvider.getIntegerProperty(REVOCATION_TRIGGER_OFFSET_PROPERTY);
        LOG.debug("Property [{}], value [{}]", REVOCATION_TRIGGER_OFFSET_PROPERTY, revocationOffsetInDays);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime offsetDate = now.plusDays(revocationOffsetInDays);
        LocalDateTime certificateEnd = LocalDateTime.fromDateFields(notAfter);
        LOG.debug("Current date[{}], offset date[{}], certificate end date:[{}]", now, offsetDate, certificateEnd);
        if (now.isAfter(certificateEnd)) {
            return CertificateStatus.REVOKED;
        } else if (offsetDate.isAfter(certificateEnd)) {
            return CertificateStatus.SOON_REVOKED;
        }
        return CertificateStatus.OK;
    }

    protected List<eu.domibus.core.certificate.Certificate> extractCertificateFromKeyStore(KeyStore trustStore) {
        List<eu.domibus.core.certificate.Certificate> certificates = new ArrayList<>();
        try {
            final Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final X509Certificate x509Certificate = (X509Certificate) trustStore.getCertificate(alias);
                eu.domibus.core.certificate.Certificate certificate = new eu.domibus.core.certificate.Certificate();
                certificate.setAlias(alias);
                certificate.setNotAfter(x509Certificate.getNotAfter());
                certificate.setNotBefore(x509Certificate.getNotBefore());
                certificates.add(certificate);
            }
        } catch (KeyStoreException e) {
            LOG.warn(e.getMessage(), e);
        }
        return Collections.unmodifiableList(certificates);
    }

    protected void sendCertificateExpiredAlerts() {
        final ExpiredCertificateModuleConfiguration configuration = expiredCertificateConfigurationManager.getConfiguration();
        final boolean activeModule = configuration.isActive();
        LOG.debug("Certificate expired alert module activated:[{}]", activeModule);
        if (!activeModule) {
            LOG.info("Certificate Expired Module is not active; returning.");
            return;
        }
        final String accessPoint = getAccessPointName();
        final Integer revokedDuration = configuration.getExpiredDuration();
        final Integer revokedFrequency = configuration.getExpiredFrequency();

        Date endNotification = LocalDateTime.now().minusDays(revokedDuration).toDate();
        Date notificationDate = LocalDateTime.now().minusDays(revokedFrequency).toDate();

        LOG.debug("Searching for expired certificate with notification date smaller than:[{}] and expiration date > current date - offset[{}]->[{}]", notificationDate, revokedDuration, endNotification);
        certificateDao.findExpiredToNotifyAsAlert(notificationDate, endNotification).forEach(certificate -> {
            certificate.setAlertExpiredNotificationDate(LocalDateTime.now().withTime(0, 0, 0, 0).toDate());
            certificateDao.saveOrUpdate(certificate);
            final String alias = certificate.getAlias();
            final String accessPointOrAlias = accessPoint == null ? alias : accessPoint;
            eventService.enqueueCertificateExpiredEvent(accessPointOrAlias, alias, certificate.getNotAfter());
        });
    }

    private String getAccessPointName() {
        String partyName = null;
        if (pModeProvider.isConfigurationLoaded()) {
            partyName = pModeProvider.getGatewayParty().getName();
        }
        return partyName;
    }

    protected boolean checkValidity(X509Certificate cert) {
        boolean result = false;
        try {
            cert.checkValidity();
            result = true;
        } catch (Exception e) {
            LOG.warn("Certificate is not valid " + cert, e);
        }

        return result;
    }

    protected void sendCertificateImminentExpirationAlerts() {
        final ImminentExpirationCertificateModuleConfiguration configuration = imminentExpirationCertificateConfigurationManager.getConfiguration();
        final Boolean activeModule = configuration.isActive();
        LOG.debug("Certificate Imminent expiration alert module activated:[{}]", activeModule);
        if (BooleanUtils.isNotTrue(activeModule)) {
            LOG.info("Imminent Expiration Certificate Module is not active; returning.");
            return;
        }
        final String accessPoint = getAccessPointName();
        final Integer imminentExpirationDelay = configuration.getImminentExpirationDelay();
        final Integer imminentExpirationFrequency = configuration.getImminentExpirationFrequency();

        final Date today = LocalDateTime.now().withTime(0, 0, 0, 0).toDate();
        final Date maxDate = LocalDateTime.now().plusDays(imminentExpirationDelay).toDate();
        final Date notificationDate = LocalDateTime.now().minusDays(imminentExpirationFrequency).toDate();

        LOG.debug("Searching for certificate about to expire with notification date smaller than:[{}] and expiration date between current date and current date + offset[{}]->[{}]",
                notificationDate, imminentExpirationDelay, maxDate);
        certificateDao.findImminentExpirationToNotifyAsAlert(notificationDate, today, maxDate).forEach(certificate -> {
            certificate.setAlertImminentNotificationDate(today);
            certificateDao.saveOrUpdate(certificate);
            final String alias = certificate.getAlias();
            final String accessPointOrAlias = accessPoint == null ? alias : accessPoint;
            eventService.enqueueImminentCertificateExpirationEvent(accessPointOrAlias, alias, certificate.getNotAfter());
        });
    }

    protected X509Certificate[] getCertificateChain(KeyStore trustStore, String alias) throws KeyStoreException {
        //TODO get the certificate chain manually based on the issued by info from the original certificate
        final java.security.cert.Certificate[] certificateChain = trustStore.getCertificateChain(alias);
        if (certificateChain == null) {
            X509Certificate certificate = (X509Certificate) trustStore.getCertificate(alias);
            return new X509Certificate[]{certificate};
        }
        return Arrays.copyOf(certificateChain, certificateChain.length, X509Certificate[].class);

    }

    protected File createFileWithLocation(String location) {
        return new File(location);
    }

    protected boolean isPemFormat(String content) {
        return StringUtils.startsWith(StringUtils.trim(content), "-----BEGIN CERTIFICATE-----");
    }

    private TrustStoreEntry createTrustStoreEntry(String alias, final X509Certificate certificate) {
        if (certificate == null)
            return null;
        TrustStoreEntry entry = new TrustStoreEntry(
                alias,
                certificate.getSubjectDN().getName(),
                certificate.getIssuerDN().getName(),
                certificate.getNotBefore(),
                certificate.getNotAfter());
        entry.setFingerprints(extractFingerprints(certificate));
        return entry;
    }

    private String extractFingerprints(final X509Certificate certificate) {
        if (certificate == null)
            return null;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new DomibusCertificateException("Could not initialize MessageDigest", e);
        }
        byte[] der;
        try {
            der = certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new DomibusCertificateException("Could not encode certificate", e);
        }
        md.update(der);
        byte[] digest = md.digest();
        String digestHex = DatatypeConverter.printHexBinary(digest);
        return digestHex.toLowerCase();
    }
}


