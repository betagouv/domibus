package eu.domibus.core.crypto.spi.dss;

import eu.europa.esig.dss.tsl.job.TLValidationJob;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Thomas Dussart
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DssRefreshCommandTest {

    @Test
    public void canHandleTrue() {
        Assertions.assertTrue(new DssRefreshCommand(null, null).canHandle(DssRefreshCommand.COMMAND_NAME));
    }

    @Test
    public void canHandleFalse() {
        Assertions.assertFalse(new DssRefreshCommand(null, null).canHandle("test"));
    }

    @Test
    public void executeRefresh(@Mocked TLValidationJob tlValidationJob) {
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, null);
        dssRefreshCommand.execute(new HashMap<>());
        new Verifications(){{
            tlValidationJob.onlineRefresh();times=1;
        }};
    }



    @Test
    public void initWithNonExistingDirectory(@Injectable TLValidationJob tlValidationJob, @Injectable File cacheDirectory, @Mocked LocalDateTime localDateTime) {
        new Expectations(){{
            localDateTime.now();
            result=null;
            cacheDirectory.toPath().toFile().exists();
            result=false;
        }};
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, cacheDirectory);
        dssRefreshCommand.loadTrustedList();
        new Verifications(){{
            tlValidationJob.onlineRefresh();times=0;
            tlValidationJob.offlineRefresh();times=0;
        }};
    }


    @Test
    public void initWithCacheDirectoryEmpty(@Injectable TLValidationJob tlValidationJob, @Injectable File cacheDirectory, @Injectable Path path, @Mocked LocalDateTime localDateTime, @Mocked Files files, @Mocked Iterator iterator) throws  IOException {
        new Expectations(){{
            localDateTime.now();
            result=null;
            cacheDirectory.toPath();
            result=path;
            path.toFile().exists();
            result=true;
            files.newDirectoryStream(path).iterator();
            result=iterator;
            iterator.hasNext();
            result=false;
        }};
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, cacheDirectory);
        dssRefreshCommand.loadTrustedList();
        new Verifications(){{
            tlValidationJob.onlineRefresh();times=1;
        }};
    }

    @Test
    public void initWithCacheDirectoryNotEmpty(@Mocked TLValidationJob tlValidationJob, @Mocked File cacheDirectory, @Mocked Path path, @Mocked LocalDateTime localDateTime, @Mocked Files files, @Mocked Iterator iterator) throws  IOException {
        new Expectations(){{
            localDateTime.now();
            result=null;
            cacheDirectory.toPath();
            result=path;
            path.toFile().exists();
            result=true;
            files.newDirectoryStream(path).iterator();
            result=iterator;
            iterator.hasNext();
            result=true;
        }};
        DssRefreshCommand dssRefreshCommand = new DssRefreshCommand(tlValidationJob, cacheDirectory);
        dssRefreshCommand.loadTrustedList();
        new Verifications(){{
            tlValidationJob.offlineRefresh();times=1;
        }};
    }
}
