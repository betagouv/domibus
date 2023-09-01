package eu.domibus.ext.delegate.mapper;

import eu.domibus.AbstractIT;
import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.ext.domain.archive.BatchDTO;
import eu.domibus.ext.domain.archive.BatchRequestType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
@Transactional
public class EArchiveExtMapperTestIT extends AbstractIT {

    @Autowired
    private EArchiveExtMapper archiveExtMapper;

    @Autowired
    TsidUtil tsidUtil;

    @Test
    public void testArchiveBatchToQueuedBatch_Manual() {
        // given
        long datetime = 21080800L;
        EArchiveBatchRequestDTO toConvert = new EArchiveBatchRequestDTO();
        toConvert.setBatchId(UUID.randomUUID().toString());
        toConvert.setRequestType(BatchRequestType.MANUAL.name());
        toConvert.setMessages(Arrays.asList("messageId1", "messageId1"));
        toConvert.setTimestamp(Calendar.getInstance().getTime());

        Long messageStartId = tsidUtil.zonedTimeDateToTsid(LocalDateTime.of(2021, 8, 8, 0, 0, 1).atZone(ZoneOffset.UTC));
        Long messageEndId = tsidUtil.zonedTimeDateToTsid(LocalDateTime.of(2021, 8, 8, 0, 0, 10).atZone(ZoneOffset.UTC));

        toConvert.setMessageStartId(messageStartId);
        toConvert.setMessageEndId(messageEndId);
        // when
        final BatchDTO converted = archiveExtMapper.archiveBatchToBatch(toConvert);
        // then
        assertEquals(toConvert.getBatchId(), converted.getBatchId());
        assertArrayEquals(toConvert.getMessages().toArray(), converted.getMessages().toArray());
        assertEquals(toConvert.getRequestType(), converted.getRequestType().name());
        assertEquals(toConvert.getTimestamp(), converted.getEnqueuedTimestamp());
        // extract just dates
        assertEquals(datetime, converted.getMessageEndDate().longValue());
        assertEquals(datetime, converted.getMessageStartDate().longValue());
    }

    @Test
    public void archiveBatchToExportBatch_continuous() {
        // given
        long datetime = 21080800L;
        EArchiveBatchRequestDTO toConvert = new EArchiveBatchRequestDTO();
        toConvert.setBatchId(UUID.randomUUID().toString());
        toConvert.setRequestType(BatchRequestType.CONTINUOUS.name());
        toConvert.setStatus(EArchiveBatchStatus.EXPORTED.name());
        toConvert.setMessages(Arrays.asList("messageId1", "messageId1"));
        toConvert.setTimestamp(Calendar.getInstance().getTime());

        Long messageStartId = tsidUtil.zonedTimeDateToTsid(LocalDateTime.of(2021, 8, 8, 0, 0, 1).atZone(ZoneOffset.UTC));
        Long messageEndId = tsidUtil.zonedTimeDateToTsid(LocalDateTime.of(2021, 8, 8, 0, 0, 10).atZone(ZoneOffset.UTC));

        toConvert.setMessageStartId(messageStartId);
        toConvert.setMessageEndId(messageEndId);

        // when
        final BatchDTO converted = archiveExtMapper.archiveBatchToBatch(toConvert);
        // then
        assertEquals(toConvert.getBatchId(), converted.getBatchId());
        assertArrayEquals(toConvert.getMessages().toArray(), converted.getMessages().toArray());
        assertEquals(toConvert.getRequestType(), converted.getRequestType().name());
        assertEquals(toConvert.getStatus(), converted.getStatus().name());
        assertEquals(toConvert.getTimestamp(), converted.getEnqueuedTimestamp());
        // extract just dates
        assertEquals(datetime, converted.getMessageEndDate().longValue());
        assertEquals(datetime, converted.getMessageStartDate().longValue());
    }


    @Test
    public void archiveBatchToExportBatch_sanitizer() {
        // given
        long datetime = 21080800L;
        EArchiveBatchRequestDTO toConvert = new EArchiveBatchRequestDTO();
        toConvert.setBatchId(UUID.randomUUID().toString());
        toConvert.setRequestType("SANITIZER");
        toConvert.setStatus(EArchiveBatchStatus.EXPORTED.name());
        toConvert.setMessages(Arrays.asList("messageId1", "messageId1"));
        toConvert.setTimestamp(Calendar.getInstance().getTime());

        Long messageStartId = tsidUtil.zonedTimeDateToTsid(LocalDateTime.of(2021, 8, 8, 0, 0, 1).atZone(ZoneOffset.UTC));
        Long messageEndId = tsidUtil.zonedTimeDateToTsid(LocalDateTime.of(2021, 8, 8, 0, 0, 10).atZone(ZoneOffset.UTC));

        toConvert.setMessageStartId(messageStartId);
        toConvert.setMessageEndId(messageEndId);
        // when
        final BatchDTO converted = archiveExtMapper.archiveBatchToBatch(toConvert);
        // then
        assertEquals(toConvert.getBatchId(), converted.getBatchId());
        assertArrayEquals(toConvert.getMessages().toArray(), converted.getMessages().toArray());
        assertEquals(BatchRequestType.CONTINUOUS, converted.getRequestType());
        assertEquals(toConvert.getStatus(), converted.getStatus().name());
        assertEquals(toConvert.getTimestamp(), converted.getEnqueuedTimestamp());
        // extract just dates
        assertEquals(datetime, converted.getMessageEndDate().longValue());
        assertEquals(datetime, converted.getMessageStartDate().longValue());
    }

}
