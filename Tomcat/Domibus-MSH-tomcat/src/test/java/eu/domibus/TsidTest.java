package eu.domibus;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * @author Cosmin Baciu
 * @since 5.2
 */
public class TsidTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TsidTest.class);

    static final int RANDOM_BITS = 22;

    @Test
    public void dateToTsid() {
        TSID.Factory tsidFactory = TSID.Factory.builder()
                .withRandomFunction(TSID.Factory.THREAD_LOCAL_RANDOM_FUNCTION)
                .build();
        final TSID tsid = tsidFactory.generate();
        final long generatedNumber = tsid.toLong();
        LOG.info("TSID date [{}] [{}]", new Date(), generatedNumber);

        //add test to check the number that the number of seconds is correct using 4194304000

        printDate(LocalDateTime.of(2023, 6, 24, 7, 30, 0));
        printDate(LocalDateTime.of(2023, 6, 24, 7, 30, 1));
        printDate(LocalDateTime.of(2023, 6, 24, 7, 30, 2));
        printDate(LocalDateTime.of(2023, 6, 25, 7, 30, 3));
        printDate(LocalDateTime.of(2023, 6, 25, 7, 30, 4));
        final LocalDateTime tsidEpoch = LocalDateTime.ofInstant(Instant.ofEpochMilli(TSID.TSID_EPOCH), ZoneOffset.UTC);
        printDate(tsidEpoch);
        printDate(tsidEpoch.plusHours(1));
        printDate(tsidEpoch.plusHours(2));
        printDate(tsidEpoch.plusHours(3));
    }

    private static void printDate(LocalDateTime dateTime) {
        long milliseconds1 = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        final long dateToTsid = (new Date(milliseconds1).getTime() - TSID.TSID_EPOCH) << RANDOM_BITS;
        LOG.info("Date [{}] to TSID [{}]", dateTime, dateToTsid);
    }
}
