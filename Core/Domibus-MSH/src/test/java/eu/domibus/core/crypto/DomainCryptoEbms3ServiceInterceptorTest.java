package eu.domibus.core.crypto;

import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.crypto.spi.CryptoSpiException;
import eu.domibus.core.crypto.spi.DomibusCertificateSpiException;
import eu.domibus.core.util.AOPUtilImpl;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyStoreException;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
//TODO split into multipe test methods.
public class DomainCryptoEbms3ServiceInterceptorTest {

    @Test
    public void convertCoreException() {

        DomainCryptoServiceInterceptor domainCryptoServiceInterceptor=new DomainCryptoServiceInterceptor(new AOPUtilImpl());
        CryptoException transformedCryptoException = (CryptoException) domainCryptoServiceInterceptor.convertCoreException(new RuntimeException("bla"));
        Assertions.assertEquals("[DOM_001]:bla",transformedCryptoException.getMessage());

        transformedCryptoException = (CryptoException) domainCryptoServiceInterceptor.convertCoreException(new CryptoSpiException("bla"));
        Assertions.assertEquals("[DOM_001]:bla",transformedCryptoException.getMessage());

        DomibusCertificateException domibusCertificateException = (DomibusCertificateException) domainCryptoServiceInterceptor.convertCoreException(new DomibusCertificateSpiException("bla"));
        Assertions.assertEquals("bla",domibusCertificateException.getMessage());

        final WSSecurityException wsSecurityException = new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR);
        final WSSecurityException returnedWsSecurityException=(WSSecurityException) domainCryptoServiceInterceptor.convertCoreException(wsSecurityException);
        Assertions.assertEquals(wsSecurityException,returnedWsSecurityException);

        final KeyStoreException keyStoreException = new KeyStoreException("test");
        final KeyStoreException returnedKeyStoreException=(KeyStoreException) domainCryptoServiceInterceptor.convertCoreException(keyStoreException);
        Assertions.assertEquals(keyStoreException,returnedKeyStoreException);

        final CryptoException cryptoException = new CryptoException("test");
        final CryptoException returnedCryptoException=(CryptoException) domainCryptoServiceInterceptor.convertCoreException(cryptoException);
        Assertions.assertEquals(cryptoException,returnedCryptoException);

        final ConfigurationException configurationException = new ConfigurationException("test");
        final ConfigurationException returnedConfigurationException=(ConfigurationException) domainCryptoServiceInterceptor.convertCoreException(configurationException);
        Assertions.assertEquals(configurationException,returnedConfigurationException);
    }
}
