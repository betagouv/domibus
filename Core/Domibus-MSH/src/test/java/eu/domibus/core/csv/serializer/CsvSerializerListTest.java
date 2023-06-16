package eu.domibus.core.csv.serializer;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static eu.domibus.core.csv.serializer.CsvSerializer.LIST_DELIMITER;

@ExtendWith(JMockitExtension.class)
public class CsvSerializerListTest {

    @Tested
    CsvSerializerList cvsSerializerList;

    @Test
    public void canHandle() {
        Assertions.assertFalse(cvsSerializerList.canHandle(new HashMap<>()));
        Assertions.assertTrue(cvsSerializerList.canHandle(new ArrayList<>()));
    }

    @Test
    public void serialize() {
        Assertions.assertEquals(cvsSerializerList.serialize(new ArrayList<>()), "");
        Assertions.assertEquals(cvsSerializerList.serialize(Arrays.asList("1", "2")), "1" + LIST_DELIMITER +  "2");
    }
}
