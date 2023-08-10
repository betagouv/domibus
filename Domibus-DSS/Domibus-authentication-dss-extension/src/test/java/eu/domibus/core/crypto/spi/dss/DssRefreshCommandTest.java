package eu.domibus.core.crypto.spi.dss;

import eu.europa.esig.dss.tsl.job.TLValidationJob;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * @author Thomas Dussart
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
class DssRefreshCommandTest {

    @Test
    void canHandleTrue() {
        Assertions.assertTrue(new DssRefreshCommand(null, null).canHandle(DssRefreshCommand.COMMAND_NAME));
    }

    @Test
    void canHandleFalse() {
        Assertions.assertFalse(new DssRefreshCommand(null, null).canHandle("test"));
    }

    @Test
    void executeRefresh(@Injectable TLValidationJob tlValidationJob) {
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, null);
        dssRefreshCommand.execute(new HashMap<>());
        new Verifications() {{
            tlValidationJob.onlineRefresh();
            times = 1;
        }};
    }


    @Test
    void initWithNonExistingDirectory(@Injectable TLValidationJob tlValidationJob, @Injectable File cacheDirectory) {
        new Expectations() {{
            cacheDirectory.toPath().toFile().exists();
            result = false;
        }};
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, cacheDirectory);
        dssRefreshCommand.loadTrustedList();
        new Verifications() {{
            tlValidationJob.onlineRefresh();
            times = 0;
            tlValidationJob.offlineRefresh();
            times = 0;
        }};
    }


    @Test
    void initWithCacheDirectoryEmpty(@Injectable TLValidationJob tlValidationJob, @Injectable File cacheDirectory, @Injectable Path path) {
        new Expectations() {{
            cacheDirectory.toPath();
            result = path;
            path.toFile().exists();
            result = true;
        }};
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, cacheDirectory);
        dssRefreshCommand.loadTrustedList();
        new Verifications() {{
            tlValidationJob.onlineRefresh();
            times = 1;
        }};
    }

    @Test
    void initWithCacheDirectoryNotEmpty(@Injectable TLValidationJob tlValidationJob,
                                        @TempDir Path tempDir) throws IOException {

        Files.createFile(tempDir.resolve("test.file"));

        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, tempDir.toFile());
        dssRefreshCommand.loadTrustedList();
        new Verifications(){{
            tlValidationJob.offlineRefresh();
            times=1;
        }};
    }
}
