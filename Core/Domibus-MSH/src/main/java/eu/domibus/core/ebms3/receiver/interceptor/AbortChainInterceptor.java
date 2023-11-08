package eu.domibus.core.ebms3.receiver.interceptor;

import eu.domibus.core.ebms3.receiver.policy.SetPolicyInServerInterceptor;
import eu.domibus.core.property.DomibusVersionService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

/**
 * Interceptor that aborts the interceptor chain when a request containing a http get method is detected
 * @since 5.2
 */
@Service
public class AbortChainInterceptor extends AbstractSoapInterceptor {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbortChainInterceptor.class);
    public static final String ORG_APACHE_CXF_REQUEST_METHOD = "org.apache.cxf.request.method";
    protected final DomibusVersionService domibusVersionService;

    public AbortChainInterceptor(DomibusVersionService domibusVersionService) {
        super(Phase.RECEIVE);
        this.addBefore(SetPolicyInServerInterceptor.class.getName());
        this.domibusVersionService = domibusVersionService;
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        final String httpMethod = (String) message.get(ORG_APACHE_CXF_REQUEST_METHOD);

        if (StringUtils.containsIgnoreCase(httpMethod, HttpMethod.GET)) {
            LOG.debug("Detected GET request on MSH: aborting the interceptor chain");
            message.getInterceptorChain().abort();

            final HttpServletResponse response = (HttpServletResponse) message.get(AbstractHTTPDestination.HTTP_RESPONSE);
            response.setStatus(HttpServletResponse.SC_OK);
            try {
                response.getWriter().println(domibusVersionService.getBuildDetails());
            } catch (IOException ex) {
                throw new Fault(ex);
            }
        }
    }


}
