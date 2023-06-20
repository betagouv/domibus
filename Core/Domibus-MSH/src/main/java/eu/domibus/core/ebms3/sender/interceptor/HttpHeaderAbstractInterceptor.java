package eu.domibus.core.ebms3.sender.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusGeneralConstants.JSON_MAPPER_BEAN;

/**
 * Abstract Interceptor for Apache CXF Http headers
 *
 * @author Catalin Enache
 * @since 4.2
 */
public abstract class HttpHeaderAbstractInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(HttpHeaderAbstractInterceptor.class);

    static final String USER_AGENT_HTTP_HEADER_KEY = "user-agent";
    static final String USER_AGENT_HTTP_HEADER_VALUE_APACHE_CXF = "Apache-CXF";

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    @Qualifier(JSON_MAPPER_BEAN)
    protected ObjectMapper objectMapper;

    public HttpHeaderAbstractInterceptor(String phase) {
        super(phase);
    }

    /**
     * It removes the user-agent header if contains Apache-CXF information
     *
     * @param message
     * @throws Fault
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        //get the headers
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);

        final Boolean httpHeaderMetadataActive = domibusPropertyProvider.getBooleanProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_DISPATCHER_HTTP_HEADER_METADATA_ACTIVE);
        if (httpHeaderMetadataActive) {
            LOG.putMDC(MessageConstants.HTTP_CONTENT_TYPE, (String) message.get("Content-Type"));
            final String httpHeadersAsJson = httpHeadersToJson(headers);
            LOG.putMDC(MessageConstants.HTTP_PROTOCOL_HEADERS, httpHeadersAsJson);
        }

        if (headers == null) {
            getLogger().debug("no http headers to intercept");
            return;
        }

        boolean removed = headers.entrySet()
                .removeIf(e -> USER_AGENT_HTTP_HEADER_KEY.equalsIgnoreCase(e.getKey())
                        && StringUtils.containsIgnoreCase(Arrays.deepToString(e.getValue().toArray()), USER_AGENT_HTTP_HEADER_VALUE_APACHE_CXF)
                );

        getLogger().debug("httpHeader=[{}] {}", USER_AGENT_HTTP_HEADER_KEY, (removed ? " was successfully removed" : " not present or value not removed"));

        //logging of the remaining headers
        getLogger().debug("httpHeaders are: {}", httpHeadersToString(headers));
    }

    protected abstract DomibusLogger getLogger();

    private String httpHeadersToString(Map<String, List<String>> headers) {
        return headers.keySet().stream()
                .map(key -> key + "=" + Arrays.deepToString(headers.get(key).toArray()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    protected String httpHeadersToJson(Map<String, List<String>> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            //we catch the error; should not impact the message exchange
            LOG.error("Could not write HTTP headers as JSON", e);
            return null;
        }
    }
}
