package eu.domibus.core.csv.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(JMockit.class)
public class CsvSerializerMapTest {

    @Tested
    CsvSerializerMap cvsSerializerMap;

    @Test
    public void canHandle() {
        Assert.assertTrue(cvsSerializerMap.canHandle(new HashMap<>()));
        Assert.assertFalse(cvsSerializerMap.canHandle(new ArrayList<>()));
    }

    @Test
    public void serialize_Empty() throws JsonProcessingException {
        Assert.assertEquals(cvsSerializerMap.serialize(new HashMap<>()), "{}");
    }

    @Test
    public void serialize_EmptyAttribute() throws JsonProcessingException {
        Map<String, Object> props = new HashMap<>();
        props.put("attribute1", "value1");
        props.put("attribute2", null);

        String res = cvsSerializerMap.serialize(props);
        Assert.assertTrue(res.contains("\"attribute2\":null"));
    }
}