package eu.domibus.core.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.api.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.util.JsonFormatterConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@ExtendWith(SpringExtension.class)
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
        Assertions.assertNotNull(result);
        Assertions.assertEquals(testEntity.getBatchId(), result.getBatchId());
        Assertions.assertEquals(testEntity.getDateRequested(), result.getTimestamp());
        Assertions.assertEquals(testEntity.getEArchiveBatchStatus().name(), result.getStatus());
        Assertions.assertEquals(testEntity.getDomibusCode(), result.getDomibusCode());
        Assertions.assertEquals(testEntity.getMessage(), result.getMessage());
        Assertions.assertEquals(testEntity.getFirstPkUserMessage(), result.getMessageStartId());
        Assertions.assertEquals(testEntity.getLastPkUserMessage(), result.getMessageEndId());
        Assertions.assertEquals(testEntity.getManifestChecksum(), result.getManifestChecksum());

        Assertions.assertTrue(result.getMessages().isEmpty());
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
        Assertions.assertNotNull(result);
        Assertions.assertEquals(testEntity.getBatchId(), result.getBatchId());
        Assertions.assertEquals(testEntity.getDateRequested(), result.getTimestamp());
        Assertions.assertEquals(testEntity.getEArchiveBatchStatus().name(), result.getStatus());
        Assertions.assertEquals(testEntity.getDomibusCode(), result.getDomibusCode());
        Assertions.assertEquals(testEntity.getMessage(), result.getMessage());
        Assertions.assertEquals(testEntity.getFirstPkUserMessage(), result.getMessageStartId());
        Assertions.assertEquals(testEntity.getLastPkUserMessage(), result.getMessageEndId());
        Assertions.assertEquals(testEntity.getManifestChecksum(), result.getManifestChecksum());
        // test list messages
        Assertions.assertEquals(batchUserMessages.size(), result.getMessages().size());
        Assertions.assertArrayEquals(
                batchUserMessages.stream().map(EArchiveBatchUserMessage::getMessageId).collect(Collectors.toList()).toArray(new String[]{}),
                result.getMessages().toArray(new String[]{}));

    }
}
