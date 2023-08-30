package eu.domibus.api.util;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Cosmin Baciu
 * @since 5.2
 */
public interface TsidUtil {

    int RANDOM_BITS = 22;

    /**
     * There are 22 bits dedicated to the RANDOM_PART. The max value generated is 2^22 - 1 = 4194303
     */
    long RANDOM_MAX_VALUE = 4194303;

    long zonedTimeDateToTsid(ZonedDateTime zonedDateTime);

    long getDateFromTsid(long tsid);

    long dateToTsid(Date date);

    long localDateTimeToTsid(LocalDateTime date);

    long dateToTsid(long date);

    long zonedTimeDateToMaxTsid(ZonedDateTime zonedDateTime);
}
