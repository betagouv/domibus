package eu.domibus.core.earchive;

import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.RawEnvelopeDto;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.core.message.PartInfoService;
import eu.domibus.core.message.nonrepudiation.UserMessageRawEnvelopeDao;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import org.apache.commons.io.IOUtils;
import org.apache.tika.mime.MimeTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.activation.DataHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static eu.domibus.core.earchive.EArchivingService.SOAP_ENVELOPE_XML;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "TestMethodWithIncorrectSignature"})
public class EArchivingServiceTest {

    public static final String RAW_ENVELOPE_CONTENT = "rawEnvelopeDto";
    public static final String CID = "cid:";
    public static final String MESSAGE = "message";
    @Tested
    private EArchivingService eArchivingService;

    @Injectable
    private UserMessageService userMessageService;

    @Injectable
    private PartInfoService partInfoService;

    @Injectable
    private UserMessageRawEnvelopeDao userMessageRawEnvelopeDao;
    private String messageId;

    @Before
    public void setUp() throws Exception {
        messageId = UUID.randomUUID().toString();
    }

    @Test
    public void getFileName_ok(@Injectable PartInfo partInfo) {
        new Expectations() {{
            partInfo.getMime();
            result = MimeTypes.XML;

            partInfo.getHref();
            result = CID + MESSAGE;
        }};
        String fileName = eArchivingService.getFileName(partInfo);

        assertEquals(MESSAGE + ".attachment.xml", fileName);
    }

    @Test
    public void getFileName_noExtension(@Injectable PartInfo partInfo) {
        new Expectations() {{
            partInfo.getMime();
            result = "NoExtension";

            partInfo.getHref();
            result = CID + MESSAGE;
        }};
        String fileName = eArchivingService.getFileName(partInfo);

        assertEquals(MESSAGE + ".attachment", fileName);
    }

    @Test
    public void getFileName_nohref(@Injectable PartInfo partInfo) {
        new Expectations() {{
            partInfo.getMime();
            result = MimeTypes.XML;

            partInfo.getHref();
            result = null;
        }};
        String fileName = eArchivingService.getFileName(partInfo);

        assertEquals("bodyload.attachment.xml", fileName);
    }

    @Test
    public void getFileName_hrefNoCid(@Injectable PartInfo partInfo) {
        new Expectations() {{
            partInfo.getMime();
            result = MimeTypes.XML;

            partInfo.getHref();
            result = "NOCID";
        }};
        String fileName = eArchivingService.getFileName(partInfo);

        assertEquals("NOCID.attachment.xml", fileName);
    }

    @Test
    public void getArchivingFiles_happyFlow(@Injectable RawEnvelopeDto rawEnvelopeDto,
                                            @Injectable UserMessage userMessage,
                                            @Injectable PartInfo partInfo1,
                                            @Injectable DataHandler dataHandler,
                                            @Injectable InputStream inputStream) throws IOException {
        List<PartInfo> partInfos = Collections.singletonList(partInfo1);
        new Expectations() {{

            rawEnvelopeDto.getRawMessage();
            result = RAW_ENVELOPE_CONTENT.getBytes(StandardCharsets.UTF_8);

            userMessageService.getByMessageId(messageId);
            result = userMessage;

            userMessage.getEntityId();
            result = 1L;

            userMessageRawEnvelopeDao.findUserMessageEnvelopeById(1L);
            result = rawEnvelopeDto;

            partInfoService.findPartInfo(userMessage);
            result = partInfos;

            partInfo1.getPayloadDatahandler();
            result = dataHandler;

            dataHandler.getInputStream();
            result = inputStream;

            partInfo1.getMime();
            result = MimeTypes.XML;

            partInfo1.getHref();
            result = CID + MESSAGE;
        }};

        Map<String, InputStream> archivingFiles = eArchivingService.getArchivingFiles(messageId);

        new FullVerifications() {
        };

        Assert.assertThat(IOUtils.toString(archivingFiles.get(SOAP_ENVELOPE_XML), StandardCharsets.UTF_8), is(RAW_ENVELOPE_CONTENT));
        Assert.assertThat(archivingFiles.get(MESSAGE + ".attachment.xml"), is(inputStream));
    }

    @Test
    public void getArchivingFiles_partInfoWithoutDataHandler(@Injectable RawEnvelopeDto rawEnvelopeDto,
                                                             @Injectable UserMessage userMessage,
                                                             @Injectable PartInfo partInfo1,
                                                             @Injectable DataHandler dataHandler,
                                                             @Injectable InputStream inputStream) {
        List<PartInfo> partInfos = Collections.singletonList(partInfo1);
        new Expectations() {{

            rawEnvelopeDto.getRawMessage();
            result = RAW_ENVELOPE_CONTENT.getBytes(StandardCharsets.UTF_8);

            userMessageService.getByMessageId(messageId);
            result = userMessage;

            userMessage.getEntityId();
            result = 1L;

            userMessageRawEnvelopeDao.findUserMessageEnvelopeById(1L);
            result = rawEnvelopeDto;

            partInfoService.findPartInfo(userMessage);
            result = partInfos;

            partInfo1.getPayloadDatahandler();
            result = null;

            partInfo1.getHref();
            result = CID + MESSAGE;
        }};

        try {
            eArchivingService.getArchivingFiles(messageId);
            Assert.fail();
        } catch (DomibusEArchiveException e) {
            //ok
        }

        new FullVerifications() {
        };
    }

    @Test
    public void getArchivingFiles_partInfoIOException(@Injectable RawEnvelopeDto rawEnvelopeDto,
                                                      @Injectable UserMessage userMessage,
                                                      @Injectable PartInfo partInfo1,
                                                      @Injectable DataHandler dataHandler,
                                                      @Injectable InputStream inputStream) throws IOException {
        List<PartInfo> partInfos = Collections.singletonList(partInfo1);
        new Expectations() {{

            rawEnvelopeDto.getRawMessage();
            result = RAW_ENVELOPE_CONTENT.getBytes(StandardCharsets.UTF_8);

            userMessageService.getByMessageId(messageId);
            result = userMessage;

            userMessage.getEntityId();
            result = 1L;

            userMessageRawEnvelopeDao.findUserMessageEnvelopeById(1L);
            result = rawEnvelopeDto;

            partInfoService.findPartInfo(userMessage);
            result = partInfos;

            partInfo1.getPayloadDatahandler();
            result = dataHandler;

            dataHandler.getInputStream();
            result = new IOException();

            partInfo1.getMime();
            result = MimeTypes.XML;

            partInfo1.getHref();
            result = CID + MESSAGE;
        }};

        try {
            eArchivingService.getArchivingFiles(messageId);
            Assert.fail();
        } catch (DomibusEArchiveException e) {
            //ok
        }

        new FullVerifications() {
        };
    }


}