package eu.domibus.core.property.listeners;

import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.core.multitenancy.DomibusDomainException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMAIN_TITLE;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Handles the change of domain title property
 */
@Service
public class DomainTitleChangeListener implements DomibusPropertyChangeListener {

    private final DomainService domainService;

    private final DomibusConfigurationService domibusConfigurationService;

    public DomainTitleChangeListener(DomainService domainService, DomibusConfigurationService domibusConfigurationService) {
        this.domainService = domainService;
        this.domibusConfigurationService = domibusConfigurationService;
    }

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsIgnoreCase(propertyName, DOMAIN_TITLE);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {

        if(domibusConfigurationService.isSingleTenantAware()){
            throw new DomibusDomainException("Cannot change domain title in single tenancy configuration.");
        }
        this.domainService.refreshDomain(domainCode);
    }
}
