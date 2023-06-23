package eu.domibus.core.property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Created by baciuco on 08/08/2016.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:spring/propertyResolverContext.xml")
public class PropertyPlaceHolderTestIT {

    @Value("${mycustomKey}")
    String value;

    @Value("${mycustomKey1}")
    String myProperty1;

    @Value("${mykey:${mycustomKey}}/work")
    String value1;

    @Test
    public void testResolveProperty() throws Exception {
        Assertions.assertNotNull(value);
        Assertions.assertEquals(value, "mycustomvalue");

        System.out.println("value1=" + value1);
        Assertions.assertNotNull(value1);


        System.out.println("mycustomKey1=" + myProperty1);
        Assertions.assertNotNull(myProperty1);
    }

}
