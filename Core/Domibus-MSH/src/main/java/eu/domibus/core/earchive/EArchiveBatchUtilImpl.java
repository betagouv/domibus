package eu.domibus.core.earchive;

import eu.domibus.api.earchive.EArchiveBatchUserMessage;
import eu.domibus.api.earchive.EArchiveBatchUtil;
import eu.domibus.api.util.DateUtil;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.core.message.UserMessageLogDao;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
@Service
public class EArchiveBatchUtilImpl implements EArchiveBatchUtil {
    private final UserMessageLogDao userMessageLogDao;

    protected TsidUtil tsidUtil;

    protected DateUtil dateUtil;

    public EArchiveBatchUtilImpl(UserMessageLogDao userMessageLogDao, TsidUtil tsidUtil, DateUtil dateUtil) {
        this.userMessageLogDao = userMessageLogDao;
        this.tsidUtil = tsidUtil;
        this.dateUtil = dateUtil;
    }

    @Override
    public List<String> getMessageIds(List<EArchiveBatchUserMessage> userMessageDtos) {
        if (CollectionUtils.isEmpty(userMessageDtos)) {
            return new ArrayList<>();
        }
        return userMessageDtos.stream()
                .map(EArchiveBatchUserMessage::getMessageId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getEntityIds(List<EArchiveBatchUserMessage> userMessageDtos) {
        if (CollectionUtils.isEmpty(userMessageDtos)) {
            return new ArrayList<>();
        }
        return userMessageDtos.stream().map(EArchiveBatchUserMessage::getUserMessageEntityId).collect(Collectors.toList());
    }

    @Override
    public Long extractDateFromPKUserMessageId(Long pkUserMessage) {
        if (pkUserMessage == null) {
            return null;
        }
        return tsidUtil.getDateFromTsid(pkUserMessage);
    }

    @Override
    public Long dateToPKUserMessageId(Long pkUserMessageDate) {
        final LocalDateTime date = dateUtil.getLocalDateTimeFromDateWithHour(pkUserMessageDate);
        return tsidUtil.localDateTimeToTsid(date);
    }

    @Override
    public int getLastIndex(List<EArchiveBatchUserMessage> batchUserMessages) {
        if (org.springframework.util.CollectionUtils.isEmpty(batchUserMessages)) {
            return 0;
        }
        return batchUserMessages.size() - 1;
    }

    @Override
    public Long getMessageStartDate(List<EArchiveBatchUserMessage> batchUserMessages, int index) {
        if (org.springframework.util.CollectionUtils.isEmpty(batchUserMessages)) {
            return null;
        }
        return batchUserMessages.get(index).getUserMessageEntityId();
    }

    @Override
    public Date getBatchMessageDate(Long userMessageEntityId) {
        Date messageStartDate = null;
        if (userMessageEntityId != null) {
            messageStartDate = userMessageLogDao.findByEntityId(userMessageEntityId).getReceived();
        }
        return messageStartDate;
    }

}
