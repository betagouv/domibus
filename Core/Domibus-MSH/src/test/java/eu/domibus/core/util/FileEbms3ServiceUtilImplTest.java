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

    @Tested
    FileServiceUtilImpl fileServiceUtil;

    @Test
    public void test_sanitizeFileName() {

        String baseFileName = "content.xml";
        String fileName = baseFileName;

        String sanitizedFileName = fileServiceUtil.sanitizeFileName(fileName);
        Assertions.assertEquals(baseFileName, sanitizedFileName);

        fileName = "./../../../" + baseFileName;
        sanitizedFileName = fileServiceUtil.sanitizeFileName(fileName);
        Assertions.assertEquals(baseFileName, sanitizedFileName);

        fileName = "./../../../../..\\..\\" + baseFileName;
        sanitizedFileName = fileServiceUtil.sanitizeFileName(fileName);
        Assertions.assertEquals(baseFileName, sanitizedFileName);
    }
}
