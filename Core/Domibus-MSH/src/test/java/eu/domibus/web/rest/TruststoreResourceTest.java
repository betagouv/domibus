package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.*;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.api.util.MultiPartFileUtil;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.certificate.CertificateHelper;
import eu.domibus.core.converter.PartyCoreMapper;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.web.rest.error.ErrorHandlerService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class TruststoreResourceTest {

    @Tested
    TruststoreResource truststoreResource;

    @Injectable
    MultiDomainCryptoService multiDomainCertificateProvider;

    @Injectable
    protected DomainContextProvider domainProvider;

    @Injectable
    CertificateService certificateService;

    @Injectable
    PartyCoreMapper partyCoreConverter;

    @Injectable
    CsvServiceImpl csvServiceImpl;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Injectable
    MultiPartFileUtil multiPartFileUtil;

    @Injectable
    private AuditService auditService;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    KeystorePersistenceService keystorePersistenceService;

    @Injectable
    CertificateHelper certificateHelper;

    @Injectable
    SecurityProfileService securityProfileService;

    @Test
    public void replaceTruststore(@Injectable Domain domain, @Injectable KeyStoreContentInfo storeInfo) {
        new Expectations() {{
            domainProvider.getCurrentDomain();
            result = domain;
        }};

        // When
        truststoreResource.doUploadStore(storeInfo);

        new Verifications() {{
            multiDomainCertificateProvider.replaceTrustStore(domainProvider.getCurrentDomain(), storeInfo);
        }};
    }

    @Test
    public void getTrustStoreEntries() {
        List<TrustStoreEntry> trustStoreEntries = new ArrayList<>();
        new Expectations() {{
            multiDomainCertificateProvider.getTrustStoreEntries(domainProvider.getCurrentDomain());
            result = trustStoreEntries;
        }};

        List<TrustStoreEntry> res = truststoreResource.doGetStoreEntries();

        Assertions.assertEquals(trustStoreEntries, res);
    }

}
