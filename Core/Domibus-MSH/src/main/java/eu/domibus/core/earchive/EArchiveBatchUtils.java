package eu.domibus.core.earchive;

import eu.domibus.api.util.TsidUtil;
import eu.domibus.core.message.UserMessageLogDao;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
@Component
public class EArchiveBatchUtils {
    private final UserMessageLogDao userMessageLogDao;

    protected TsidUtil tsidUtil;

    public EArchiveBatchUtils(UserMessageLogDao userMessageLogDao, TsidUtil tsidUtil) {
        this.userMessageLogDao = userMessageLogDao;
        this.tsidUtil = tsidUtil;
    }

    public List<String> getMessageIds(List<EArchiveBatchUserMessage> userMessageDtos) {
        if (CollectionUtils.isEmpty(userMessageDtos)) {
            return new ArrayList<>();
        }
        return userMessageDtos.stream()
                .map(EArchiveBatchUserMessage::getMessageId)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Long> getEntityIds(List<EArchiveBatchUserMessage> userMessageDtos) {
        if (CollectionUtils.isEmpty(userMessageDtos)) {
            return new ArrayList<>();
        }
        return userMessageDtos.stream().map(EArchiveBatchUserMessage::getUserMessageEntityId).collect(Collectors.toList());
    }

    public Long extractDateFromPKUserMessageId(Long pkUserMessage) {
        if (pkUserMessage == null) {
            return null;
        }
        return tsidUtil.getDateFromTsid(pkUserMessage);
    }

    public Long dateToPKUserMessageId(Long pkUserMessageDate) {
        return tsidUtil.dateToTsid(new Date(pkUserMessageDate));
    }

    public int getLastIndex(List<EArchiveBatchUserMessage> batchUserMessages) {
        if (org.springframework.util.CollectionUtils.isEmpty(batchUserMessages)) {
            return 0;
        }
        return batchUserMessages.size() - 1;
    }

    public Long getMessageStartDate(List<EArchiveBatchUserMessage> batchUserMessages, int index) {
        if (org.springframework.util.CollectionUtils.isEmpty(batchUserMessages)) {
            return null;
        }
        return batchUserMessages.get(index).getUserMessageEntityId();
    }

    public Date getBatchMessageDate(Long userMessageEntityId) {
        Date messageStartDate = null;
        if (userMessageEntityId != null) {
            messageStartDate = userMessageLogDao.findByEntityId(userMessageEntityId).getReceived();
        }
        return messageStartDate;
    }

}
