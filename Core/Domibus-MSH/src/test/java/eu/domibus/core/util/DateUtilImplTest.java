package eu.domibus.core.util;

import eu.domibus.api.exceptions.DomibusDateTimeException;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Sebastian-Ion TINCU
 */
@RunWith(JMockit.class)
public class DateUtilImplTest {

    @Tested
    private DateUtilImpl dateUtilImpl;

    @After
    public void tearDown() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Brussels"));
    }


    @Test
    public void convertsIso8601ValuesToDates_SummerTime() {
        // Given
        String value = "2020-08-29T11:53:37";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        LocalDateTime expected = LocalDateTime.of(2020, Month.AUGUST, 29, 11, 53, 37);

        Assert.assertEquals("Should have converted correctly the ISO 8601 value to a timestamp",
                expected.toInstant(ZoneOffset.UTC).atZone(ZoneId.systemDefault()).toLocalDateTime(), actual.toLocalDateTime());
    }

    @Test
    public void convertsIso8601ValuesToDates_WinterTime() {
        // Given
        String value = "2020-02-29T11:53:37";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        LocalDateTime expected = LocalDateTime.of(2020, Month.FEBRUARY, 29, 11, 53, 37);

        Assert.assertEquals("Should have converted correctly the ISO 8601 value to a timestamp",
                expected.toInstant(ZoneOffset.UTC).atZone(ZoneId.systemDefault()).toLocalDateTime(), actual.toLocalDateTime());
    }

    @Test
    public void convertsIso8601ValuesToDates_EpochZulu() {
        // Given
        String value = "1970-01-01T00:00:00";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        Instant expected = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0, 0).atOffset(ZoneOffset.UTC).toInstant();
        Assert.assertEquals("Should have converted correctly the epoch ISO 8601 value to a timestamp", expected, actual.toInstant());
    }

    @Test
    public void convertsIso8601ValuesToDates_ZoneOffset() {
        // Given
        String value = "2020-02-29T11:53:37+02:00";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        OffsetDateTime expected = OffsetDateTime.of(2020, 2, 29, 11, 53, 37, 0, ZoneOffset.of("+02:00"));
        Assert.assertEquals("Should have converted correctly the offset ISO 8601 value to a timestamp",
                expected.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), actual.toLocalDateTime());
    }

    @Test
    public void convertsNumberValuesToDates() {
        // Given
        Number value = new Long(912740921);

        // When
        Timestamp actual = dateUtilImpl.fromNumber(value);

        // Then
        Assert.assertEquals("Should have converted correctly the number value to a timestamp", new Timestamp(912740921), actual);
    }

    @Test
    public void convertsNumberValuesPassedInAsStringToDates() {
        // Given
        String value = "13231";

        // When
        Date actual = dateUtilImpl.fromString(value);

        // Then
        Assert.assertEquals("Should have converted correctly the string number value to a timestamp", new Timestamp(13231), actual);
    }

    @Test
    public void convertsIso8601ValuesPassedInAsStringToDates() {
        // Given
        String value = "1989-12-24T12:59:59";

        // When
        Date actual = dateUtilImpl.fromString(value);

        // Then
        long expected = LocalDateTime.of(1989, Month.DECEMBER, 24, 12, 59, 59).atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
        Assert.assertEquals("Should have converted correctly the string ISO 8601 value to a timestamp", expected, actual.getTime());
    }

    @Test
    public void returnsNullWhenConvertingNullValuesPassedInAsStringToDates() {
        // When
        Date actual = dateUtilImpl.fromString(null);

        // Then
        Assert.assertNull("Should have returned null when converting null values to a timestamp", actual);
    }

    @Test
    public void returnsCorrectlyTheStartOfDayAsADate() {
        // When
        Date actual = dateUtilImpl.getStartOfDay();

        // Then
        Assert.assertEquals("Should have returned the correct start of day as a date",
                LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant(), actual.toInstant());
    }

    @Test
    public void getIdPkDateHour() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Brussels"));
        long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01T10H");

        Assert.assertEquals(220101090000000000L, idPkDateHour);
    }

    @Test
    public void getIdPkDateHour_utc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01T10H");

        Assert.assertEquals(220101100000000000L, idPkDateHour);
    }

    @Test
    public void getIdPkDateHour_nok() {
        try {
            dateUtilImpl.getIdPkDateHour("2022-01-01T");
            Assert.fail();
        } catch (DomibusDateTimeException e) {
            //OK
        }
    }

    @Test
    public void getIdPkDateHour_onlyDate_Utc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01");

        Assert.assertEquals(220101000000000000L, idPkDateHour);
    }


    @Test
    public void getIdPkDateHour_onlyDate() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Brussels"));
        long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01");

        Assert.assertEquals(211231230000000000L, idPkDateHour);
    }

    @Test
    public void getIdPkDateHour_notACorrectDate() {
        try {
            dateUtilImpl.getIdPkDateHour("2022-99-99T10H");
            Assert.fail();
        } catch (DomibusDateTimeException e) {
            //OK
        }
    }

    @Test
    public void getIdPkDateHour_empty() {
        try {
            dateUtilImpl.getIdPkDateHour("");
            Assert.fail();
        } catch (DomibusDateTimeException e) {
            //OK
        }
    }


    @Test
    public void getIdPkDateHourPrefixTest() {
        String DATETIME_FORMAT_DEFAULT = "yyMMddHH";
        final SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT_DEFAULT);
        sdf.setTimeZone(TimeZone.getTimeZone("EST"));

        Date currentDate = dateUtilImpl.getUtcDate();
        Date newDate = DateUtils.addMinutes(currentDate, 10);
        Integer partitionNameEES = new Integer(sdf.format(newDate).substring(0, 8));
        
        Integer partitionNameUTC = new Integer(dateUtilImpl.getIdPkDateHourPrefix(currentDate));

        Assert.assertTrue(partitionNameUTC - partitionNameEES > 0);
    }
    
}