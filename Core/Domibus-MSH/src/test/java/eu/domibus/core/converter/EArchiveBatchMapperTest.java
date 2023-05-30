package eu.domibus.core.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.core.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.util.JsonFormatterConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = {EArchiveBatchMapperImpl.class, JsonFormatterConfiguration.class}
)
public class EArchiveBatchMapperTest extends AbstractMapperTest {


    @Autowired
    ObjectMapper objectMapper;

    @Qualifier("eArchiveBatchMapper")
    @Autowired
    EArchiveBatchMapper testInstance;

    @Test
    public void testEArchiveBatchSummaryEntityToDto() {
        // given
        EArchiveBatchEntity testEntity = new EArchiveBatchEntity();
        testEntity.setBatchId(UUID.randomUUID().toString());
        testEntity.setBatchSize(1123);
        testEntity.setDateRequested(Calendar.getInstance().getTime());
        testEntity.setEArchiveBatchStatus(EArchiveBatchStatus.EXPORTED);
        testEntity.setDomibusCode("DOM10");
        testEntity.setMessage("Error message: " + UUID.randomUUID());
        testEntity.setFirstPkUserMessage(10L);
        testEntity.setLastPkUserMessage(20L);
        testEntity.setStorageLocation("/test");
        testEntity.setManifestChecksum("sha256:test");
        //when
        EArchiveBatchRequestDTO result = testInstance.eArchiveBatchRequestEntityToDto(testEntity);
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(testEntity.getBatchId(), result.getBatchId());
        Assert.assertEquals(testEntity.getDateRequested(), result.getTimestamp());
        Assert.assertEquals(testEntity.getEArchiveBatchStatus().name(), result.getStatus());
        Assert.assertEquals(testEntity.getDomibusCode(), result.getDomibusCode());
        Assert.assertEquals(testEntity.getMessage(), result.getMessage());
        Assert.assertEquals(testEntity.getFirstPkUserMessage(), result.getMessageStartId());
        Assert.assertEquals(testEntity.getLastPkUserMessage(), result.getMessageEndId());
        Assert.assertEquals(testEntity.getManifestChecksum(), result.getManifestChecksum());

        Assert.assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testEArchiveBatchEntityToDto() {

        // given
        List<EArchiveBatchUserMessage> batchUserMessages = Arrays.asList(new EArchiveBatchUserMessage(1L, UUID.randomUUID().toString()),
                new EArchiveBatchUserMessage(2L, UUID.randomUUID().toString()),
                new EArchiveBatchUserMessage(3L, UUID.randomUUID().toString()));
        EArchiveBatchEntity testEntity = new EArchiveBatchEntity();
        testEntity.setBatchId(UUID.randomUUID().toString());
        testEntity.setBatchSize(1123);
        testEntity.setDateRequested(Calendar.getInstance().getTime());
        testEntity.setEArchiveBatchStatus(EArchiveBatchStatus.EXPORTED);
        testEntity.setDomibusCode("DOM10");
        testEntity.setMessage("Message: " + UUID.randomUUID());
        testEntity.setFirstPkUserMessage(10L);
        testEntity.setLastPkUserMessage(20L);
        testEntity.setStorageLocation("/test");
        testEntity.setManifestChecksum("sha256:test");
        testEntity.seteArchiveBatchUserMessages(batchUserMessages);
        // when
        EArchiveBatchRequestDTO result = testInstance.eArchiveBatchRequestEntityToDto(testEntity);
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(testEntity.getBatchId(), result.getBatchId());
        Assert.assertEquals(testEntity.getDateRequested(), result.getTimestamp());
        Assert.assertEquals(testEntity.getEArchiveBatchStatus().name(), result.getStatus());
        Assert.assertEquals(testEntity.getDomibusCode(), result.getDomibusCode());
        Assert.assertEquals(testEntity.getMessage(), result.getMessage());
        Assert.assertEquals(testEntity.getFirstPkUserMessage(), result.getMessageStartId());
        Assert.assertEquals(testEntity.getLastPkUserMessage(), result.getMessageEndId());
        Assert.assertEquals(testEntity.getManifestChecksum(), result.getManifestChecksum());
        // test list messages
        Assert.assertEquals(batchUserMessages.size(), result.getMessages().size());
        Assert.assertArrayEquals(
                batchUserMessages.stream().map(EArchiveBatchUserMessage::getMessageId).collect(Collectors.toList()).toArray(new String[]{}),
                result.getMessages().toArray(new String[]{}));

    }
}