package eu.domibus.core.payload.temp;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class TemporaryPayloadServiceImplTest {

    @Tested
    TemporaryPayloadServiceImpl temporaryPayloadService;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void cleanTemporaryPayloads(@Injectable File directory,
                                       @Injectable File file1,
                                       @Injectable Domain domain) {
        final Collection<File> filesToClean = new ArrayList<>();
        filesToClean.add(file1);

        new Expectations(temporaryPayloadService) {{
            temporaryPayloadService.getFilesToClean(directory);
            result = filesToClean;

            temporaryPayloadService.deleteFileSafely((File) any);

        }};

        temporaryPayloadService.cleanTemporaryPayloads(directory);

        new Verifications() {{
            temporaryPayloadService.deleteFileSafely(file1);
        }};
    }

    @Test
    public void deleteFileSafely(@Injectable File file) {
        temporaryPayloadService.deleteFileSafely(file);

        new Verifications() {{
            file.delete();
        }};
    }

    @Test
    public void deleteFileSafelyWhenExceptionIsThrown(@Injectable File file) {
        new Expectations() {{
            file.delete();
            result = new RuntimeException();
        }};

        temporaryPayloadService.deleteFileSafely(file);

        new Verifications() {{
            file.delete();
        }};
    }


    @Test
    public void getRegexFileFilter(@Injectable Pattern regexPattern,
                                   @Mocked RegexIOFileFilter regexIOFileFilterBase,
                                   @Injectable Domain domain) {
        String excludeRegex = "regexExpression";
        new MockUp<Pattern>() {
            @Mock
            Pattern compile(String regex) {
                return regexPattern;
            }
        };


        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_EXCLUDE_REGEX);
            result = excludeRegex;

            RegexIOFileFilter regexIOFileFilter = new RegexIOFileFilter(regexPattern);

            regexIOFileFilter.negate();
            times = 1;
        }};

        temporaryPayloadService.getRegexFileFilter();

    }

    @Test
    public void getAgeFileFilter() {
        int expirationThresholdInMinutes = 5000;

        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_EXPIRATION);
            result = expirationThresholdInMinutes;

        }};

        IOFileFilter ageFileFilter = temporaryPayloadService.getAgeFileFilter();


        Assertions.assertEquals(AgeFileFilter.class, ageFileFilter.getClass());


        Long cutoffMillis = (Long) ReflectionTestUtils.getField(ageFileFilter, "cutoffMillis");
        assertThat(cutoffMillis, lessThan(System.currentTimeMillis()));

    }

    @Test
    public void getTemporaryLocations(@Injectable File dir1,
                                      @Injectable File dir2,
                                      @Injectable Domain domain) {
        String directories = "dir1,dir2";

        new Expectations(temporaryPayloadService) {{
            domibusPropertyProvider.getProperty(DOMIBUS_PAYLOAD_TEMP_JOB_RETENTION_DIRECTORIES);
            result = directories;

            temporaryPayloadService.getDirectory("dir1");
            result = dir1;

            temporaryPayloadService.getDirectory("dir2");
            result = dir2;
        }};

        final List<File> temporaryLocations = temporaryPayloadService.getTemporaryLocations();
        Assertions.assertEquals(2, temporaryLocations.size());
        Assertions.assertSame(temporaryLocations.iterator().next(), dir1);
        Assertions.assertSame(temporaryLocations.iterator().next(), dir1);
    }

    @Test
    public void getDirectory(@Injectable File directoryFile) {
        String directory = "dir1";

        new Expectations(temporaryPayloadService) {{
            temporaryPayloadService.getDirectoryIfExists(directory);
            result = directoryFile;
        }};

        final File payloadServiceDirectory = temporaryPayloadService.getDirectory(directory);
        Assertions.assertSame(payloadServiceDirectory, directoryFile);

        new Verifications() {{
            domibusPropertyProvider.getProperty(anyString);
            times = 0;
        }};
    }

    @Test
    public void getDirectoryFromDomibusProperties(@Injectable File directoryFile) {
        String directory = "dir1";

        new Expectations(temporaryPayloadService) {{
            temporaryPayloadService.getDirectoryIfExists(directory);
            result = null;

            domibusPropertyProvider.getProperty(directory);
            result = "myprop";

            temporaryPayloadService.getDirectoryIfExists("myprop");
            result = directoryFile;
        }};

        final File payloadServiceDirectory = temporaryPayloadService.getDirectory(directory);
        Assertions.assertSame(payloadServiceDirectory, directoryFile);
    }

    @Test
    public void getDirectoryIfExists(@Mocked File fileBased) {
        String directory = "dir1";

        new Expectations() {{
            File file = new File(directory);

            file.exists();
            result = true;
        }};

        final File result = temporaryPayloadService.getDirectoryIfExists(directory);
        Assertions.assertNotNull(result);
    }
}
