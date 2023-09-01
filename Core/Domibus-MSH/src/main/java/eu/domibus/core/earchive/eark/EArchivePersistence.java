package eu.domibus.core.earchive.eark;

import eu.domibus.core.earchive.BatchEArchiveDTO;
import eu.domibus.api.earchive.EArchiveBatchUserMessage;

import java.util.Date;
import java.util.List;

/**
 * @author François Gautier
 * @since 5.0
 */
public interface EArchivePersistence {

    DomibusEARKSIPResult createEArkSipStructure(BatchEArchiveDTO batchEArchiveDTO, List<EArchiveBatchUserMessage> userMessageEntityIds, Date messageStartDate, Date messageEndDate);

}
