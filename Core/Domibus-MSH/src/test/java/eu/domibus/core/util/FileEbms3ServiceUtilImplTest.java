package eu.domibus.core.util;

import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Catalin Enache
 * @since 4.1.4
 */
@RunWith(JMockit.class)
public class FileEbms3ServiceUtilImplTest {

    public static final String BASE_FILE_NAME = "content.xml";
    @Tested
    FileServiceUtilImpl fileServiceUtil;

    @Test
    public void test_sanitizeFileName() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName(BASE_FILE_NAME);
        Assert.assertEquals(BASE_FILE_NAME, sanitizedFileName);

    }

    @Test
    public void test_sanitizeFileName_blank() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName("");
        Assert.assertNull(sanitizedFileName);

    }

    @Test
    public void test_sanitizeFileName_path() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName("./../../../" + BASE_FILE_NAME);
        Assert.assertEquals(BASE_FILE_NAME, sanitizedFileName);

    }

    @Test
    public void test_sanitizeFileName_path2() {

        String sanitizedFileName = fileServiceUtil.sanitizeFileName("./../../../../..\\..\\" + BASE_FILE_NAME);
        Assert.assertEquals(BASE_FILE_NAME, sanitizedFileName);
    }
}