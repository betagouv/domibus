package eu.domibus.core.util;

import eu.domibus.AbstractIT;
import eu.domibus.api.util.TsidUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TsidUtilImpl extends AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TsidUtilImpl.class);

    @Autowired
    TsidUtil tsidUtil;

    @Test
    public void testMinValue() {
        final LocalDateTime localDateTime = LocalDateTime.of(2023, 6, 24, 7, 30, 0);
        final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneOffset.UTC);

        final long minDateToTsid = tsidUtil.zonedTimeDateToTsid(zonedDateTime);
        final long maxDateToTsid = tsidUtil.zonedTimeDateToMaxTsid(zonedDateTime);
        LOG.info("Date [{}], TSID min [{}], TSID max ", localDateTime, minDateToTsid, maxDateToTsid);
    }
}
