package eu.domibus.core.converter;


import eu.domibus.api.earchive.EArchiveBatchRequestDTO;
import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.api.earchive.EArchiveBatchUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
@Mapper(componentModel = "spring")
public abstract class EArchiveBatchMapper {

    @Autowired
    protected EArchiveBatchUtil archiveBatchUtils;

    @Mapping(ignore = true, target = "version")
    @Mapping(source = "EArchiveBatchStatus", target = "status")
    @Mapping(source = "message", target = "message")
    @Mapping(source = "dateRequested", target = "timestamp")
    @Mapping(source = "lastPkUserMessage", target = "messageEndId")
    @Mapping(source = "firstPkUserMessage", target = "messageStartId")
    @Mapping(source = "batchBaseEntity", target = "messages", qualifiedByName = "userMessageListToMessageIdList")
    public abstract EArchiveBatchRequestDTO eArchiveBatchRequestEntityToDto(EArchiveBatchEntity batchBaseEntity);

    @Named("userMessageListToMessageIdList")
    public List<String> userMessageListToMessageIdList(EArchiveBatchEntity entity) {
        return archiveBatchUtils.getMessageIds(entity.geteArchiveBatchUserMessages());
    }


}
