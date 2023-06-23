package eu.domibus.core.cxf;

import eu.domibus.core.ssl.offload.SslOffloadService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class DomibusURLConnectionHTTPConduitTest {

    @Injectable
    private Message message;

    @Injectable
    private HTTPClientPolicy csPolicy;

    @Injectable
    private DomibusHttpsURLConnectionFactory domibusHttpsURLConnectionFactory;

    @Injectable
    private SslOffloadService sslOffloadService;

    @Injectable
    private DomibusBus bus;

    @Injectable
    private EndpointInfo endpointInfo;

    @Injectable
    private EndpointReferenceType target;

    @Tested
    private DomibusURLConnectionHTTPConduit domibusURLConnectionHTTPConduit;

    @BeforeEach
    public void stubSuperCallToSetupConnection() {
        new MockUp<URLConnectionHTTPConduit>() {
            @Mock void setupConnection(Message message, Address connectionAddress, HTTPClientPolicy csPolicy) {
                // do nothing
            }
        };
    }

    @Test
    public void setupConnection_WithoutSslOffload() throws Exception{
        final Address address = new Address("http://host:8443");
        new Expectations() {{
           sslOffloadService.isSslOffloadEnabled(address.getURL());
           result = false;
        }};

        domibusURLConnectionHTTPConduit.setupConnection(message, address, csPolicy);

        new FullVerifications() {{
            sslOffloadService.offload(address); times = 0;
        }};
    }

    @Test
    public void setupConnection_WithSslOffload() throws Exception{
        final Address address = new Address("https://host:8443");
        new Expectations() {{
            sslOffloadService.isSslOffloadEnabled(address.getURL());
            result = true;
        }};

        domibusURLConnectionHTTPConduit.setupConnection(message, address, csPolicy);
        new FullVerifications() {{
            sslOffloadService.offload(address); times = 1;
        }};
    }
}
