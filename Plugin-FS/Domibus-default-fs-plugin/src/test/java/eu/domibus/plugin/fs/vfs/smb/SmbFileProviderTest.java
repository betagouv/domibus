package eu.domibus.plugin.fs.vfs.smb;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class tests SmbFileProvider by calling the public API of VFS and casting
 * the returned values to the expected classes.
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
public class SmbFileProviderTest {
    
    private FileSystemManager manager;
    private FileSystemOptions defaultAuthOpts;
    
    public SmbFileProviderTest() {
    }
    
    @BeforeEach
    public void setUp() throws FileSystemException {
        manager = VFS.getManager();
        
        defaultAuthOpts = new FileSystemOptions();
        StaticUserAuthenticator auth = new StaticUserAuthenticator("domain", "user", "password");
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(defaultAuthOpts, auth);
    }

    @Test
    public void testGetProviderCapabilities() throws FileSystemException {
        Collection<Capability> expectedCapabilities = Arrays.asList(new Capability[]{
            Capability.CREATE,
            Capability.DELETE,
            Capability.RENAME,
            Capability.GET_TYPE,
            Capability.GET_LAST_MODIFIED,
            Capability.SET_LAST_MODIFIED_FILE,
            Capability.SET_LAST_MODIFIED_FOLDER,
            Capability.LIST_CHILDREN,
            Capability.READ_CONTENT,
            Capability.URI,
            Capability.WRITE_CONTENT,
            Capability.APPEND_CONTENT
        });
        
        Collection<Capability> result = manager.getProviderCapabilities("smb");
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedCapabilities.size(), result.size());
        Assertions.assertTrue(result.containsAll(expectedCapabilities));
    }
    
    @Test
    public void testResolveFile() throws FileSystemException {
        SmbFileObject result = (SmbFileObject) manager.resolveFile("smb://example.org/sharename/file1", defaultAuthOpts);
        
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getName());
        Assertions.assertEquals("smb://example.org/sharename/file1", result.getName().getURI());
    }

    @Test
    public void testResolveURI() throws FileSystemException {
        SmbFileName result = (SmbFileName) manager.resolveURI("smb://example.org/sharename/file2");
        
        Assertions.assertNotNull(result);
        Assertions.assertEquals("smb://example.org/sharename/file2", result.getURI());
    }
    
}
