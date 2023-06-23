package eu.domibus.core.property;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Federico Martini
 */
@ExtendWith(JMockitExtension.class)
public class DomibusVersionServiceTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusVersionServiceTest.class);

    @Tested
    DomibusVersionService service;

    @Test
    public void testDisplayVersion() throws Exception {

        DomibusVersionService service = new DomibusVersionService();

        assertEquals("domibus-MSH", service.getArtifactName());
        assertNotEquals("", service.getBuiltTime());
        assertNotEquals("", service.getArtifactVersion());

        LOG.info(service.getDisplayVersion());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testVersionNumber(@Mocked Properties versionProps) throws Exception {

        new Expectations() {{
            versionProps.getProperty("Artifact-Version");
            returns("4.1-RC1", "4.0.2");
        }};

        String version = service.getVersionNumber();
        assertEquals("4.1", version);

        String version2 = service.getVersionNumber();
        assertEquals("4.0.2", version2);
    }

    @Test
    public void testGetBuildDetails() {

        String artifactName = "domibus-MSH";

        new Expectations(service) {{
            service.getArtifactName();
            result = artifactName;

        }};

        String buildDetails = service.getBuildDetails();

        assertTrue(buildDetails.contains(artifactName));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getBuiltTime(@Mocked Properties versionProps) {
        Locale.setDefault(Locale.ENGLISH);
        new Expectations() {{
            versionProps.getProperty("Build-Time");
            result = "2021-02-18 09:47";
        }};

        String time = service.getBuiltTime();

        assertEquals("2021-02-18 09:47|Coordinated Universal Time", time);
    }
}
