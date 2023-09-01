package eu.domibus.core.util;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.exceptions.DomibusDateTimeException;
import eu.domibus.api.util.DateUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author Cosmin Baciu
 * @author Sebastian-Ion TINCU
 * @since 3.3
 */
@Component(DateUtil.BEAN_NAME)
public class DateUtilImpl implements DateUtil {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DateUtilImpl.class);

    @Override
    public Date fromString(String value) {
        Date result = null;

        if (StringUtils.isNotEmpty(value)) {
            if (StringUtils.isNumeric(value)) {
                result = fromNumber(Long.parseLong(value));
            } else {
                result = fromISO8601(value);
            }
        }

        return result;
    }

    public Timestamp fromNumber(Number value) {
        return new Timestamp(value.longValue());
    }

    public Timestamp fromISO8601(String value) {
        Date date = null;
        try {
            LOG.debug("Parsing an offset date time value: [{}]", value);
            OffsetDateTime dateTime = OffsetDateTime.parse(value);
            date = Date.from(dateTime.toInstant());
        } catch (DateTimeParseException ex) {
            LOG.debug("Error during Parsing offset date time value: [{}]", value);

            try {
                LOG.debug("Parsing local date time value: [{}]", value);
                LocalDateTime dateTime = LocalDateTime.parse(value);
                date = Date.from(dateTime.toInstant(ZoneOffset.UTC));
            } catch (DateTimeParseException exception) {
                LOG.debug("Exception occurred during parsing of date time", exception);
                throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Cannot parse datetime value", exception);
            }
        }

        return new Timestamp(date.getTime());
    }

    @Override
    public Date getStartOfDay() {
        return Date.from(ZonedDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant());
    }

    @Override
    public String getCurrentTime(DateTimeFormatter dateTimeFormatter) {
        return java.time.LocalDateTime.now(ZoneOffset.UTC).format(dateTimeFormatter);
    }

    @Override
    public String getCurrentTime() {
        return getCurrentTime(DEFAULT_FORMATTER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getUtcDate() {
        return new Date();
    }

    @Override
    public LocalDateTime getUtcLocalDateTime(LocalDateTime localDateTime){
      return localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    @Override
    public long getDiffMinutesBetweenDates(Date date1, Date date2) {
        long diffInMillies = Math.abs(date2.getTime() - date1.getTime());
        return TimeUnit.MINUTES.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    @Override
    public LocalDateTime getLocalDateTime(String date) {
        if (StringUtils.isBlank(date)) {
            throw new DomibusDateTimeException(date, REST_FORMATTER_PATTERNS_MESSAGE);
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(date, REST_FORMATTER);
            return getUtcLocalDateTime(localDateTime);
        } catch (Exception e) {
            throw new DomibusDateTimeException(date, REST_FORMATTER_PATTERNS_MESSAGE, e);
        }
    }

    @Override
    public LocalDateTime getLocalDateTimeFromDateWithHour(Long dateWithHour) {
        if (dateWithHour == null) {
            throw new DomibusDateTimeException(DATETIME_FORMAT_DEFAULT);
        }
        try {
            //we add 20 as prefix to have 4 digits for the year eg 22 becomes 2022
            final String dateWithHourAndFullYear = "20" + dateWithHour;
            LocalDateTime localDateTime = LocalDateTime.parse(dateWithHourAndFullYear, REST_FORMATTER_FOR_DATE_WITH_HOUR_NO_SEPARATORS);
            return localDateTime.atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            throw new DomibusDateTimeException(dateWithHour + "", DATETIME_FORMAT_DEFAULT, e);
        }
    }

    @Override
    public LocalDateTime convertToLocalDateTime(Date date) {
        if(date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    @Override
    public Date convertFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public String getIdPkDateHourPrefix(Date value) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT_DEFAULT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(value).substring(0, 8);
    }
}
