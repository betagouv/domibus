package eu.domibus.plugin.fs.worker;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.plugin.fs.property.FSPluginProperties;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.plugin.fs.worker.FSSendMessagesService.DEFAULT_DOMAIN;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class FSDomainServiceTest {

    @Injectable
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    @Injectable
    protected DomainExtService domainExtService;

    @Injectable
    protected FSPluginProperties fsPluginProperties;

    @Injectable
    protected DomainContextExtService domainContextExtService;

    @Tested
    FSDomainService fsDomainService;

    @Test
    public void testGetFSPluginDomainNonMultitenanncy() {

        new Expectations(fsDomainService) {{
            domainContextExtService.getCurrentDomain().getCode();
            result = DEFAULT_DOMAIN;
        }};

        final String fsPluginDomain = fsDomainService.getFSPluginDomain();
        Assertions.assertEquals(FSSendMessagesService.DEFAULT_DOMAIN, fsPluginDomain);
    }

    @Test
    public void testGetFSPluginDomainMultitenanncy() {
        final String mydomain = "mydomain";

        new Expectations() {{
            domainContextExtService.getCurrentDomain().getCode();
            result = mydomain;
        }};

        final String fsPluginDomain = fsDomainService.getFSPluginDomain();
        Assertions.assertEquals(mydomain, fsPluginDomain);
    }

    @Test
    public void fsDomainToDomibusDomainNonMultitenancyMode() {
        String fsPluginDomain = "myDomain";

        new Expectations() {{
            domibusConfigurationExtService.isMultiTenantAware();
            result = false;

            domainExtService.getDomain("default");
            result = new DomainDTO("default", "default");
        }};

        final DomainDTO domainDTO = fsDomainService.fsDomainToDomibusDomain(fsPluginDomain);
        Assertions.assertEquals(FSSendMessagesService.DEFAULT_DOMAIN, domainDTO.getCode());

        new Verifications() {{
            domainExtService.getDomain(fsPluginDomain);
            times = 0;
        }};
    }

    @Test
    public void fsDomainToDomibusDomainMultitenancyMode() {
        String fsPluginDomain = "myDomain";

        new Expectations() {{
            domibusConfigurationExtService.isMultiTenantAware();
            result = true;

            domainExtService.getDomain(fsPluginDomain);
            result = new DomainDTO(fsPluginDomain, fsPluginDomain);
        }};

        final DomainDTO domainDTO = fsDomainService.fsDomainToDomibusDomain(fsPluginDomain);
        Assertions.assertEquals(fsPluginDomain, domainDTO.getCode());

        new Verifications() {{
            domainExtService.getDomain("default");
            times = 0;
        }};
    }
}
