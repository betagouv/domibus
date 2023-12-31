package eu.domibus.plugin.fs;

import eu.domibus.common.MessageStatus;
import eu.domibus.plugin.fs.configuration.FSPluginConfiguration;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@ExtendWith(JMockitExtension.class)
public class FSFileNameHelperTest {

    @Tested
    FSFileNameHelper fsFileNameHelper;

    public FSFileNameHelperTest() {
        List<String> stateSuffixes = new FSPluginConfiguration().getStateSuffixes();
        fsFileNameHelper = new FSFileNameHelper(stateSuffixes);
    }

    @Test
    public void testIsAnyState() {
        boolean result = fsFileNameHelper.isAnyState("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf.READY_TO_SEND");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsAnyState_Fail() {
        boolean result = fsFileNameHelper.isAnyState("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf");

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsProcessed() {
        boolean result = fsFileNameHelper.isProcessed("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsProcessed_NoExtension() {
        boolean result = fsFileNameHelper.isProcessed("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsProcessed_Fail1() {
        // missing one character in UUID in message ID
        boolean result = fsFileNameHelper.isProcessed("invoice_c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf");

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsProcessed_Fail2() {
        // missing underscore before UUID
        boolean result = fsFileNameHelper.isProcessed("invoice3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf");

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsProcessed_Fail3() {
        // missing message ID
        boolean result = fsFileNameHelper.isProcessed("invoice.pdf");

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsMessageRelated() {
        boolean result = FSFileNameHelper.isMessageRelated("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsMessageRelated_Fail1() {
        // missing message ID
        boolean result = FSFileNameHelper.isMessageRelated("invoice.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsLockFile() {
        boolean result = fsFileNameHelper.isLockFile("large_invoice.pdf.lock");

        Assertions.assertTrue(result);
    }

    @Test
    public void testIsLockFile_Fail() {
        boolean result = fsFileNameHelper.isLockFile("large_invoice.pdf");

        Assertions.assertFalse(result);
    }

    @Test
    public void testStripLockSuffix() {
        String result = fsFileNameHelper.stripLockSuffix("large_invoice.pdf.lock");

        Assertions.assertEquals("large_invoice.pdf", result);
    }

    @Test
    public void testIsMessageRelated_Fail2() {
        // missing one character in UUID in message ID
        boolean result = fsFileNameHelper.isMessageRelated("invoice_c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsMessageRelated_Fail3() {
        // missing underscore before UUID
        boolean result = fsFileNameHelper.isMessageRelated("invoice3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertFalse(result);
    }

    @Test
    public void testDeriveFileName() {
        String result = fsFileNameHelper.deriveFileName("invoice.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertEquals("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", result);
    }

    @Test
    public void testDeriveFileName_MultipleParts1() {
        String result = fsFileNameHelper.deriveFileName("invoice.foo.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertEquals("invoice.foo_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", result);
    }

    @Test
    public void testDeriveFileName_MultipleParts2() {
        String result = fsFileNameHelper.deriveFileName("invoice.foo.bar.pdf", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertEquals("invoice.foo.bar_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", result);
    }

    @Test
    public void testDeriveFileName_NoExtension() {
        String result = fsFileNameHelper.deriveFileName("invoice", "3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu");

        Assertions.assertEquals("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu", result);
    }

    @Test
    public void testDeriveFileName_NoStatus() {
        String result = fsFileNameHelper.deriveFileName("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf", MessageStatus.READY_TO_SEND);

        Assertions.assertEquals("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf.READY_TO_SEND", result);
    }

    @Test
    public void testDeriveFileName_ReplaceStatus() {
        String result = fsFileNameHelper.deriveFileName("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf.READY_TO_SEND", MessageStatus.SEND_ENQUEUED);

        Assertions.assertEquals("invoice_3c5558e4-7b6d-11e7-bb31-be2e44b06b34@domibus.eu.pdf.SEND_ENQUEUED", result);
    }

    @Test
    public void testDeriveSentDirectoryLocation() {
        String result = fsFileNameHelper.deriveSentDirectoryLocation("smb://example.org/fs_plugin_data/OUT");

        Assertions.assertEquals("smb://example.org/fs_plugin_data/SENT/", result);
    }

    @Test
    public void testDeriveSentDirectoryLocation_Domain1() {
        String result = fsFileNameHelper.deriveSentDirectoryLocation("smb://example.org/fs_plugin_data/DOMAIN1/OUT/Invoice");

        Assertions.assertEquals("smb://example.org/fs_plugin_data/DOMAIN1/SENT/Invoice", result);
    }

    @Test
    public void testDeriveSentDirectoryLocation_Domain1Out() {
        String result = fsFileNameHelper.deriveSentDirectoryLocation("smb://example.org/fs_plugin_data/DOMAIN1/OUT/OUTGOING");

        Assertions.assertEquals("smb://example.org/fs_plugin_data/DOMAIN1/SENT/OUTGOING", result);
    }

    @Test
    public void testDeriveSentDirectoryLocation_OutDomain() {
        String result = fsFileNameHelper.deriveSentDirectoryLocation("smb://example.org/fs_plugin_data/OUTDOMAIN/OUT/OUTGOING");

        Assertions.assertEquals("smb://example.org/fs_plugin_data/OUTDOMAIN/SENT/OUTGOING", result);
    }


    @Test
    public void testGetLockFilename(@Injectable FileObject file) {
        final String filename = "invoice.pdf";
        new Expectations() {{
            file.getName().getBaseName();
            this.result = filename;
        }};

        final String lockFilename = fsFileNameHelper.getLockFilename(file);
        Assertions.assertEquals(lockFilename, filename + ".lock");
    }

    @Test
    public void getRelativeName() throws FileSystemException {
        String location = "ram:///FSSendMessagesServiceTest";

        FileSystemManager fsManager = VFS.getManager();
        FileObject rootDir = fsManager.resolveFile(location);
        rootDir.createFolder();

        FileObject rootFolder = rootDir.resolveFile(FSFilesManager.OUTGOING_FOLDER);
        rootFolder.createFolder();

        String subFolderName = "folder1";
        FileObject subFolder = rootDir.resolveFile(subFolderName);
        subFolder.createFolder();

        String fileName = "content.xml";
        FileObject contentFile = subFolder.resolveFile(fileName);
        contentFile.createFile();

        final Optional<String> filePath = fsFileNameHelper.getRelativeName(rootFolder, contentFile);

        Assertions.assertEquals(filePath.get(), "../" + subFolderName + "/" + fileName);
    }
}
