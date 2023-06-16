package eu.domibus.jms.spi.helper;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static eu.domibus.jms.spi.InternalJMSConstants.*;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@ExtendWith(JMockitExtension.class)
public class JMSSelectorUtilImplTest {

    @Tested
    JMSSelectorUtilImpl selectorUtil;

    @Test
    public void testGetSelector()  {
        String selector = selectorUtil.getSelector("myMessageId");
        Assertions.assertEquals("JMSMessageID = 'myMessageId'", selector);
    }

    @Test
    public void testGetSelectorWithMultipleIds()  {
        String selector = selectorUtil.getSelector(new String[]{"message1", "message2"});
        Assertions.assertEquals("JMSMessageID IN ('message1', 'message2')", selector);
    }

    @Test
    public void testGetSelectorWithClause() throws Exception {
        Map<String, Object> criteria = new HashMap<String, Object>();
        criteria.put(CRITERIA_JMS_TYPE, "myType");
        criteria.put(CRITERIA_JMS_TIMESTAMP_FROM, 123L);
        criteria.put(CRITERIA_JMS_TIMESTAMP_TO, 456L);
        criteria.put(CRITERIA_SELECTOR_CLAUSE, "JMSMessageID = 'myMessageId'");
        String selector = selectorUtil.getSelector(criteria);
        Assertions.assertEquals("JMSType='myType' and JMSTimestamp>=123 and JMSTimestamp<=456 and JMSMessageID = 'myMessageId'", selector);

        criteria = new HashMap<String, Object>();
        criteria.put(CRITERIA_JMS_TYPE, "my'Type'e'");
        criteria.put(CRITERIA_JMS_TIMESTAMP_FROM, 123L);
        criteria.put(CRITERIA_JMS_TIMESTAMP_TO, 456L);
        criteria.put(CRITERIA_SELECTOR_CLAUSE, "JMSMessageID = 'myMessageId'");
        selector = selectorUtil.getSelector(criteria);
        Assertions.assertEquals("JMSType='my''Type''e''' and JMSTimestamp>=123 and JMSTimestamp<=456 and JMSMessageID = 'myMessageId'", selector);
        //test even number of apostrophes in the string
        Assertions.assertEquals(0, selector.replaceAll("[^']", "").length() % 2);

    }


}
