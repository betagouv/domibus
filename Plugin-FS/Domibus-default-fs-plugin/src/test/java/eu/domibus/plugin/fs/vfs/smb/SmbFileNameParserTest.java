package eu.domibus.plugin.fs.vfs.smb;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.FileNameParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
public class SmbFileNameParserTest {

    private SmbFileNameParser smbFileNameParser;

    @BeforeEach
    public void setUp() throws Exception {
        smbFileNameParser = new SmbFileNameParser();
    }

    @Test
    public void testGetInstance() {
        FileNameParser result1 = SmbFileNameParser.getInstance();
        FileNameParser result2 = SmbFileNameParser.getInstance();

        Assertions.assertNotNull(result1);
        Assertions.assertNotNull(result2);
        Assertions.assertSame(result1, result2);
    }

    @Test
    public void testParseUri() throws FileSystemException {
        SmbFileName result = (SmbFileName) smbFileNameParser.parseUri(null, null, "smb://example.org/sharename/file1");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("smb://example.org/sharename/file1", result.getURI());
    }

    @Test
    public void testParseUri_AllFields() throws FileSystemException {
        SmbFileName result = (SmbFileName) smbFileNameParser.parseUri(null, null, "smb://domain\\user:password@example.org:12345/sharename/file1");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("smb://domain\\user:password@example.org:12345/sharename/file1", result.getURI());
    }

    @Test
    public void testParseUri_NoDomain() throws FileSystemException {
        SmbFileName result = (SmbFileName) smbFileNameParser.parseUri(null, null, "smb://user:password@example.org/sharename/file1");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("smb://user:password@example.org/sharename/file1", result.getURI());
    }

    @Test
    void testParseUri_EmptyShareName() {
        Assertions.assertThrows(FileSystemException.class, () -> smbFileNameParser.parseUri(null, null, "smb://example.org/"));
    }

    @Test
    void testParseUri_NoShareName() {
        Assertions.assertThrows(FileSystemException.class, () -> smbFileNameParser.parseUri(null, null, "smb://example.org"));
    }

}
