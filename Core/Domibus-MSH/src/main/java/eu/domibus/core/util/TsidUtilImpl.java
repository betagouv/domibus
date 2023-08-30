package eu.domibus.core.util;

import eu.domibus.api.util.TsidUtil;
import io.hypersistence.tsid.TSID;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Cosmin Baciu
 * @since 5.2
 */
@Service
public class TsidUtilImpl implements TsidUtil {

    @Override
    public long zonedTimeDateToTsid(ZonedDateTime zonedDateTime) {
        return epochMilliToTsid(zonedDateTime.toInstant().toEpochMilli());
    }

    protected long epochMilliToTsid(long epochMilli) {
        return (epochMilli - TSID.TSID_EPOCH) << RANDOM_BITS;
    }

    @Override
    public long getDateFromTsid(long tsid) {
        return (tsid >> RANDOM_BITS) + TSID.TSID_EPOCH;
    }

    @Override
    public long dateToTsid(Date date) {
        return epochMilliToTsid(date.getTime());
    }

    @Override
    public long dateToTsid(long date) {
        return epochMilliToTsid(date);
    }

    @Override
    public long zonedTimeDateToMaxTsid(ZonedDateTime zonedDateTime) {
        final long dateToTsid = zonedTimeDateToTsid(zonedDateTime);
        return dateToTsid + TsidUtil.RANDOM_MAX_VALUE;
    }

    @Override
    public long localDateTimeToTsid(LocalDateTime localDateTime) {
        final Date date = Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
        return dateToTsid(date);
    }
}
