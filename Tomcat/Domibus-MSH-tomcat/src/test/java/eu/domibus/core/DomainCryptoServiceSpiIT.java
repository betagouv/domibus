package eu.domibus.core;

import eu.domibus.AbstractIT;
import eu.domibus.core.crypto.spi.DomainCryptoServiceSpi;
import eu.domibus.core.crypto.spi.DomainSpi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author François Gautier
 * @since 5.1
 */

public class DomainCryptoServiceSpiIT extends AbstractIT {

    @Autowired
    private DomainCryptoServiceSpi domainCryptoServiceSpi;

    @Test
    public void domainCryptoServiceSpi_init() {

        Assertions.assertNull(domainCryptoServiceSpi.getKeyStore());
        Assertions.assertNull(domainCryptoServiceSpi.getTrustStore());
        domainCryptoServiceSpi.setDomain(new DomainSpi("default", "default"));
        domainCryptoServiceSpi.init();
        Assertions.assertNotNull(domainCryptoServiceSpi.getKeyStore());
        Assertions.assertNotNull(domainCryptoServiceSpi.getTrustStore());
    }
}
