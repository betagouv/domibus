package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.multitenancy.DynamicDomainManagementService;
import eu.domibus.core.multitenancy.dao.DomainDao;
import eu.domibus.web.rest.ro.DomainRO;
import eu.domibus.web.security.AuthenticationService;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
public class DomainResourceTest {

    @Tested
    private DomainResource domainsResource;

    @Injectable
    private DomibusCoreMapper coreMapper;

    @Injectable
    private UserDomainService userDomainService;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DynamicDomainManagementService dynamicDomainManagementService;

    @Injectable
    private DomainDao domainDao;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    private AuthenticationService authenticationService;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Test
    public void testGetDomains_IgnoringActiveFlag(){
        List<Domain> domainEntries= new ArrayList<>();
        List<DomainRO> domainROEntries = new ArrayList<>();
        // GIVEN
        final Boolean activeFlag = null;
        new Expectations() {{
            domainService.getAllValidDomains();
            result = domainEntries;

            coreMapper.domainListToDomainROList(domainEntries);
            result = domainROEntries;
        }};

        // WHEN
        List<DomainRO> result = domainsResource.getDomains(activeFlag);

        // THEN
        new FullVerifications() { /* no unexpected interactions */
        };
        assertEquals(domainROEntries, result);
    }

    @Test
    public void testGetDomains_ActiveFlagWhenNoUserDetails() {
        // GIVEN
        new Expectations() {{
            domainService.getDomains();
            result = new ArrayList<>();
        }};

        // WHEN
        domainsResource.getDomains(true);

        // THEN
        new FullVerifications() {{
            coreMapper.domainListToDomainROList(new ArrayList<>());
        }};
    }

}
