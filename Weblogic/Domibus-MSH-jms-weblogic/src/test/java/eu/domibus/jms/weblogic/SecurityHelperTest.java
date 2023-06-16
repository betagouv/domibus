package eu.domibus.jms.weblogic;

import mockit.Expectations;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by Cosmin Baciu on 30-Sep-16.
 */
@ExtendWith(JMockitExtension.class)
public class SecurityHelperTest {

    @Tested
    SecurityHelper securityHelper;

    @Test
    public void testGetBootIdentityWithUserAndPasswordProvidedAsSystemVariables() throws Exception {
        new Expectations(System.class) {{
            System.getProperty("weblogic.management.username");
            result = "myusername";

            System.getProperty("weblogic.management.password");
            result = "mypwd";
        }};

        final Map<String, String> bootIdentity = securityHelper.getBootIdentity();
        assertEquals("myusername", bootIdentity.get("username"));
        assertEquals("mypwd", bootIdentity.get("password"));
    }

    @Test
    public void testGetBootIdentityWithUserAndPasswordFromTheBootFile() throws Exception {
        File bootPropertiesFile = new File(getClass().getClassLoader().getResource("jms/boot.properties").toURI());
        final String bootPropertiesPath = bootPropertiesFile.getAbsolutePath();

        new Expectations(System.class) {{
            System.getProperty("weblogic.management.username");
            result = null;

            System.getProperty("weblogic.management.password");
            result = null;

            System.getProperty("weblogic.system.BootIdentityFile");
            result = bootPropertiesPath;
        }};

        new Expectations(securityHelper) {{
            securityHelper.decrypt("{AES}myuser");
            result = "{AES}myuser";

            securityHelper.decrypt("{AES}mypwd");
            result = "{AES}mypwd";
        }};

        final Map<String, String> bootIdentity = securityHelper.getBootIdentity();
        assertEquals("{AES}myuser", bootIdentity.get("username"));
        assertEquals("{AES}mypwd", bootIdentity.get("password"));
    }


}
