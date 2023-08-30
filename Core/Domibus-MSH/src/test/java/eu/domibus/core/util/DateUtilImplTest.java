package eu.domibus.core.util;

import eu.domibus.api.exceptions.DomibusDateTimeException;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Sebastian-Ion TINCU
 */
@ExtendWith(JMockitExtension.class)
public class DateUtilImplTest {

    @Tested
    private DateUtilImpl dateUtilImpl;

    @AfterEach
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

        Assertions.assertEquals(expected.toInstant(ZoneOffset.UTC).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                actual.toLocalDateTime(), "Should have converted correctly the ISO 8601 value to a timestamp");
    }

    @Test
    public void convertsIso8601ValuesToDates_WinterTime() {
        // Given
        String value = "2020-02-29T11:53:37";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        LocalDateTime expected = LocalDateTime.of(2020, Month.FEBRUARY, 29, 11, 53, 37);

        Assertions.assertEquals(expected.toInstant(ZoneOffset.UTC).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                actual.toLocalDateTime(), "Should have converted correctly the ISO 8601 value to a timestamp");
    }

    @Test
    public void convertsIso8601ValuesToDates_EpochZulu() {
        // Given
        String value = "1970-01-01T00:00:00";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        Instant expected = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0, 0).atOffset(ZoneOffset.UTC).toInstant();
        Assertions.assertEquals(expected, actual.toInstant(), "Should have converted correctly the epoch ISO 8601 value to a timestamp");
    }

    @Test
    public void convertsIso8601ValuesToDates_ZoneOffset() {
        // Given
        String value = "2020-02-29T11:53:37+02:00";

        // When
        Timestamp actual = dateUtilImpl.fromISO8601(value);

        // Then
        OffsetDateTime expected = OffsetDateTime.of(2020, 2, 29, 11, 53, 37, 0, ZoneOffset.of("+02:00"));
        Assertions.assertEquals(expected.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
                actual.toLocalDateTime(), "Should have converted correctly the offset ISO 8601 value to a timestamp");
    }

    @Test
    public void convertsNumberValuesToDates() {
        // Given
        Number value = new Long(912740921);

        // When
        Timestamp actual = dateUtilImpl.fromNumber(value);

        // Then
        Assertions.assertEquals(new Timestamp(912740921), actual, "Should have converted correctly the number value to a timestamp");
    }

    @Test
    public void convertsNumberValuesPassedInAsStringToDates() {
        // Given
        String value = "13231";

        // When
        Date actual = dateUtilImpl.fromString(value);

        // Then
        Assertions.assertEquals(new Timestamp(13231), actual, "Should have converted correctly the string number value to a timestamp");
    }

    @Test
    public void convertsIso8601ValuesPassedInAsStringToDates() {
        // Given
        String value = "1989-12-24T12:59:59";

        // When
        Date actual = dateUtilImpl.fromString(value);

        // Then
        long expected = LocalDateTime.of(1989, Month.DECEMBER, 24, 12, 59, 59).atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
        Assertions.assertEquals(expected, actual.getTime(), "Should have converted correctly the string ISO 8601 value to a timestamp");
    }

    @Test
    public void returnsNullWhenConvertingNullValuesPassedInAsStringToDates() {
        // When
        Date actual = dateUtilImpl.fromString(null);

        // Then
        Assertions.assertNull(actual, "Should have returned null when converting null values to a timestamp");
    }

    @Test
    public void returnsCorrectlyTheStartOfDayAsADate() {
        // When
        Date actual = dateUtilImpl.getStartOfDay();

        // Then
        Assertions.assertEquals(LocalDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant(),
                actual.toInstant(), "Should have returned the correct start of day as a date");
    }

    @Test
    public void getIdPkDateHour() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Brussels"));
        /*long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01T10H");

        Assertions.assertEquals(220101090000000000L, idPkDateHour);*/
    }

    @Test
    public void getIdPkDateHour_utc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
       /* long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01T10H");

        Assertions.assertEquals(220101100000000000L, idPkDateHour);*/
    }

    @Test
    public void getIdPkDateHour_nok() {
       /* try {
            dateUtilImpl.getIdPkDateHour("2022-01-01T");
            Assertions.fail();
        } catch (DomibusDateTimeException e) {
            //OK
        }*/
    }

    @Test
    public void getIdPkDateHour_onlyDate_Utc() {
        /*TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01");

        Assertions.assertEquals(220101000000000000L, idPkDateHour);*/
    }


    @Test
    public void getIdPkDateHour_onlyDate() {
/*        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Brussels"));
        long idPkDateHour = dateUtilImpl.getIdPkDateHour("2022-01-01");

        Assertions.assertEquals(211231230000000000L, idPkDateHour);*/
    }

    @Test
    public void getIdPkDateHour_notACorrectDate() {
     /*   try {
            dateUtilImpl.getIdPkDateHour("2022-99-99T10H");
            Assertions.fail();
        } catch (DomibusDateTimeException e) {
            //OK
        }*/
    }

    @Test
    public void getIdPkDateHour_empty() {
       /* try {
            dateUtilImpl.getIdPkDateHour("");
            Assertions.fail();
        } catch (DomibusDateTimeException e) {
            //OK
        }*/
    }
}
