package eu.domibus.plugin.ws.backend.dispatch;

import eu.domibus.common.NotificationType;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.ws.property.WSPluginPropertyManager;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.bind.JAXBContext;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * @author François Gautier
 * @since 5.0
 */
@Configuration
public class WSPluginDispatcherTestConfiguration extends WSPluginDispatchConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WSPluginDispatcherTestConfiguration.class);

    @Bean
    public WSPluginMessageBuilder wsPluginMessageBuilder(XMLUtilExtService xmlUtilExtService, JAXBContext jaxbContextWebserviceBackend) {
        return new WSPluginMessageBuilder(xmlUtilExtService, jaxbContextWebserviceBackend, null);
    }

    @Bean
    public WSPluginDispatcher wsPluginDispatcher(DomainContextExtService domainContextExtService,
                                                 WSPluginDispatchClientProvider wsPluginDispatchClientProvider) {
        return new WSPluginDispatcher(domainContextExtService, wsPluginDispatchClientProvider);
    }

    @Bean
    public WSPluginDispatchClientProvider wsPluginDispatchClientProvider(@Qualifier("taskExecutor") Executor executor,
                                                                         TLSReaderExtService tlsReaderDelegate,
                                                                         ProxyCxfUtilExtService proxyUtilExtService,
                                                                         WSPluginPropertyManager wsPluginPropertyManager) {
        return new WSPluginDispatchClientProvider(executor,
                tlsReaderDelegate,
                proxyUtilExtService,
                wsPluginPropertyManager);
    }

    @Bean
    public XMLUtilExtService xmlUtilExtService() {
        return () -> {
            try {
                return MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            } catch (SOAPException e) {
                throw new IllegalStateException("MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL) could not be build");
            }
        };
    }

    @Bean
    public ProxyCxfUtilExtService proxyUtilExtService() {
        return (httpClientPolicy, httpConduit) -> LOG.info("configureProxy");
    }

    @Bean
    public TLSReaderExtService tlsReaderExtService() {
        return domainCode -> new TLSClientParameters();
    }

    @Bean
    public WSPluginPropertyManager wspluginPropertyManager() {
        return new WSPluginPropertyManager();
    }

    @Bean
    public DomibusPropertyExtService domibusPropertyExtService() {
        return new DomibusPropertyExtService() {
            @Override
            public String getProperty(String propertyName) {
                switch (propertyName) {
                    case WSPluginPropertyManager.DISPATCHER_CONNECTION_TIMEOUT:
                        return "1000";
                    case WSPluginPropertyManager.DISPATCHER_RECEIVE_TIMEOUT:
                        return "2000";
                    case WSPluginPropertyManager.DISPATCHER_ALLOW_CHUNKING:
                    case WSPluginPropertyManager.DISPATCHER_CONNECTION_KEEP_ALIVE:
                        return "true";
                    case WSPluginPropertyManager.DISPATCHER_CHUNKING_THRESHOLD:
                        return "3000";
                    default:
                        return null;
                }
            }

            @Override
            public String getProperty(DomainDTO domain, String propertyName) {
                return null;
            }

            @Override
            public Integer getIntegerProperty(String propertyName) {
                return null;
            }

            @Override
            public Boolean getBooleanProperty(String propertyName) {
                return null;
            }

            @Override
            public Set<String> filterPropertiesName(Predicate<String> predicate) {
                return null;
            }

            @Override
            public List<String> getNestedProperties(String prefix) {
                return null;
            }

            @Override
            public List<NotificationType> getConfiguredNotifications(String notificationPropertyName) {
                return null;
            }

            @Override
            public String getDomainProperty(DomainDTO domain, String propertyName) {
                return null;
            }

            @Override
            public void setDomainProperty(DomainDTO domain, String propertyName, String propertyValue) {

            }

            @Override
            public void setProperty(String propertyName, String propertyValue) {

            }

            @Override
            public boolean containsDomainPropertyKey(DomainDTO domain, String propertyName) {
                return false;
            }

            @Override
            public boolean containsPropertyKey(String propertyName) {
                return false;
            }

            @Override
            public String getDomainProperty(DomainDTO domain, String propertyName, String defaultValue) {
                return null;
            }

            @Override
            public String getDomainResolvedProperty(DomainDTO domain, String propertyName) {
                return null;
            }

            @Override
            public String getResolvedProperty(String propertyName) {
                return null;
            }
        };
    }

    @Bean
    public DomainExtService domainExtService() {
        return new DomainExtService() {
            @Override
            public DomainDTO getDomainForScheduler(String schedulerName) {
                return null;
            }

            @Override
            public DomainDTO getDomain(String code) {
                return null;
            }
        };
    }

    @Bean
    public DomainContextExtService domainContextProvider() {
        return new DomainContextExtService() {
            @Override
            public DomainDTO getCurrentDomain() {
                return new DomainDTO("TEST", "TEST");
            }

            @Override
            public DomainDTO getCurrentDomainSafely() {
                return null;
            }

            @Override
            public void setCurrentDomain(DomainDTO domain) {

            }

            @Override
            public void clearCurrentDomain() {

            }
        };
    }

    @Bean(name = "taskExecutor")
    public Executor executor() {
        return command -> LOG.info("Command started with class [{}]", command.getClass());
    }
}