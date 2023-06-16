package eu.domibus.core.certificate;

import eu.domibus.api.pki.DomibusCertificateException;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.core.certificate.CertificateHelper.JKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class CertificateHelperTest {

    @Tested
    CertificateHelper certificateHelper;

    @Test
    public void checkTruststoreTypeValidationHappy1() {
        certificateHelper.validateStoreType(JKS, "test.jks");
    }

    @Test
    public void checkTruststoreTypeValidationHappy2() {
        certificateHelper.validateStoreType(JKS, "test.JKS");
    }

    @Test
    public void checkTruststoreTypeValidationHappy3() {
        certificateHelper.validateStoreType("pkcs12", "test_filename.pfx");
    }

    @Test
    public void checkTruststoreTypeValidationHappy4() {
        certificateHelper.validateStoreType("pkcs12", "test_filename.p12");
    }

    @Test
    public void checkTruststoreTypeValidationNegative1() {
        try {
            certificateHelper.validateStoreType(JKS, "test_filename_wrong_extension.p12");
            Assertions.fail("Expected exception was not raised!");
        } catch (DomibusCertificateException e) {
            assertTrue(e.getMessage().contains(JKS));
        }
    }

    @Test
    public void checkTruststoreTypeValidationNegative2() {
        try {
            certificateHelper.validateStoreType(JKS, "test_filename_no_extension");
            Assertions.fail("Expected exception was not raised!");
        } catch (DomibusCertificateException e) {
            assertTrue(e.getMessage().contains(JKS));
        }
    }

    @Test
    public void checkTruststoreTypeValidationNegative3() {
        try {
            certificateHelper.validateStoreType("pkcs12", "test_filename_unknown_extension.txt");
            Assertions.fail("Expected exception was not raised!");
        } catch (DomibusCertificateException e) {
            assertTrue(e.getMessage().contains("pkcs12"));
        }
    }

    @Test
    public void testValidateStoreFileNameNegative() {
        try {
            certificateHelper.validateStoreFileName("test_filename_unknown_extension.txt");
            Assertions.fail("Expected exception was not raised!");
        } catch (DomibusCertificateException e) {
            assertTrue(e.getMessage().contains("txt"));
        }
    }

    @Test
    public void testValidateStoreFileNamePositive() {
        try {
            certificateHelper.validateStoreFileName("test_filename.p12");
            certificateHelper.validateStoreFileName("test_filename.jks");
            certificateHelper.validateStoreFileName("test_filename.pfx");
        } catch (DomibusCertificateException e) {
            Assertions.fail("Unexpected exception was raised!");
        }
    }
}
