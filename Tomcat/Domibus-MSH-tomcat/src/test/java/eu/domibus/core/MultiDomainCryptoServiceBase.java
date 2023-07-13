package eu.domibus.core;

import eu.domibus.AbstractIT;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.pki.KeystorePersistenceService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.certificate.Certificate;
import eu.domibus.core.certificate.CertificateDaoImpl;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.certificate.CertificateServiceImpl;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.core.crypto.TruststoreDao;
import eu.domibus.core.util.SecurityUtilImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_TRUSTSTORE_LOCATION;
import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author Ion Perpegel
 * @since 5.0
 */

public class MultiDomainCryptoServiceBase extends AbstractIT {
    public static final String KEYSTORES = "keystores";

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MultiDomainCryptoServiceBase.class);

    @Autowired
    MultiDomainCryptoServiceImpl multiDomainCryptoService;

    @Autowired
    TruststoreDao truststoreDao;

    @Autowired
    CertificateServiceImpl certificateService;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    CertificateDaoImpl certificateDao;

    @Autowired
    CertificateHelper certificateHelper;

    @Autowired
    KeystorePersistenceService keystorePersistenceService;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    KeystorePersistenceService getKeystorePersistenceService;

    @Autowired
    SecurityUtilImpl securityUtil;

    Domain domain;
    String location, back;

    @BeforeEach
    public void doBefore() {
        domain = DomainService.DEFAULT_DOMAIN;

        final LocalDateTime localDateTime = LocalDateTime.of(0, 1, 1, 0, 0);
        final LocalDateTime offset = localDateTime.minusDays(15);
        final LocalDateTime notification = localDateTime.minusDays(7);
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        List<Certificate> certs2 = certificateDao.findExpiredToNotifyAsAlert(getDate(notification), getDate(offset));
        certificateDao.deleteAll(certs2);

        resetInitialTruststore();

        // back-up initial trust file
        location = domibusPropertyProvider.getProperty(DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
        back = location.replace("gateway_truststore.jks", "gateway_truststore_back.jks");
        try {
            Files.copy(Paths.get(location), Paths.get(back), REPLACE_EXISTING);
        } catch (IOException e) {

            LOG.error("Error backing trustStore file", e);
        }
    }

    @BeforeEach
    public void doAfter() {
        //restore initial trust store
        if (back != null && location != null) {
            try {
                Files.copy(Paths.get(back), Paths.get(location), REPLACE_EXISTING);
                Files.delete(Paths.get(back));
            } catch (IOException e) {
                LOG.error("Error restoring trsustore file", e);
            }
        }
    }

    Path getPath(String newStoreName) {
        return Paths.get(domibusConfigurationService.getConfigLocation(), "domains", domain.getCode(), KEYSTORES, newStoreName);
    }


    void removeCertificate(String name) {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        boolean removed = multiDomainCryptoService.removeCertificate(domainContextProvider.getCurrentDomain(), name);
        Assertions.assertTrue(removed);
    }

    void addCertificate(String alias) {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        String certFileName = "red_gw.cer";
        addCertificate(alias, certFileName);
    }

    void addCertificate(String alias, String certFileName) {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        try {
            Path path = getPath(certFileName);
            byte[] content = Files.readAllBytes(path);
            X509Certificate x509Certificate = certificateService.loadCertificate(Base64.getEncoder().encodeToString(content));
            boolean added = multiDomainCryptoService.addCertificate(domainContextProvider.getCurrentDomain(), x509Certificate, alias, true);
            Assertions.assertTrue(added);
            checkDiskFileinSyncWithMemoryStore();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void resetInitialTruststore() {
        try {
            String storePassword = "test123";

            String fileName = "gateway_truststore_original.jks";
            Path path = getPath(fileName);
            byte[] content = Files.readAllBytes(path);
            KeyStoreContentInfo storeInfo = certificateHelper.createStoreContentInfo(DOMIBUS_TRUSTSTORE_NAME, "gateway_truststore.jks", content, storePassword);
            multiDomainCryptoService.replaceTrustStore(domain, storeInfo);
        } catch (Exception e) {
            LOG.info("Error restoring initial keystore", e);
        }
    }

    private Date getDate(LocalDateTime localDateTime1) {
        return Date.from(localDateTime1.atZone(ZoneOffset.UTC).toInstant());
    }

    void runThreads(List<Runnable> tasks) {
        List<Thread> threads = new ArrayList<>();

        for (Runnable task : tasks) {
            Thread t = new Thread(task);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                LOG.error("Thread join stopped", e);
            }
        }

    }

    void doCheckDiskFileinSyncWithMemoryStore(boolean inSync) {
        KeyStoreContentInfo info = getKeystorePersistenceService.loadStore(getKeystorePersistenceService.getTrustStorePersistenceInfo());
        KeyStore storeOnDisk = certificateService.loadStore(info);
        KeyStore memoryStore = multiDomainCryptoService.getTrustStore(domain);
        Assertions.assertEquals(securityUtil.areKeystoresIdentical(memoryStore, storeOnDisk), inSync);
    }

    void checkDiskFileinSyncWithMemoryStore() {
        doCheckDiskFileinSyncWithMemoryStore(true);
    }

    void checkDiskFileinSyncWithMemoryStore(boolean inSync) {
        doCheckDiskFileinSyncWithMemoryStore(inSync);
    }
}
