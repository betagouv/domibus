package eu.domibus.plugin.convert;

import eu.domibus.ext.exceptions.DomibusDateTimeExtException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * @author Sebastian-Ion TINCU
 * @since 5.0
 */
public class StringToTemporalAccessorConverterTest {

    private DateTimeFormatter formatter;

    private StringToTemporalAccessorConverter converter;

    @Test
    public void convert_returnsNullWhenSourceIsNull() {
        // GIVEN
        String source = null;
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(source);

        // THEN
        Assertions.assertNull(result, "Should have returned a null UTC local date time when the input source is null");
    }

    @Test
    public void convert_throwsParseExceptionWhenDateTimeSourceHasWrongFormat() {
        // GIVEN
        String invalid = "2021/07/21 13:10:00";
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        Assertions.assertThrows(DomibusDateTimeExtException.class,
                () -> converter.convert(invalid),
                "Invalid date time value [" + invalid + "]");

    }

    @Test
    public void convert_throwsParseExceptionWhenDateSourceHasWrongFormat() {
        // GIVEN
        String invalid = "2021/07/21";
        givenFormatter(DateTimeFormatter.ISO_DATE);

        // WHEN
        Assertions.assertThrows(DomibusDateTimeExtException.class,
                () -> converter.convert(invalid),
                "Invalid date time value [" + invalid + "]");
    }

    @Test
    public void convert_throwsParseExceptionWhenTimeSourceHasWrongFormat() {
        // GIVEN
        String invalid = "13H10'00";
        givenFormatter(DateTimeFormatter.ISO_TIME);

        // WHEN
        Assertions.assertThrows(DomibusDateTimeExtException.class,
                () -> converter.convert(invalid),
                "Invalid date time value [" + invalid + "]");
    }

    @Test
    public void convert_dateTimeSource() {
        // GIVEN
        String valid = "2021-07-21T10:15:30";
        LocalDateTime expected = LocalDateTime.of(2021, 07, 21, 10, 15, 30);
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        Assertions.assertEquals( expected, result, "Should have returned a valid UTC local date time for a valid date time source");
    }

    @Test
    public void convert_dateTimeSourceWithOffset() {
        // GIVEN
        String valid = "2021-07-21T10:15:30+04:00";
        LocalDateTime expected = LocalDateTime.of(2021, 07, 21, 6, 15, 30);
        givenFormatter(DateTimeFormatter.ISO_DATE_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        Assertions.assertEquals( expected, result, "Should have returned a valid UTC local date time for a valid offset date time source");
    }

    @Test
    public void convert_dateSource() {
        // GIVEN
        String valid = "2021-07-21";
        LocalDate expected = LocalDate.of(2021, 07, 21);
        givenFormatter(DateTimeFormatter.ISO_DATE);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        Assertions.assertEquals( expected, result, "Should have returned a valid UTC local date for a valid date source");
    }

    @Test
    public void convert_dateSourceWithOffset_IgnoresOffset() {
        // GIVEN
        String valid = "2021-07-21+04:00";
        LocalDate expected = LocalDate.of(2021, 07, 21);
        givenFormatter(DateTimeFormatter.ISO_DATE);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        Assertions.assertEquals( expected, result, "Should have ignored the offset and returned a valid UTC local date for a valid offset date source");
    }

    @Test
    public void convert_timeSource() {
        // GIVEN
        String valid = "10:15:30";
        LocalTime expected = LocalTime.of(10, 15, 30);
        givenFormatter(DateTimeFormatter.ISO_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        Assertions.assertEquals( expected, result, "Should have returned a valid UTC local time for a valid time source");
    }

    @Test
    public void convert_timeSourceWithOffset() {
        // GIVEN
        String valid = "10:15:30+04:30";
        LocalTime expected = LocalTime.of(5, 45, 30);
        givenFormatter(DateTimeFormatter.ISO_TIME);

        // WHEN
        TemporalAccessor result = converter.convert(valid);

        // THEN
        Assertions.assertEquals( expected, result, "Should have returned a valid UTC local time for a valid offset time source");
    }

    public void givenFormatter(DateTimeFormatter formatter) {
        this.formatter = formatter;
        converter = new StringToTemporalAccessorConverter(formatter);
    }

}
