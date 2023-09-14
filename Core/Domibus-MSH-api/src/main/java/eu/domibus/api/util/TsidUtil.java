package eu.domibus.api.util;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Cosmin Baciu
 * @since 5.2
 */
public interface TsidUtil {

    String BEAN_NAME = "domibusTsidUtil";

    int RANDOM_BITS = 22;

    /**
     * There are 22 bits dedicated to the RANDOM_PART. The max value generated is 2^22 - 1 = 4194303
     */
    long RANDOM_MAX_VALUE = 4194303;

    Long zonedTimeDateToTsid(ZonedDateTime zonedDateTime);

    Long getDateFromTsid(Long tsid);

    Long dateToTsid(Date date);

    Long localDateTimeToTsid(LocalDateTime date);

    Long dateToTsid(Long date);

    Long zonedTimeDateToMaxTsid(ZonedDateTime zonedDateTime);
}
