package eu.domibus.core.payload.persistence.filesystem;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Flavio Santos
 */
@ExtendWith(JMockitExtension.class)
public class StorageTest {

    @Tested
    private PayloadFileStorage storage;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void testMissingPayloadFolder(@Injectable Domain domain) throws IOException {
        String folderName = String.valueOf(System.currentTimeMillis());
        Path tempPath = Paths.get("src", "test", "resources", folderName).toAbsolutePath();
        Assertions.assertTrue(Files.notExists(tempPath));

        new Expectations(storage) {{
            domibusPropertyProvider.getProperty((Domain) any,  PayloadFileStorage.ATTACHMENT_STORAGE_LOCATION);
            result = tempPath.toString();
        }};

        storage.init();

        Assertions.assertTrue(Files.exists(tempPath));
        File storageDirectory = (File) ReflectionTestUtils.getField(storage, "storageDirectory");
        Assertions.assertEquals(tempPath.toAbsolutePath().toString(), storageDirectory.toString());
        Files.delete(tempPath);
    }

    @Test
    public void testWrongPayloadFolder(@Injectable Domain domain) throws Exception {
        Path tempPath = Paths.get("src", "test", "resources");
        new Expectations(storage) {{
            domibusPropertyProvider.getProperty( (Domain)any, PayloadFileStorage.ATTACHMENT_STORAGE_LOCATION);
            result = isWindowsOS() ? getWindowsFileSystemIncorrectPath(tempPath.toString()) : getLinuxFileSystemIncorrectPath(tempPath.toString());
        }};

        storage.init();

        File storageDirectory = (File) ReflectionTestUtils.getField(storage, "storageDirectory");
        Assertions.assertEquals(Paths.get(System.getProperty("java.io.tmpdir")).toString(), storageDirectory.toString());
    }

    private boolean isWindowsOS() {
        String osName = System.getProperty("os.name");
        return (osName.toLowerCase().startsWith("windows"));
    }

    private String getWindowsFileSystemIncorrectPath(String path) throws Exception {
        //Select a non existent windows file system drive to mock an incorrect path
        char[] possibleDriveLetters = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        List<File> availableDriveLetters = new ArrayList<>(Arrays.asList(File.listRoots()));
        for (int i = 0; i < possibleDriveLetters.length; i++) {
            if (!availableDriveLetters.contains(possibleDriveLetters[i] + ":\\")) {
                return possibleDriveLetters[i] + ":\\" + path;
            }
        }
        throw new Exception("Available (not used) drive not found in the file system.");
    }

    private String getLinuxFileSystemIncorrectPath(String path) {
        //Select a non existent linux file system path to mock an incorrect path
        return "/wrongpath/" + path;
    }
}
