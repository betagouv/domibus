package eu.domibus.core.util;

import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author François Gautier
 * @since 5.0
 */
public class FileSystemUtilTest {

    @Tested
    private FileSystemUtil fileSystemUtil;

    @Test
    public void createLocationWithRelativePath_returnTempFile() {
        final String location = "..\\domibus_blue\\domibus\\earchiving_storage";
        Path path = fileSystemUtil.createLocation(location);
        Assertions.assertNotNull(path);
        assertTrue(Files.exists(path));
    }

    @Test
    public void createLocationWithAbsolutePath() {
        final String location = System.getProperty("java.io.tmpdir");
        Path path = fileSystemUtil.createLocation(location);
        Assertions.assertNotNull(path);
        assertTrue(Files.exists(path));
    }

    @Test
    public void createLocationWithInvalidPath_returnTempFile() {
        final String location = "domibus:path";
        Path path = fileSystemUtil.createLocation(location);
        Assertions.assertNotNull(path);
        assertTrue(Files.exists(path));
    }
}
