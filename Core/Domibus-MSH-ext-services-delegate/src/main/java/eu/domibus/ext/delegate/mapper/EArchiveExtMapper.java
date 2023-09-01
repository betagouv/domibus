package eu.domibus.ext.delegate.mapper;

import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.ext.domain.archive.BatchDTO;
import eu.domibus.ext.domain.archive.BatchRequestType;
import eu.domibus.ext.domain.archive.BatchStatusDTO;
import eu.domibus.ext.exceptions.DomibusEArchiveExtException;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
@Mapper(componentModel = "spring")
public interface EArchiveExtMapper {

    @Mapping(source = "timestamp", target = "enqueuedTimestamp")
    @Mapping(source = "requestType", target = "requestType", qualifiedByName = "stringToBatchRequestType")
    @Mapping(source = "messageEndId", target = "messageEndDate", qualifiedByName = "messageIdToDate")
    @Mapping(source = "messageStartId", target = "messageStartDate", qualifiedByName = "messageIdToDate")
    BatchDTO archiveBatchToBatch(EArchiveBatchRequestDTO archiveBatchDTO);

    BatchStatusDTO archiveBatchToBatchStatus(EArchiveBatchRequestDTO archiveBatchDTO);

    @Named("stringToBatchRequestType")
    default BatchRequestType stringToBatchRequestType(String requestType) {
        if (isEmpty(requestType)) {
            return null;
        }
        if (Arrays.stream(BatchRequestType.values()).anyMatch(batchRequestType -> StringUtils.equalsIgnoreCase(requestType, batchRequestType.name()))) {
            return BatchRequestType.valueOf(requestType);
        }
        if (StringUtils.equalsIgnoreCase(requestType, EArchiveRequestType.SANITIZER.name())) {
            return BatchRequestType.CONTINUOUS;
        }
        throw new DomibusEArchiveExtException("RequestType unknown [" + requestType + "]");
    }

    @Named("messageIdToDate")
    default Long messageIdToMessageDateHour(Long messageId) {
        TsidUtil tsidUtil = SpringContextProvider.getApplicationContext().getBean(TsidUtil.BEAN_NAME, TsidUtil.class);
        return messageId == null ? null : tsidUtil.getDateFromTsid(messageId);
    }
}
