package eu.domibus.core.property.encryption;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.encryption.PasswordEncryptionContext;
import eu.domibus.api.property.encryption.PasswordEncryptionSecret;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class PasswordDecryptionHelperTest {

    @Tested
    PasswordDecryptionHelper passwordDecryptionHelper;

    @Test
    public void isValueEncryptedWithNonEncryptedValue() {
        Assertions.assertFalse(passwordDecryptionHelper.isValueEncrypted("nonEncrypted"));
    }

    @Test
    public void isValueEncryptedWithEncryptedValue() {
        Assertions.assertTrue(passwordDecryptionHelper.isValueEncrypted("ENC(nonEncrypted)"));
    }

    @Test
    public void isValueEncrypted_blank() {
        assertFalse(passwordDecryptionHelper.isValueEncrypted(""));
    }

}
