package eu.domibus.common;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ionut Breaz
 * @since 5.1
 */


@Component
public class CsvUtil {
    public List<List<String>> getCsvRecords(String csv) throws IOException, CsvValidationException {
        List<List<String>> records = new ArrayList<>();
        try (StringReader stringReader = new StringReader(csv);
             CSVReader csvReader = new CSVReader(stringReader)) {
            String[] values = csvReader.readNext();
            while (values != null) {
                records.add(Arrays.asList(values));
                values = csvReader.readNext();
            }
        }
        return records;
    }
}
