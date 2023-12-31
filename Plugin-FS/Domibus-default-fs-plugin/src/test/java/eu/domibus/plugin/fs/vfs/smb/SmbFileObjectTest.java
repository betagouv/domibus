package eu.domibus.plugin.fs.vfs.smb;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class SmbFileObjectTest {

    @Tested
    private SmbFileObject fileObject;

    @Injectable
    private AbstractFileName name;

    @Injectable
    private SmbFileSystem fileSystem;

//    Do not inject this field globally because in some cases we need a @Mocked instance
//    @Injectable
//    private SmbFile file;

    private FileSystemOptions defaultAuthOpts;

    @BeforeEach
    public void setUp() throws FileSystemException {
        defaultAuthOpts = new FileSystemOptions();
        StaticUserAuthenticator auth = new StaticUserAuthenticator("domain", "user", "password");
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(defaultAuthOpts, auth);

        // smb://example.org/sharename/file1
        name = new SmbFileName("smb", "example.org", -1, null, null, null, "sharename", "/file1", FileType.FILE);
        fileSystem = new SmbFileSystem(name, defaultAuthOpts);
    }

    @AfterEach
    public void tearDown() {
        fileSystem.close();
    }

    @Test
    public void testDoAttach(@Mocked final SmbFile mockFile /*mocks the constructor new SmbFile(path, auth);*/) throws Exception {
        new Expectations() {{
            mockFile.isDirectory();
            result = false;
        }};

        fileObject.doAttach();

        SmbFile result = (SmbFile) getPrivateField(fileObject, "file");

        Assertions.assertNotNull(result);
    }

    @Test
    public void testDoDetach(@Injectable final SmbFile file) throws Exception {
        fileObject.doDetach();

        SmbFile result = (SmbFile) getPrivateField(fileObject, "file");

        Assertions.assertNull(result);
    }

    @Test
    public void testDoGetType_File(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.exists();
            result = true;

            file.isDirectory();
            result = false;

            file.isFile();
            result = true;

        }};

        FileType result = fileObject.doGetType();

        Assertions.assertEquals(FileType.FILE, result);
    }

    @Test
    public void testDoGetType_Folder(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.exists();
            result = true;

            file.isDirectory();
            result = true;

        }};

        FileType result = fileObject.doGetType();

        Assertions.assertEquals(FileType.FOLDER, result);
    }

    @Test
    public void testDoGetType_Imaginary(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.exists();
            result = false;

        }};

        FileType result = fileObject.doGetType();

        Assertions.assertEquals(FileType.IMAGINARY, result);
    }

    @Test
    void testDoGetType_None(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.exists();
            result = true;

            file.isDirectory();
            result = false;

            file.isFile();
            result = false;
        }};

        Assertions.assertThrows(FileSystemException.class, () -> fileObject.doGetType());
    }

    @Test
    public void testDoListChildren(@Injectable final SmbFile file) throws Exception {
        final String[] childList = new String[]{
                "smb://example.org/sharename/file1/child1",
                "smb://example.org/sharename/file1/child2"
        };

        new Expectations() {{
            file.isDirectory();
            result = true;

            file.list();
            result = childList;
        }};

        String[] result = fileObject.doListChildren();

        Assertions.assertArrayEquals(childList, result);
    }

    @Test
    public void testDoListChildren_NotDirectory(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.isDirectory();
            result = false;
        }};

        String[] result = fileObject.doListChildren();

        Assertions.assertArrayEquals(ArrayUtils.EMPTY_STRING_ARRAY, result);
    }

    @Test
    public void testDoIsHidden(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.isHidden();
            result = true;
        }};

        boolean result = fileObject.doIsHidden();

        Assertions.assertTrue(result);
    }

    @Test
    public void testDoDelete(@Injectable final SmbFile file) throws Exception {
        fileObject.doDelete();

        new VerificationsInOrder() {{
            file.delete();
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testDoRename(@Mocked final SmbFile mockFile) throws Exception {
        AbstractFileName name2 = new SmbFileName("smb", "example.org", -1, null, null, null, "sharename", "/file2", FileType.FILE);
        SmbFileSystem fileSystem2 = new SmbFileSystem(name, defaultAuthOpts);

        new Expectations() {{
            mockFile.isDirectory();
            result = false;
        }};

        fileObject.doRename(new SmbFileObject(name2, fileSystem2));

        new VerificationsInOrder() {{
//            file.renameTo(mockFile);
        }};

        SmbFile result = (SmbFile) getPrivateField(fileObject, "file");

        Assertions.assertNotNull(result);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testDoCreateFolder(@Mocked final SmbFile file) throws Exception {
        ReflectionTestUtils.setField(fileObject, "file", file);
        new Expectations() {{
            SmbFile mockFile1 = new SmbFile("smb://example.org/sharename/file1", (NtlmPasswordAuthentication) any);
            mockFile1.isDirectory();
            result = true;

            mockFile1.toString();
            result = "smb://example.org/sharename/file1";

            SmbFile mockFile2 = new SmbFile("smb://example.org/sharename/file1/", (NtlmPasswordAuthentication) any);
        }};

        fileObject.doCreateFolder();

        SmbFile result = (SmbFile) getPrivateField(fileObject, "file");

        Assertions.assertNotNull(result);
    }

    @Test
    public void testDoGetContentSize(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.length();
            result = 12345;
        }};

        long result = fileObject.doGetContentSize();

        Assertions.assertEquals(12345, result);
    }

    @Test
    public void testDoGetLastModifiedTime(@Injectable final SmbFile file) throws Exception {
        new Expectations() {{
            file.getLastModified();
            result = 1503495641984L;
        }};

        long result = fileObject.doGetLastModifiedTime();

        Assertions.assertEquals(1503495641984l, result);
    }

    @Test
    public void testDoGetInputStream(@Mocked final SmbFileInputStream mockInputStream) throws Exception {

        new Expectations() {{
        }};
        SmbFileInputStream result = (SmbFileInputStream) fileObject.doGetInputStream();

        Assertions.assertNotNull(result);
    }

    @Test
    public void testDoGetOutputStream(@Mocked final SmbFileOutputStream mockOutputStream) throws Exception {

        SmbFileOutputStream result = (SmbFileOutputStream) fileObject.doGetOutputStream(true);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testDoSetLastModifiedTime(@Injectable final SmbFile file) throws Exception {
        boolean result = fileObject.doSetLastModifiedTime(1503495641984l);

        new VerificationsInOrder() {{
            file.setLastModified(1503495641984L);
        }};

        Assertions.assertTrue(result);
    }

    private Object getPrivateField(final Object object, final String field) throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, SecurityException {
        Field privateStringField = SmbFileObject.class.getDeclaredField(field);
        privateStringField.setAccessible(true);
        return privateStringField.get(object);
    }

}
