package eu.domibus.core.util;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author François Gautier
 * @since 4.2
 */
public class DomibusDateFormatterTest {

    public static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS][.SSSSSS][.SSS][z]";
    public static final String VALID_DATE_STRING = "2020-06-02T20:00:00";
    private DomibusDateFormatter domibusDateFormatter;

    @BeforeEach
    public void setUp() {
        domibusDateFormatter = new DomibusDateFormatter(DateTimeFormatter.ofPattern(DEFAULT_PATTERN));
    }

    @Test
    public void fromString_ok() {
        Date date = domibusDateFormatter.fromString(VALID_DATE_STRING);
        LocalDateTime from = Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        Assertions.assertEquals(2020, from.getYear());
        Assertions.assertEquals(20, from.getHour());
        Assertions.assertEquals(0, from.getMinute());
        Assertions.assertEquals(0, from.getSecond());
    }

    @Test
    public void fromString_exception() {
        try {
            domibusDateFormatter.fromString("tomorrow");
            Assertions.fail();
        } catch (DomibusCoreException e) {
            assertThat(e.getError(), Is.is(DomibusCoreErrorCode.DOM_007));
        }

    }
}
