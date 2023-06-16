package eu.domibus.plugin.ws.jaxb;

import eu.domibus.plugin.convert.StringToTemporalAccessorConverter;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Sebastian-Ion TINCU
 */
@ExtendWith(JMockitExtension.class)
public class DateTimeAdapterTest {

    @Injectable
    private StringToTemporalAccessorConverter converter;

    @Tested(availableDuringSetup = true)
    private DateTimeAdapter dateTimeAdapter;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(dateTimeAdapter, "converter", converter, true);
    }

    @Test
    public void testUnmarshall_returnsNullDateTimeForNullInputString() throws Exception {
        // GIVEN
        String input = null;
        new Expectations() {{
            converter.convert(input); result = null;
        }};

        // WHEN
        LocalDateTime result = dateTimeAdapter.unmarshal(input);

        // THEN
        Assertions.assertNull(result, "Should have returned null when unmarshalling a null input string");
    }

    @Test
    public void testUnmarshall_returnsParsedDateTimeForNonNullInputString(@Injectable LocalDateTime parsedDateTime) throws Exception {
        // GIVEN
        String input = "2019-04-17T09:34:36";
        new Expectations() {{
            converter.convert(input); result = parsedDateTime;
        }};

        // WHEN
        LocalDateTime result = dateTimeAdapter.unmarshal(input);

        // THEN
        Assertions.assertSame(parsedDateTime, result, "Should have returned the parsed date time when unmarshalling a non-null input string");
    }

    @Test
    public void testMarshal_returnsNullFormattedDateTimeForNullInputDateTime() throws Exception {
        // GIVEN
        LocalDateTime input = null;

        // WHEN
        String result = dateTimeAdapter.marshal(input);

        // THEN
        Assertions.assertNull(result, "Should have returned null when marshalling a null input date time");
    }


    @Test
    public void testMarshall_returnsFormattedDateTimeForNonNullInputDateTime(@Injectable LocalDateTime inputDate) throws Exception {
        // GIVEN
        String formattedDateTime = "2019-04-17T09:34:36";
        new Expectations() {{
            inputDate.format(DateTimeFormatter.ISO_DATE_TIME); result = formattedDateTime;
        }};

        // WHEN
        String result = dateTimeAdapter.marshal(inputDate);

        // THEN
        Assertions.assertEquals("Should have returned the formatted date time when marshalling a non-null input date time", formattedDateTime, result);
    }
}
