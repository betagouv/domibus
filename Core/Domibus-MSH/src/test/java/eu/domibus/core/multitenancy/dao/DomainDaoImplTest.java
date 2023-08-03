package eu.domibus.core.multitenancy.dao;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMAIN_TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
public class DomainDaoImplTest {

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Tested
    DomainDaoImpl domainDao;

    @Test
    public void findAll() {
        new Expectations() {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;
        }};

        new Expectations(domainDao) {{
            domainDao.findAllDomainCodes();
            result = Arrays.asList("zdomain", "adomain");
            domainDao.checkValidDomain((List<Domain>) any, anyString);
            domainDao.getDomainTitle((Domain) any);
            result = "adomain";
            domainDao.getDomainTitle((Domain) any);
            result = "zdomain";
        }};

        List<Domain> domains = domainDao.findAll();

        assertEquals(2, domains.size());
        assertEquals("zdomain", domains.get(0).getCode());
        assertEquals("adomain", domains.get(1).getCode());
    }

    @Test
    public void findAllDomainCodes() throws IOException {
        new Expectations() {{
            domibusConfigurationService.getConfigLocation();
            result = "src/test/resources/config";
        }};

        Path emptyDomainDir = Files.createTempDirectory(Paths.get(domibusConfigurationService.getConfigLocation()), "emptyDomainDir");

        List<String> domainCodes = domainDao.findAllDomainCodes();

        Files.deleteIfExists(emptyDomainDir);

        assertEquals(2, domainCodes.size());
        assertEquals("default", domainCodes.get(0));
        assertEquals("domain_name", domainCodes.get(1));
    }

    @Test
    public void testValidateDomain_InvalidDomain(@Injectable Domain domain) {

        final String domainCode = "Domain&7";
        List<Domain> domains = new ArrayList<>();

        try {
            domainDao.checkValidDomain(domains, domainCode);
            Assertions.fail();
        } catch (DomibusCoreException ex) {
            assertEquals(ex.getError(), DomibusCoreErrorCode.DOM_001);
            assertEquals(ex.getMessage(), "[DOM_001]:Forbidden characters like capital letters or special characters, except underscore found in domain name. Invalid domain name:Domain&7");
        }
    }

    @Test
    public void testValidateDomain_DuplicateDomain(@Injectable Domain domain) {

        final String domainCode1 = "domaina";
        final String domainCode = "domaina";
        List<Domain> domains = new ArrayList<>();
        Domain domain1 = new Domain(domainCode1, null);
        domains.add(domain1);
        try {
            domainDao.checkValidDomain(domains, domainCode);
            Assertions.fail();
        } catch (DomibusCoreException ex) {
            assertEquals(ex.getError(), DomibusCoreErrorCode.DOM_001);
            assertEquals(ex.getMessage(), "[DOM_001]:Found duplicate domain name :domaina");
        }
    }

    @Test
    public void testValidateDomain_ValidDomain() {

        final String domainCode = "domain1";
        List<Domain> domains = new ArrayList<>();

        new Expectations(domainDao) {{
            domainDao.checkConfigFile(domainCode);
        }};

        domainDao.checkValidDomain(domains, domainCode);
    }


    @Test
    public void testValidateDomainStartsWithNumber() {

        final String domainCode = "1domain22";
        List<Domain> domains = new ArrayList<>();

        try {
            domainDao.checkValidDomain(domains, domainCode);
            Assertions.fail();
        } catch (DomibusCoreException ex) {
            assertEquals(ex.getError(), DomibusCoreErrorCode.DOM_001);
            assertEquals(ex.getMessage(), "[DOM_001]:Domain name should not start with a number. Invalid domain name:1domain22");
        }
    }

    @Test
    public void getDomainTitle(@Injectable Domain domain) {
        String domainTitle = StringUtils.repeat("X", 52);
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMAIN_TITLE);
            result = domainTitle;
        }};
        String newDomainTitle = domainDao.getDomainTitle(domain);
        Assertions.assertEquals(newDomainTitle.length(),50);
    }
}
