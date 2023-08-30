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

public class TsidUtilImplIT extends AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TsidUtilImplIT.class);

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
}
