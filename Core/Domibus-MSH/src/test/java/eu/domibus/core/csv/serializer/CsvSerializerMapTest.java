package eu.domibus.core.csv.serializer;

import eu.domibus.api.exceptions.DomibusCoreException;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(JMockitExtension.class)
public class CsvSerializerMapTest {

    @Tested
    CsvSerializerMap cvsSerializerMap;

    @Test
    public void canHandle() {
        Assertions.assertTrue(cvsSerializerMap.canHandle(new HashMap<>()));
        Assertions.assertFalse(cvsSerializerMap.canHandle(new ArrayList<>()));
    }

    @Test
    public void serialize_Empty() throws DomibusCoreException {
        Assertions.assertEquals(cvsSerializerMap.serialize(new HashMap<>()), "{}");
    }

    @Test
    public void serialize_EmptyAttribute() throws DomibusCoreException {
        Map<String, Object> props = new HashMap<>();
        props.put("attribute1", "value1");
        props.put("attribute2", null);

        String res = cvsSerializerMap.serialize(props);
        Assertions.assertTrue(res.contains("\"attribute2\":null"));
    }
}
