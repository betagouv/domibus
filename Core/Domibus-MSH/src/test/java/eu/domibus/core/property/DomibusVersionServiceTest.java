package eu.domibus.core.property;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

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
    public void testDisplayVersion() {

        DomibusVersionService service = new DomibusVersionService();

        assertEquals("domibus-MSH", service.getArtifactName());
        assertNotEquals("", service.getBuiltTime());
        assertNotEquals("", service.getArtifactVersion());

        LOG.info(service.getDisplayVersion());
    }

    @Test
    public void testVersionNumber() {
        Properties versionProps = new Properties();
        versionProps.put("Artifact-Version", "4.1-RC1");
        ReflectionTestUtils.setField(service, "versionProps", versionProps);

        new Expectations() {{
            versionProps.getProperty("Artifact-Version");
            returns("4.1-RC1", "4.0.2");
        }};

        String version = service.getVersionNumber();
        assertEquals("4.1", version);

        versionProps.put("Artifact-Version", "4.0.2");

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
    public void getBuiltTime() {
        Properties versionProps = new Properties();
        versionProps.put("Build-Time", "2021-02-18 09:47");
        ReflectionTestUtils.setField(service, "versionProps", versionProps);

        Locale.setDefault(Locale.ENGLISH);
        String time = service.getBuiltTime();

        assertEquals("2021-02-18 09:47|Coordinated Universal Time", time);
    }
}
