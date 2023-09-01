package eu.domibus.api.earchive;

import java.util.Date;
import java.util.List;

public interface EArchiveBatchUtil {
    List<String> getMessageIds(List<EArchiveBatchUserMessage> userMessageDtos);

    List<Long> getEntityIds(List<EArchiveBatchUserMessage> userMessageDtos);

    Long extractDateFromPKUserMessageId(Long pkUserMessage);

    Long dateToPKUserMessageId(Long pkUserMessageDate);

    int getLastIndex(List<EArchiveBatchUserMessage> batchUserMessages);

    Long getMessageStartDate(List<EArchiveBatchUserMessage> batchUserMessages, int index);

    Date getBatchMessageDate(Long userMessageEntityId);
}
