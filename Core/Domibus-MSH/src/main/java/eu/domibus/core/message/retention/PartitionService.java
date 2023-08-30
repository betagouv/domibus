package eu.domibus.core.message.retention;

import eu.domibus.api.model.DatabasePartition;
import eu.domibus.api.util.DateUtil;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * This service class is responsible for the handling of partitions
 *
 * @author idragusa
 * @since 5.0
 */
@Service
public class PartitionService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartitionService.class);

    protected DateUtil dateUtil;

    protected TsidUtil tsidUtil;

    public PartitionService(DateUtil dateUtil, TsidUtil tsidUtil) {
        this.dateUtil = dateUtil;
        this.tsidUtil = tsidUtil;
    }

    public Long getPartitionHighValueFromDate(Date partitionDate) {
        Long highValue = tsidUtil.dateToTsid(partitionDate);
        LOG.debug("Get partition highValue from date [{}], highValue [{}]", partitionDate, highValue);
        return highValue;
    }
}
