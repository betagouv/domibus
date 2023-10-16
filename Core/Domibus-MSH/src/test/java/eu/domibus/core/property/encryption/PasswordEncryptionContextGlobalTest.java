package eu.domibus.core.property.encryption;

import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.property.encryption.PasswordEncryptionContextAbstract;
import eu.domibus.api.property.encryption.PasswordEncryptionService;
import eu.domibus.core.property.DomibusRawPropertyProvider;
import eu.domibus.core.property.GlobalPropertyMetadataManager;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PASSWORD_ENCRYPTION_KEY_LOCATION;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PASSWORD_ENCRYPTION_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class PasswordEncryptionContextDefaultTest {

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private PasswordEncryptionService passwordEncryptionService;

    @Injectable
    private DomibusRawPropertyProvider domibusRawPropertyProvider;

    @Injectable
    private GlobalPropertyMetadataManager globalPropertyMetadataManager;

    @Tested
    private PasswordEncryptionContextGlobal passwordEncryptionContextGlobal;

    @Test
    public void isPasswordEncryptionActive() {
        new Expectations() {{
            domibusConfigurationService.isPasswordEncryptionActive();
            result = true;
        }};

        Assertions.assertTrue(passwordEncryptionContextDefault.isPasswordEncryptionActive());
    }

    @Test
    public void getProperty() {
        String myProperty = "myProperty";
        passwordEncryptionContextGlobal.getProperty(myProperty);

        new Verifications() {{
            domibusRawPropertyProvider.getRawPropertyValue(myProperty);
        }};
    }

    @Test
    public void getConfigurationFileName() {
        String myConfFile = "myConfFile";

        new Expectations() {{
            domibusConfigurationService.getConfigurationFileName();
            result = myConfFile;
        }};

        Assertions.assertEquals(myConfFile, passwordEncryptionContextDefault.getConfigurationFileName());
    }

    @Test
    public void getEncryptedKeyFile() {
        String encryptionKeyLocation = "home" + File.separator + "location";

        new Expectations(passwordEncryptionContextGlobal) {{
            passwordEncryptionContextGlobal.getProperty(DOMIBUS_PASSWORD_ENCRYPTION_KEY_LOCATION);
            result = encryptionKeyLocation;
        }};

        final File encryptedKeyFile = passwordEncryptionContextGlobal.getEncryptedKeyFile();
        assertEquals(PasswordEncryptionContextAbstract.ENCRYPTED_KEY, encryptedKeyFile.getName());
    }
    
}
