package eu.domibus.core.property.listeners;

import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private DomainService domainService;

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsIgnoreCase(propertyName, DOMAIN_TITLE);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {

        if ((StringUtils.length(propertyValue) > DOMAIN_TITLE_MAX_LENGTH)) {
            throw new DomibusPropertyException(String.format("Cannot change domain title to [%s] because it is greater than the maximum allowed length [%s].", propertyValue, DOMAIN_TITLE_MAX_LENGTH));
        }


        if(domibusConfigurationService.isSingleTenantAware()){
            throw new DomibusDomainException("Cannot change domain title in single tenancy configuration.");
        }
        this.domainService.refreshDomain(domainCode);
    }
}
