package eu.domibus.core.spi.soapenvelope;

import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 5.2
 */
public class HttpMetadata {

    protected String contentType;
    protected Map<String, List<String>> headers;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
}
