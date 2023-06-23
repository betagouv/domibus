package eu.domibus.core.property.encryption;

import eu.domibus.plugin.encryption.PluginPropertyEncryptionListener;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class DomibusPropertyEncryptionNotifierImplTest {

    @Tested
    protected DomibusPropertyEncryptionNotifierImpl domibusPropertyEncryptionNotifier;

    @Test
    public void signalEncryptPasswords(@Mocked PluginPropertyEncryptionListener pluginPropertyEncryptionListener) {
        domibusPropertyEncryptionNotifier.signalEncryptPasswords();

        new Verifications() {{
            pluginPropertyEncryptionListener.encryptPasswords();
            times = 0;
        }};
    }
}
