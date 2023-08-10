package eu.domibus.ext.delegate.services.util;

import eu.domibus.api.exceptions.DomibusDateTimeException;
import eu.domibus.api.util.DateUtil;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.ext.exceptions.DomibusDateTimeExtException;
import eu.domibus.ext.services.DateExtService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Delegate service exposing date utility operations.
 *
 * @author Sebastian-Ion TINCU
 * @since 5.0
 */
@Service
public class DateUtilDelegate implements DateExtService {

    private final DateUtil dateUtil;

    private final TsidUtil tsidUtil;

    public DateUtilDelegate(DateUtil dateUtil, TsidUtil tsidUtil) {
        this.dateUtil = dateUtil;
        this.tsidUtil = tsidUtil;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getUtcDate() {
        return dateUtil.getUtcDate();
    }


    @Override
    public LocalDateTime getUtcLocalDateTime(LocalDateTime localDateTime) {
        return dateUtil.getUtcLocalDateTime(localDateTime);
    }


    @Override
    public Long getIdPkDateHour(String date) {
        try {
            final LocalDateTime localDateTime = dateUtil.getLocalDateTime(date);
            return tsidUtil.localDateTimeToTsid(localDateTime);
        } catch (DomibusDateTimeException e) {
            throw new DomibusDateTimeExtException("Could not get IdPK from date [" + date + "]", e);
        }
    }

}
