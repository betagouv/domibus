package eu.domibus.core.property.listeners;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pki.CertificateInitValueType;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.core.property.GatewayConfigurationValidator;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Arrays;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_KEYSTORE_LOCATION;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SECURITY_KEY_PRIVATE_ALIAS;

/**
 * @author Ion Perpegel
 * @since 5.0
 * <p>
 * Handles the change of DOMIBUS_SECURITY_KEYSTORE_LOCATION property
 */
@Service
public class KeystorePropertiesChangeListener implements DomibusPropertyChangeListener {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(KeystorePropertiesChangeListener.class);

    private final MultiDomainCryptoService multiDomainCryptoService;

    private final DomainService domainService;

    private final GatewayConfigurationValidator gatewayConfigurationValidator;

    public KeystorePropertiesChangeListener(MultiDomainCryptoService multiDomainCryptoService,
                                            DomainService domainService,
                                            GatewayConfigurationValidator gatewayConfigurationValidator) {
        this.multiDomainCryptoService = multiDomainCryptoService;
        this.domainService = domainService;
        this.gatewayConfigurationValidator = gatewayConfigurationValidator;
    }

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsIgnoreCase(propertyName, DOMIBUS_SECURITY_KEYSTORE_LOCATION)
                || StringUtils.equalsIgnoreCase(propertyName, DOMIBUS_SECURITY_KEY_PRIVATE_ALIAS);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        LOG.debug("[{}] property has changed for domain [{}].", propertyName, domainCode);

        Domain domain = domainService.getDomain(domainCode);

        if (StringUtils.equalsIgnoreCase(propertyName, DOMIBUS_SECURITY_KEYSTORE_LOCATION)) {
            multiDomainCryptoService.replaceKeyStore(domain, propertyValue);
            multiDomainCryptoService.reset(domain, Arrays.asList(CertificateInitValueType.KEYSTORE));
            gatewayConfigurationValidator.validateCertificates();
        } else {
            multiDomainCryptoService.reset(domain, Arrays.asList(CertificateInitValueType.KEYSTORE)); // ??
        }
    }

}
