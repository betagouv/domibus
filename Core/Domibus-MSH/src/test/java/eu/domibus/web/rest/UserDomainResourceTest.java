package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.multitenancy.DynamicDomainManagementService;
import eu.domibus.core.multitenancy.dao.DomainDao;
import eu.domibus.web.rest.ro.DomainRO;
import eu.domibus.web.security.DomibusUserDetailsImpl;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JMockitExtension.class)
public class UserDomainResourceTest {

    @Tested
    private UserDomainResource domainsResource;

    @Injectable
    private DomibusCoreMapper coreMapper;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DynamicDomainManagementService dynamicDomainManagementService;

    @Injectable
    private DomainDao domainDao;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Test
    public void testGetDomains_ActiveFlagWhenNoUserDetails() {
        // GIVEN
        new Expectations() {{
            authUtils.getUserDetails();
            result = null;
        }};

        // WHEN
        domainsResource.getDomains();

        // THEN
        new FullVerifications() {};
    }


    @Test
    public void testGetDomains(@Injectable DomibusUserDetailsImpl userDetails) {
        final Domain red = new Domain("red", "Red");
        final Domain yellow = new Domain("yellow", "Yellow");
        final Domain blue = new Domain("blue", "Blue");
        List<DomainRO> domainROEntries = new ArrayList<>();
        // GIVEN
        new Expectations() {{
            authUtils.getUserDetails();
            result = userDetails;

            userDetails.getAvailableDomainCodes();
            result = new HashSet<>(Arrays.asList("red", "yellow", "blue"));

            domainService.getDomain("red");
            result = red;
            domainService.getDomain("yellow");
            result = yellow;
            domainService.getDomain("blue");
            result = blue;

            coreMapper.domainListToDomainROList(with(
                    //"The argument list can contain domains in any order"
                            new Delegate<List<Domain>>() {
                                @SuppressWarnings({"unused", "ProtectedMemberInFinalClass"})
                                protected boolean matchesSafely(List<Domain> domains) {
                                    return CollectionUtils.containsAll(domains,
                                            Arrays.asList(red, yellow, blue));
                                }
                            }
                    )
            );
            result = domainROEntries;
        }};

        // WHEN
        final List<DomainRO> result = domainsResource.getDomains();

        // THEN
        new FullVerifications() { /* no unexpected interactions */
        };
        assertEquals(domainROEntries, result);
    }
}
