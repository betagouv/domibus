package eu.domibus.core.util.backup;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DateUtil;
import eu.domibus.core.util.DateUtilImpl;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static eu.domibus.core.util.backup.BackupServiceImpl.BACKUP_EXT;
import static eu.domibus.core.util.backup.BackupServiceImpl.BACKUP_FILE_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class BackupServiceImplTest {

    @Tested
    BackupServiceImpl backupService;

    @Injectable
    DateUtil dateUtil;

    @Tested
    DateUtilImpl dateUtilImpl;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    DomainContextProvider domainProvider;

    @Test
    @Disabled("EDELIVERY-6896")
    public void testBackupFile() throws IOException {
        File originalFile = new File("testfile");
        new Expectations(FileUtils.class) {{
            FileUtils.copyFile((File) any, (File) any);
        }};

        backupService.backupFile(originalFile);

        new Verifications() {{
            File backupFile;
            backupService.getBackupFile(originalFile);
            FileUtils.copyFile(originalFile, backupFile = withCapture());
            assertFalse(backupFile.getName().equalsIgnoreCase(originalFile.getName()));
            assertTrue(backupFile.getName().contains(originalFile.getName()));
        }};
    }

    @Test
    public void testBackupFileName() {
        String timePart = "2019-07-15_23_01_01.111";
        final String originalFileName = "domibus.properties";
        final String parentDirectory = "home";

        new Expectations() {{
            dateUtil.getCurrentTime(BACKUP_FILE_FORMATTER);
            result = timePart;
        }};

        final File originalFile = new File(parentDirectory, originalFileName);
        final File backupFile = backupService.getBackupFile(originalFile);

        assertEquals(originalFileName + BACKUP_EXT + timePart, backupFile.getName());
        assertEquals(parentDirectory, backupFile.getParent());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testTimestampFormatter() {
        final LocalDateTime now = LocalDateTime.of(2019, 9, 2, 15, 1, 55, 123 * 1000000);
        final String expectedValue = "2019-09-02_15_01_55.123";

        new Expectations(LocalDateTime.class) {{
            LocalDateTime.now(ZoneOffset.UTC);
            result = now;
        }};

        String value = dateUtilImpl.getCurrentTime(BACKUP_FILE_FORMATTER);
        assertEquals(expectedValue, value);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void backupFileInLocation(@Injectable Domain currentDomain) throws IOException {
        File originalFile = new File("testfile");
        String backupLocation = "testfile_backup";
        File backupFile = new File(backupLocation);

        new Expectations(FileUtils.class, backupService) {{
            backupService.createBackupFileInLocation(originalFile, backupLocation);
            result = backupFile;
            FileUtils.copyFile((File) any, (File) any);
        }};

        backupService.backupFileInLocation(originalFile, backupLocation);

        new Verifications() {{
            File backupFile;
            FileUtils.copyFile(originalFile, backupFile = withCapture());
            assertTrue(backupFile.getName().startsWith(originalFile.getName()));
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void createBackupFileInLocation() throws IOException {
        File originalFile = new File("testfile");
        final String backupLocation = "test_backupFile";
        File backupFile = new File(backupLocation);
        new Expectations(backupService, Files.class) {{
            Files.exists(Paths.get(backupLocation).normalize());
            result = false;
        }};

        backupService.createBackupFileInLocation(originalFile, backupLocation);

        new Verifications() {{
            backupService.getBackupFile(originalFile, backupFile);
        }};
    }

    @Test
    public void getBackupFile() {
        String timePart = "2019-07-15_23_01_01.111";
        File originalFile = new File("test_file");
        final String backupLocation = "test_backupFile";
        File backupFile = new File(backupLocation);

        new Expectations() {{
            dateUtil.getCurrentTime(BACKUP_FILE_FORMATTER);
            result = timePart;
        }};
        File newBackupFile = backupService.getBackupFile(originalFile, backupFile);
        assertTrue(newBackupFile.getName().startsWith(originalFile.getName()));
    }
}
