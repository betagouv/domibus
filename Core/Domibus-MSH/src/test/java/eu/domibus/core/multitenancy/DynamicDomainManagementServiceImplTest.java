package eu.domibus.core.multitenancy;

import eu.domibus.api.cluster.SignalService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainsAware;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DbSchemaUtil;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.multitenancy.dao.DomainDao;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainsAwareExt;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@SuppressWarnings("DataFlowIssue")
@ExtendWith(JMockitExtension.class)
public class DynamicDomainManagementServiceImplTest {

    DynamicDomainManagementServiceImpl dynamicDomainManagementService;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomainDao domainDao;

    @Injectable
    private SignalService signalService;

    private final List<DomainsAware> domainsAwareList = new ArrayList<>();

    private final List<DomainsAwareExt> externalDomainsAwareList = new ArrayList<>();

    @Injectable
    private DomibusCoreMapper coreMapper;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DbSchemaUtil dbSchemaUtil;

    @Injectable
    BackendConnectorService backendConnectorService;

    List<Domain> domains, allDomains;
    Domain domain1, domain2;

    {
        domain1 = new Domain("domain1", "domain1");
        domain2 = new Domain("domain2", "domain2");
        domains = new ArrayList<>();
        domains.add(domain1);
        allDomains = Arrays.asList(domain1, domain2);


        externalDomainsAwareList.add(new DomainsAwareExt() {
            @Override
            public void onDomainAdded(DomainDTO domain) {

            }

            @Override
            public void onDomainRemoved(DomainDTO domain) {

            }
        });
    }

    @BeforeEach
    void setUp() {
        dynamicDomainManagementService = new DynamicDomainManagementServiceImpl(domainService, domibusPropertyProvider, domainDao, signalService, domainsAwareList, externalDomainsAwareList, coreMapper, domibusConfigurationService, backendConnectorService);
    }

    @Test
    void validateAdditionInvalidName() {
        new Expectations() {{
            domainService.getDomains();
            result = domains;
            domainDao.findAll();
            result = allDomains;
        }};

        Assertions.assertThrows(DomibusDomainException. class,() -> dynamicDomainManagementService.validateAddition("domain3"));
    }

    @Test
    void validateAdditionAlreadyAdded() {
        new Expectations() {{
            domainService.getDomains();
            result = domains;
        }};

        Assertions.assertThrows(DomibusDomainException.class, () -> dynamicDomainManagementService.validateAddition("domain1"));
    }

    @Test
    public void internalAddDomain() throws Exception {
        new Expectations() {{
            domibusPropertyProvider.loadProperties((Domain) any);
        }};

        dynamicDomainManagementService.internalAddDomain(domain2);

        new Verifications() {{
            domainService.addDomain(domain2);
            dynamicDomainManagementService.notifyInternalBeansOfAddition(domain2);
        }};
    }

    @SuppressWarnings("unchecked")
    @Test
    void internalAddDomainError() throws Exception {
        domainsAwareList.add(new DomainsAware() {
            @Override
            public void onDomainAdded(Domain domain) {
                throw new DomibusCertificateException();
            }

            @Override
            public void onDomainRemoved(Domain domain) {

            }
        });

        new Expectations(dynamicDomainManagementService) {{
            domibusPropertyProvider.loadProperties((Domain) any);
        }};

        Assertions.assertThrows(DomibusDomainException. class,() -> dynamicDomainManagementService.internalAddDomain(domain2));

        Assertions.assertFalse(domainService.getDomains().contains(domain2));

        new Verifications() {{
            dynamicDomainManagementService.handleAddDomainException(domain2, (List<DomainsAware>) any, (DomainsAware) any, (Exception) any);
        }};
    }

    @Test
    public void notifyExternalModules() {
        DomainDTO domain1Dto = new DomainDTO(domain1.getCode(), domain1.getName());
        new Expectations() {{
            coreMapper.domainToDomainDTO(domain1);
            result = domain1Dto;
        }};

        dynamicDomainManagementService.notifyExternalModulesOfAddition(domain1);

        new Verifications() {{
            externalDomainsAwareList.get(0).onDomainAdded(domain1Dto);
        }};
    }

}
