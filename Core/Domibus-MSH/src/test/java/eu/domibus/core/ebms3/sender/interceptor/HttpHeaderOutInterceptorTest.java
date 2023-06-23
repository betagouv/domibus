package eu.domibus.core.ebms3.sender.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.property.DomibusPropertyProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Catalin Enache
 * @since 4.2
 */
public class HttpHeaderOutInterceptorTest {

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;
    @Injectable
    private ObjectMapper domibusJsonMapper;

    @Tested
    HttpHeaderInInterceptor httpHeaderInInterceptor;

    @Test
    @Disabled("EDELIVERY-6896")
    public void test_handleMessage_UserAgentPresentApache(final @Mocked Message message) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("user-agent", Collections.singletonList("Apache-CXF/3.2"));
        headers.put("cache-control", Collections.singletonList("no-cache"));
        headers.put("connection", Collections.singletonList("keep-alive"));
        headers.put("content-type", Collections.singletonList("multipart/related; type=\"application/soap+xml\"; boundary=\"uuid:4f015876-24a6-48fe-88c7-23dc84886eca\"; start=\"<root.message@cxf.apache.org>\"; start-info=\"application/soap+xml\""));

        new Expectations() {{
            message.get(Message.PROTOCOL_HEADERS);
            result = headers;
        }};

        //tested method
        httpHeaderInInterceptor.handleMessage(message);

        Assertions.assertNull(headers.get("user-agent"));
        Assertions.assertNotNull(headers.get("cache-control"));
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void test_handleMessage_UserAgentNotPresent(final @Mocked Message message) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("accept", Collections.singletonList("*/*"));
        headers.put("connection", Collections.singletonList("keep-alive"));

        new Expectations() {{
            message.get(Message.PROTOCOL_HEADERS);
            result = headers;
        }};

        //tested method
        httpHeaderInInterceptor.handleMessage(message);

        Assertions.assertTrue(headers != null && headers.size() == 2);
        Assertions.assertNotNull(headers.get("connection"));

    }
}
