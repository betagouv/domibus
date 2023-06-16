package eu.domibus.plugin.fs;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@ExtendWith(JMockitExtension.class)
public class FSMimeTypeHelperImplTest {

    @Tested
    protected FSMimeTypeHelperImpl fsMimeTypeHelper;
    
    public FSMimeTypeHelperImplTest() {
    }

    @Test
    public void testGetMimeType_Text() {
        String result = fsMimeTypeHelper.getMimeType("file.txt");
        
        Assertions.assertEquals("text/plain", result);
    }
    
    @Test
    public void testGetMimeType_Xml() {
        String result = fsMimeTypeHelper.getMimeType("file.xml");
        
        Assertions.assertEquals("application/xml", result);
    }
    
    @Test
    public void testGetMimeType_Pdf() {
        String result = fsMimeTypeHelper.getMimeType("file.pdf");
        
        Assertions.assertEquals("application/pdf", result);
    }

    @Test
    public void testGetExtension_Text() throws Exception {
        String result = fsMimeTypeHelper.getExtension("text/plain");
        
        Assertions.assertEquals(".txt", result);
    }

    @Test
    public void testGetExtension_Xml() throws Exception {
        String result = fsMimeTypeHelper.getExtension("application/xml");
        
        Assertions.assertEquals(".xml", result);
    }
    
    @Test
    public void testGetExtension_TextXml() throws Exception {
        String result = fsMimeTypeHelper.getExtension("text/xml");
        
        Assertions.assertEquals(".xml", result);
    }

    @Test
    public void testGetExtension_Pdf() throws Exception {
        String result = fsMimeTypeHelper.getExtension("application/pdf");
        
        Assertions.assertEquals(".pdf", result);
    }
    
}
