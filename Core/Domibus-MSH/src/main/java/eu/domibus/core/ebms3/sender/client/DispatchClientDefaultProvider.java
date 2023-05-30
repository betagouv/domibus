package eu.domibus.core.ebms3.sender.client;

import eu.domibus.api.pmode.PModeConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.proxy.DomibusProxyService;
import eu.domibus.common.DomibusCacheConstants;
import eu.domibus.core.cxf.DomibusHTTPConduitFactory;
import eu.domibus.core.ehcache.IgnoreSizeOfWrapper;
import eu.domibus.core.proxy.ProxyCxfUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Policy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.soap.SOAPBinding;
import java.util.concurrent.Executor;

import static eu.domibus.api.cache.DomibusLocalCacheService.DISPATCH_CLIENT;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Service
@DependsOn(Bus.DEFAULT_BUS_ID)
public class DispatchClientDefaultProvider implements DispatchClientProvider {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DispatchClientDefaultProvider.class);

    public static final String MESSAGING_KEY_CONTEXT_PROPERTY = "MESSAGING_KEY_CONTEXT_PROPERTY";

    public static final String ASYMMETRIC_SIG_ALGO_PROPERTY = "ASYMMETRIC_SIG_ALGO_PROPERTY";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String MESSAGE_ROLE = "MESSAGE_ROLE";
    public static final String NAMESPACE_URI = "http://domibus.eu";
    public static final QName SERVICE_NAME = new QName(NAMESPACE_URI, "msh-dispatch-service");
    public static final QName LOCAL_SERVICE_NAME = new QName(NAMESPACE_URI, "local-msh-dispatch-service");
    public static final QName PORT_NAME = new QName(NAMESPACE_URI, "msh-dispatch");
    public static final QName LOCAL_PORT_NAME = new QName(NAMESPACE_URI, "local-msh-dispatch");
    public static final String DOMIBUS_DISPATCHER_CONNECTIONTIMEOUT = DOMIBUS_DISPATCHER_CONNECTION_TIMEOUT;
    public static final String DOMIBUS_DISPATCHER_RECEIVETIMEOUT = DOMIBUS_DISPATCHER_RECEIVE_TIMEOUT;
    public static final String DOMIBUS_DISPATCHER_ALLOWCHUNKING = DOMIBUS_DISPATCHER_ALLOW_CHUNKING;
    public static final String DOMIBUS_DISPATCHER_CHUNKINGTHRESHOLD = DOMIBUS_DISPATCHER_CHUNKING_THRESHOLD;

    private final TLSReaderServiceImpl tlsReader;

    private final Executor executor;

    protected final DomibusPropertyProvider domibusPropertyProvider;

    protected final DomibusProxyService domibusProxyService;

    protected final ProxyCxfUtil proxyUtil;

    protected final DomibusHTTPConduitFactory domibusHTTPConduitFactory;

    public DispatchClientDefaultProvider(TLSReaderServiceImpl tlsReader,
                                         @Qualifier("taskExecutor") Executor executor,
                                         DomibusPropertyProvider domibusPropertyProvider,
                                         @Qualifier("domibusProxyService") DomibusProxyService domibusProxyService,
                                         ProxyCxfUtil proxyUtil, DomibusHTTPConduitFactory domibusHTTPConduitFactory) {
        this.tlsReader = tlsReader;
        this.executor = executor;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.domibusProxyService = domibusProxyService;
        this.proxyUtil = proxyUtil;
        this.domibusHTTPConduitFactory = domibusHTTPConduitFactory;
    }

    /**
     * JIRA: EDELIVERY-6755 showed a deadlock while instantiating cxf dispatcher during start-up
     * (in concurrency with the beans creation).
     * To initialize it in the {@link PostConstruct} avoid this issue.
     */
    @PostConstruct
    void init() {
        LOG.debug("Pre-instantiate cxf dispatcher");
        createWSServiceDispatcher("http://localhost:8080");
    }

    @Cacheable(cacheManager = DomibusCacheConstants.CACHE_MANAGER, value = DISPATCH_CLIENT, key = "#domain + #endpoint + #pModeKey", condition = "#cacheable")
    @Override
    public IgnoreSizeOfWrapper<Dispatch<SOAPMessage>> getClient(String domain, String endpoint, String algorithm, Policy policy, final String pModeKey, boolean cacheable) {
        LOG.debug("Getting the dispatch client for endpoint [{}] on domain [{}]", endpoint, domain);

        final Dispatch<SOAPMessage> dispatch = createWSServiceDispatcher(endpoint);
        dispatch.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, policy);
        if (StringUtils.isNotBlank(algorithm)) {
            dispatch.getRequestContext().put(ASYMMETRIC_SIG_ALGO_PROPERTY, algorithm);
        }
        dispatch.getRequestContext().put(PModeConstants.PMODE_KEY_CONTEXT_PROPERTY, pModeKey);
        final Client client = ((DispatchImpl<SOAPMessage>) dispatch).getClient();
        client.getEndpoint().getEndpointInfo().setProperty(HTTPConduitFactory.class.getName(), domibusHTTPConduitFactory);
        final HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        final HTTPClientPolicy httpClientPolicy = httpConduit.getClient();

        httpConduit.setClient(httpClientPolicy);
        setHttpClientPolicy(httpClientPolicy);

        if (endpoint.startsWith("https://")) {
            final TLSClientParameters params = tlsReader.getTlsClientParameters(domain);
            if (params != null) {
                httpConduit.setTlsClientParameters(params);
            }
        }

        proxyUtil.configureProxy(httpClientPolicy, httpConduit);
        LOG.debug("END Getting the dispatch client for endpoint [{}] on domain [{}]", endpoint, domain);

        return new IgnoreSizeOfWrapper<>(dispatch);
    }


    @Override
    public Dispatch<SOAPMessage> getLocalClient(String domain, String endpoint) {
        LOG.debug("Creating the dispatch client for endpoint [{}] on domain [{}]", endpoint, domain);
        Dispatch<SOAPMessage> dispatch = createLocalWSServiceDispatcher(endpoint);

        final Client client = ((DispatchImpl<SOAPMessage>) dispatch).getClient();
        final LocalConduit httpConduit = (LocalConduit) client.getConduit();

        httpConduit.setMessageObserver(message -> {
            message.getExchange().getOutMessage().put(ClientImpl.SYNC_TIMEOUT, 0);
            message.getExchange().put(ClientImpl.FINISHED, Boolean.TRUE);
            LOG.debug("on message");
        });


        return dispatch;
    }

    public void setHttpClientPolicy(HTTPClientPolicy httpClientPolicy) {
        //ConnectionTimeOut - Specifies the amount of time, in milliseconds, that the consumer will attempt to establish a connection before it times out. 0 is infinite.
        int connectionTimeout = Integer.parseInt(domibusPropertyProvider.getProperty(DOMIBUS_DISPATCHER_CONNECTIONTIMEOUT));
        httpClientPolicy.setConnectionTimeout(connectionTimeout);
        //ReceiveTimeOut - Specifies the amount of time, in milliseconds, that the consumer will wait for a response before it times out. 0 is infinite.
        int receiveTimeout = Integer.parseInt(domibusPropertyProvider.getProperty(DOMIBUS_DISPATCHER_RECEIVETIMEOUT));
        httpClientPolicy.setReceiveTimeout(receiveTimeout);
        httpClientPolicy.setAllowChunking(Boolean.valueOf(domibusPropertyProvider.getProperty(DOMIBUS_DISPATCHER_ALLOWCHUNKING)));
        httpClientPolicy.setChunkingThreshold(Integer.parseInt(domibusPropertyProvider.getProperty(DOMIBUS_DISPATCHER_CHUNKINGTHRESHOLD)));

        Boolean keepAlive = Boolean.parseBoolean(domibusPropertyProvider.getProperty(DOMIBUS_DISPATCHER_CONNECTION_KEEP_ALIVE));
        ConnectionType connectionType = ConnectionType.CLOSE;
        if (BooleanUtils.isTrue(keepAlive)) {
            connectionType = ConnectionType.KEEP_ALIVE;
        }
        httpClientPolicy.setConnection(connectionType);
    }

    protected Dispatch<SOAPMessage> createWSServiceDispatcher(String endpoint) {
        final javax.xml.ws.Service service = javax.xml.ws.Service.create(SERVICE_NAME);
        service.setExecutor(executor);
        service.addPort(PORT_NAME, SOAPBinding.SOAP12HTTP_BINDING, endpoint);
        return service.createDispatch(PORT_NAME, SOAPMessage.class, javax.xml.ws.Service.Mode.MESSAGE);
    }

    protected Dispatch<SOAPMessage> createLocalWSServiceDispatcher(String endpoint) {
        final javax.xml.ws.Service service = javax.xml.ws.Service.create(LOCAL_SERVICE_NAME);
        service.setExecutor(executor);
        service.addPort(LOCAL_PORT_NAME, SOAPBinding.SOAP12HTTP_BINDING, endpoint);
        return service.createDispatch(LOCAL_PORT_NAME, SOAPMessage.class, javax.xml.ws.Service.Mode.MESSAGE);
    }

}
