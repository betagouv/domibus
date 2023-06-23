package eu.domibus.core.crypto;

import com.google.common.collect.Lists;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.crypto.spi.DomainCryptoServiceSpi;
import eu.domibus.core.crypto.spi.DomainSpi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */

@ExtendWith(MockitoExtension.class)
public class DomainCryptoServiceImplTest {

    @Mock
    private DomibusPropertyProvider domibusPropertyProvider;

    @Mock
    private eu.domibus.api.multitenancy.Domain domain;

    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private DomainCryptoServiceImpl domainCryptoService;

    @BeforeEach
    public void setup() {
        
    }

    @Test
    public void init() {
        final String dss = "DSS";
        final DomainCryptoServiceSpi defaultSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        final DomainCryptoServiceSpi dssSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        when(defaultSpi.getIdentifier()).thenReturn("DEFAULT");
        when(dssSpi.getIdentifier()).thenReturn(dss);
        when(domain.getCode()).thenReturn("DEF");
        when(domain.getName()).thenReturn("DEFAULT");
        domainCryptoService.setDomainCryptoServiceSpiList(Lists.newArrayList(defaultSpi, dssSpi));
        when(domainCryptoService.getSpiIdentifier()).thenReturn(dss);

        domainCryptoService.init();

        verify(dssSpi, times(1)).setDomain(new DomainSpi("DEF", "DEFAULT"));
        verify(dssSpi, times(1)).init();
    }


    @Test
    void initTooManyProviderForGivenIdentifier() {
        final String dss = "DSS";
        final DomainCryptoServiceSpi defaultSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        final DomainCryptoServiceSpi dssSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        when(defaultSpi.getIdentifier()).thenReturn(dss);
        when(dssSpi.getIdentifier()).thenReturn(dss);
        domainCryptoService.setDomainCryptoServiceSpiList(Lists.newArrayList(defaultSpi, dssSpi));
        when(domainCryptoService.getSpiIdentifier()).thenReturn(dss);

        Assertions.assertThrows(IllegalStateException.class, () -> domainCryptoService.init());
    }

    @Test
    void initNoProviderCorrespondToIdentifier() {
        final String dss = "DSS";
        final DomainCryptoServiceSpi defaultSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        final DomainCryptoServiceSpi dssSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        lenient().when(defaultSpi.getIdentifier()).thenReturn(dss);
        lenient().when(dssSpi.getIdentifier()).thenReturn(dss);
        domainCryptoService.setDomainCryptoServiceSpiList(Lists.newArrayList());
        when(domainCryptoService.getSpiIdentifier()).thenReturn(dss);

        Assertions.assertThrows(IllegalStateException.class, () -> domainCryptoService.init());
    }

    @Test
    public void initTrustStore() {
        final String dss = "DSS";
        final DomainCryptoServiceSpi defaultSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        final DomainCryptoServiceSpi dssSpi = Mockito.mock(DomainCryptoServiceSpi.class);
        when(defaultSpi.getIdentifier()).thenReturn("DEFAULT");
        when(dssSpi.getIdentifier()).thenReturn(dss);
        when(domain.getCode()).thenReturn("DEF");
        when(domain.getName()).thenReturn("DEFAULT");
        domainCryptoService.setDomainCryptoServiceSpiList(Lists.newArrayList(defaultSpi, dssSpi));
        when(domainCryptoService.getSpiIdentifier()).thenReturn(dss);

        domainCryptoService.init();

        verify(dssSpi, times(1)).setDomain(new DomainSpi("DEF", "DEFAULT"));
        verify(dssSpi, times(1)).init();
    }
}

