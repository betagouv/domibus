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
@Service(TsidUtil.BEAN_NAME)
public class TsidUtilImpl implements TsidUtil {

    @Override
    public Long zonedTimeDateToTsid(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return epochMilliToTsid(zonedDateTime.toInstant().toEpochMilli());
    }

    protected long epochMilliToTsid(long epochMilli) {
        return (epochMilli - TSID.TSID_EPOCH) << RANDOM_BITS;
    }

    @Override
    public Long getDateFromTsid(Long tsid) {
        if (tsid == null) {
            return null;
        }
        return (tsid >> RANDOM_BITS) + TSID.TSID_EPOCH;
    }

    @Override
    public Long dateToTsid(Date date) {
        if (date == null) {
            return null;
        }
        return epochMilliToTsid(date.getTime());
    }

    @Override
    public Long dateToTsid(Long date) {
        if (date == null) {
            return null;
        }
        return epochMilliToTsid(date);
    }

    @Override
    public Long zonedTimeDateToMaxTsid(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }

        final long dateToTsid = zonedTimeDateToTsid(zonedDateTime);
        return dateToTsid + TsidUtil.RANDOM_MAX_VALUE;
    }

    @Override
    public Long localDateTimeToTsid(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        final Date date = Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
        return dateToTsid(date);
    }
}
