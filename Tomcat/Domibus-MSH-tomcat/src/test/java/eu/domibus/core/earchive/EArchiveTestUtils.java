package eu.domibus.core.earchive;

import eu.domibus.api.earchive.EArchiveBatchEntity;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;

import java.util.Date;

public class EArchiveTestUtils {

    public static EArchiveBatchEntity createEArchiveBatchEntity(final String batchIdq,
                                                                final EArchiveRequestType requestType,
                                                                final EArchiveBatchStatus eArchiveBatchStatus,
                                                                final Date dateRequested,
                                                                final Long firstPkUserMessage,
                                                                final Long lastPkUserMessage,
                                                                final Integer batchSize,
                                                                final String storageLocation
                                                                ) {
        EArchiveBatchEntity instance = new EArchiveBatchEntity();
        instance.setBatchId(batchIdq);
        instance.setRequestType(requestType);
        instance.setEArchiveBatchStatus(eArchiveBatchStatus);
        instance.setDateRequested(dateRequested);
        instance.setFirstPkUserMessage(firstPkUserMessage);
        instance.setLastPkUserMessage(lastPkUserMessage);
        instance.setBatchSize(batchSize);
        instance.setStorageLocation(storageLocation);
        return instance;
    }
}

