package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.property.DomibusVersionService;
import eu.domibus.web.rest.ro.DomibusInfoRO;
import eu.domibus.web.rest.ro.SupportTeamInfoRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Tiago Miguel, Catalin Enache
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class ApplicationResourceTest {

    private static final String DOMIBUS_VERSION = "Domibus Unit Tests";
    private static final String DOMIBUS_CUSTOMIZED_NAME = "Domibus Customized Name";

    @Tested
    ApplicationResource applicationResource;

    @Injectable
    DomibusVersionService domibusVersionService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomainService domainService;

    @Injectable
    DomibusCoreMapper coreMapper;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    AuthUtils authUtils;

    @Injectable
    DomainTaskExecutor domainTaskExecutor;

    @Injectable
    DomibusLocalCacheService domibusLocalCacheService;

    @Test
    public void testGetDomibusInfo() {
        // Given
        new Expectations() {{
            domibusVersionService.getDisplayVersion();
            result = DOMIBUS_VERSION;
        }};

        // When
        DomibusInfoRO domibusInfo = applicationResource.getDomibusInfo();

        // Then
        Assertions.assertNotNull(domibusInfo);
        Assertions.assertEquals(DOMIBUS_VERSION, domibusInfo.getVersion());
    }

    public void testDomibusName(String name) {
        // Given
        new Expectations(applicationResource) {{
            domibusPropertyProvider.getProperty(DomainService.DEFAULT_DOMAIN, ApplicationResource.DOMIBUS_CUSTOM_NAME);
            result = name;
        }};

        // When
        final String domibusName = applicationResource.getDomibusName();

        // Then
        Assertions.assertEquals(name, domibusName);
    }

    @Test
    public void testGetDomibusCustomName() {
        testDomibusName(DOMIBUS_CUSTOMIZED_NAME);
    }

    @Test
    public void testGetDomibusDefaultName() {
        testDomibusName("Domibus");
    }

    @Test
    public void testGetMultiTenancy() {
        // Given
        new Expectations(applicationResource) {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;
        }};

        // When
        final Boolean isMultiTenancy = applicationResource.getMultiTenancy();

        // Then
        Assertions.assertEquals(true, isMultiTenancy);
    }

    @Test
    public void testGetSupportTeamInfo() {
        final String supportTeamName = "The Avengers";
        final String supportTeamEmail = "ironman@avengers.com";
        new Expectations() {{
            domibusPropertyProvider.getProperty(ApplicationResource.SUPPORT_TEAM_NAME_KEY);
            result = supportTeamName;

            domibusPropertyProvider.getProperty(ApplicationResource.SUPPORT_TEAM_EMAIL_KEY);
            result = supportTeamEmail;
        }};

        //tested method
        SupportTeamInfoRO supportTeamInfoRO = applicationResource.getSupportTeamInfo();

        Assertions.assertNotNull(supportTeamInfoRO);
        Assertions.assertEquals(supportTeamName, supportTeamInfoRO.getName());
        Assertions.assertEquals(supportTeamEmail, supportTeamInfoRO.getEmail());
    }
}
