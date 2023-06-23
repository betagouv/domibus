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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Sebastian-Ion TINCU
 */
@ExtendWith(JMockitExtension.class)
public class TimeAdapterTest {

    @Injectable
    private StringToTemporalAccessorConverter converter;

    @Tested(availableDuringSetup = true)
    private TimeAdapter timeAdapter;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(timeAdapter, "converter", converter, true);
    }

    @Test
    public void testUnmarshall_returnsNullTimeForNullInputString() throws Exception {
        // GIVEN
        String input = null;
        new Expectations() {{
            converter.convert(input);
            result = null;
        }};

        // WHEN
        LocalTime result = timeAdapter.unmarshal(input);

        // THEN
        Assertions.assertNull(result, "Should have returned null when unmarshalling a null input string");
    }

    @Test
    public void testUnmarshall_returnsParsedTimeForNonNullInputString(@Injectable LocalTime parsedTime) throws Exception {
        // GIVEN
        String input = "09:34:36";
        new Expectations() {{
            converter.convert(input);
            result = parsedTime;
        }};

        // WHEN
        LocalTime result = timeAdapter.unmarshal(input);

        // THEN
        Assertions.assertSame(parsedTime, result, "Should have returned the parsed time when unmarshalling a non-null input string");
    }

    @Test
    public void testMarshal_returnsNullFormattedTimeForNullInputTime() throws Exception {
        // GIVEN
        LocalTime input = null;

        // WHEN
        String result = timeAdapter.marshal(input);

        // THEN
        Assertions.assertNull(result, "Should have returned null when marshalling a null input time");
    }


    @Test
    public void testMarshall_returnsFormattedTimeForNonNullInputTime(@Injectable LocalTime input) throws Exception {
        // GIVEN
        String formattedTime = "09:34:36";
        new Expectations() {{
            input.format(DateTimeFormatter.ISO_TIME);
            result = formattedTime;
        }};

        // WHEN
        String result = timeAdapter.marshal(input);

        // THEN
        Assertions.assertEquals(formattedTime, result, "Should have returned the formatted time when marshalling a non-null input time");
    }
}
