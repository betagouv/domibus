package eu.domibus.core.util;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Catalin Enache
 * @since 4.1.4
 */
@ExtendWith(JMockitExtension.class)
public class FileEbms3ServiceUtilImplTest {

    public static final String BASE_FILE_NAME = "content.xml";
    @Tested
    FileServiceUtilImpl fileServiceUtil;

    @Test
    public void test_sanitizeFileName() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName(BASE_FILE_NAME);
        Assertions.assertEquals(BASE_FILE_NAME, sanitizedFileName);

    }

    @Test
    public void test_sanitizeFileName_blank() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName("");
        Assertions.assertNull(sanitizedFileName);

    }

    @Test
    public void test_sanitizeFileName_path() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName("./../../../" + BASE_FILE_NAME);
        Assertions.assertEquals(BASE_FILE_NAME, sanitizedFileName);

    }

    @Test
    public void test_sanitizeFileName_path2() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName("./../../../../..\\..\\" + BASE_FILE_NAME);
        Assertions.assertEquals(BASE_FILE_NAME, sanitizedFileName);
    }
}