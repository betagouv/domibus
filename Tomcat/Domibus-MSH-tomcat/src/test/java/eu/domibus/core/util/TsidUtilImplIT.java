package eu.domibus.core.util;


import eu.domibus.AbstractIT;
import eu.domibus.api.util.DateUtil;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Cosmin Baciu
 * @since 5.2
 */
public class TsidUtilImplIT extends AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TsidUtilImplIT.class);
    public static final long TSID_DIFFERENCE_BETWEEN_2_DATES_WITH_ONE_HOUR_DIFFERENCE = 15099494400000L;

    @Autowired
    TsidUtil tsidUtil;

    @Autowired
    DateUtil dateUtil;

    @Test
    public void testMinAndMaxValue() {
        final LocalDateTime localDateTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneOffset.UTC);

        final long minDateToTsid = tsidUtil.zonedTimeDateToTsid(zonedDateTime);
        final long maxDateToTsid = tsidUtil.zonedTimeDateToMaxTsid(zonedDateTime);
        LOG.info("Date [{}], TSID min [{}], TSID max [{}]", localDateTime, minDateToTsid, maxDateToTsid);
        Assertions.assertEquals(0L, minDateToTsid);
        Assertions.assertEquals(minDateToTsid + TsidUtil.RANDOM_MAX_VALUE, maxDateToTsid);
    }

    @Test
    public void getZonedTimeDateFromTsid() {
        final LocalDateTime localDateTime = LocalDateTime.of(2023, 3, 5, 0, 0, 0);
        final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneOffset.UTC);

        final long tsid = tsidUtil.zonedTimeDateToTsid(zonedDateTime);
        final long dateFromTsid = tsidUtil.getDateFromTsid(tsid);
        final LocalDateTime extractedLocalDateTimeFromTsid = dateUtil.convertToLocalDateTime(new Date(dateFromTsid));
        Assertions.assertEquals(localDateTime, extractedLocalDateTimeFromTsid);
    }

    @Test
    public void getDateFromTsid() {
        final LocalDateTime localDateTime = LocalDateTime.of(2023, 3, 5, 0, 0, 0);
        final Date initialDate = dateUtil.convertFromLocalDateTime(localDateTime);

        final long tsid = tsidUtil.dateToTsid(initialDate);
        final long dateFromTsid = tsidUtil.getDateFromTsid(tsid);
        final Date extractedDateFromTsid = new Date(dateFromTsid);
        Assertions.assertEquals(initialDate, extractedDateFromTsid);
    }

    @Test
    public void testTsidValueBetweenTwoDates() {
        final ZonedDateTime zonedDateTime1 = LocalDateTime.of(2023, 1, 1, 1, 0, 0).atZone(ZoneOffset.UTC);
        final long tsid1 = tsidUtil.zonedTimeDateToTsid(zonedDateTime1);

        final ZonedDateTime zonedDateTime2 = LocalDateTime.of(2023, 1, 1, 2, 0, 0).atZone(ZoneOffset.UTC);
        final long tsid2 = tsidUtil.zonedTimeDateToTsid(zonedDateTime2);

        LOG.info("ZonedDateTime1 [{}], TSID1  [{}], ZonedDateTime2 [{}], TSID2  [{}],", zonedDateTime1, tsid1, zonedDateTime2, tsid2);
        //15099494400000 represents the difference between two dates with one hour difference eg Date1; Date2=Date1 + 1h
        Assertions.assertEquals(TSID_DIFFERENCE_BETWEEN_2_DATES_WITH_ONE_HOUR_DIFFERENCE, tsid2 - tsid1);

        final ZonedDateTime zonedDateTime3 = LocalDateTime.of(2023, 1, 1, 1, 0, 0).atZone(ZoneOffset.UTC);
        final long tsid3 = tsidUtil.zonedTimeDateToTsid(zonedDateTime3);

        final ZonedDateTime zonedDateTime4 = LocalDateTime.of(2023, 1, 2, 1, 0, 0).atZone(ZoneOffset.UTC);
        final long tsid4 = tsidUtil.zonedTimeDateToTsid(zonedDateTime4);

        LOG.info("ZonedDateTime1 [{}], TSID1  [{}], ZonedDateTime2 [{}], TSID2  [{}],", zonedDateTime3, tsid3, zonedDateTime4, tsid4);
        //24 hours difference between date 3 and date 4
        Assertions.assertEquals(24 * TSID_DIFFERENCE_BETWEEN_2_DATES_WITH_ONE_HOUR_DIFFERENCE, tsid4 - tsid3);

    }
}
