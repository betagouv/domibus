package eu.domibus.core.payload.temp;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@ExtendWith(JMockitExtension.class)
public class RegexIOFileFilterTest {

    @Injectable
    Pattern pattern;

    @Tested
    RegexIOFileFilter regexIOFileFilter;

    @Test
    @Disabled("EDELIVERY-6896")
    public void acceptFile(@Injectable File file) {
        String myFile = "myFile";
        new Expectations(regexIOFileFilter) {{
            file.getName();
            result = myFile;

            regexIOFileFilter.accept(anyString);
            result = true;
        }};

        final boolean accept = regexIOFileFilter.accept(file);

        new Verifications() {{
            regexIOFileFilter.accept(myFile);
            Assertions.assertTrue(accept);
        }};

    }

    @Test
    public void accept(@Injectable Matcher matcher) {
        String myFile = "myFile";

        new Expectations() {{
            pattern.matcher(myFile);
            result = matcher;

            matcher.matches();
            result = true;
        }};

        Assertions.assertTrue(regexIOFileFilter.accept(myFile));
    }
}
