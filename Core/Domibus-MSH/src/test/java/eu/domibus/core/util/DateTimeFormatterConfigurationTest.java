package eu.domibus.core.util;

import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_DATE_TIME_PATTERN_ON_RECEIVING;

/**
 * @author François Gautier
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DateTimeFormatterConfigurationTest {

    public static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS][.SSSSSS][.SSS][z]";
    @Tested
    private DateTimeFormatterConfiguration dateTimeFormatterConfiguration;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @BeforeEach
    public void setUp() {
        new Expectations(){{
            domibusPropertyProvider.getProperty(DOMIBUS_DATE_TIME_PATTERN_ON_RECEIVING);
            result = DEFAULT_DATE_TIME_PATTERN;
            times = 1;
        }};
    }

    @AfterEach
public void tearDown() {
        new FullVerifications(){};
    }

    private void parse(String s) {
        LocalDateTime.parse(s, dateTimeFormatterConfiguration.dateTimeFormatter());
    }

    @Test
    public void format() {
        parse("2020-06-02T20:00:00");
    }

    @Test
    public void format_frac() {
        parse("2020-06-02T20:00:00.000");
    }

    @Test
    public void format_frac6() {
        parse("2020-06-02T20:00:00.000000");
    }

    @Test
    public void format_frac9() {
        parse("2020-06-02T20:00:00.000000000");
    }

    @Test
    public void format_frac7() {
        Assertions.assertThrows(DateTimeParseException. class,() -> parse("2020-06-02T20:00:00.0000000"));
    }

    @Test
    public void format_UTC() {
        parse("2020-06-02T20:00:00Z");
    }

    @Test
    public void format_FractionalSeconds1_UTC() {
        Assertions.assertThrows(DateTimeParseException. class,() -> parse("2020-06-02T09:00:00.0Z"));
    }

    /**
     * Strangely enough, this is an accepted
     */
    @Test
    public void format_FractionalSeconds2_UTC() {
        parse("2020-06-02T09:00:00.12Z");
    }

    @Test
    public void format_FractionalSeconds2() {
        Assertions.assertThrows(DateTimeParseException. class,() -> parse("2020-06-02T09:00:00.12"));
    }

    @Test
    public void format_FractionalSeconds3_UTC() {
        parse("2020-06-02T09:00:00.000Z");
    }

    @Test
    public void format_FractionalSeconds_timeZone() {
        parse("2020-06-02T23:00:00.000+03:00");
    }

    @Test
    public void format_FractionalSeconds6_timeZone() {
        parse("2020-06-02T23:00:00.000000+03:00");
    }

    @Test
    public void format_FractionalSeconds9_timeZone() {
        parse("2020-06-02T23:00:00.000000000+03:00");
    }

    @Test
    public void format_FractionalSeconds12_timeZone() {
        Assertions.assertThrows(DateTimeParseException. class,() -> parse("2020-06-02T23:00:00.000000000000+03:00"));
    }

    @Test
    public void format_timeZone() {
        parse("2020-06-02T23:00:00+03:00");
    }

    @Test
    public void format_z() {
        Assertions.assertThrows(DateTimeParseException. class,() -> parse("2000-03-04T20:00:00z"));
    }
}
