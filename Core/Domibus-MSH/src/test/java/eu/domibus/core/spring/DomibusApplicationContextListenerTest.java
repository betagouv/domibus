package eu.domibus.core.spring;

import eu.domibus.api.crypto.TLSCertificateManager;
import eu.domibus.api.encryption.EncryptionService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.core.earchive.storage.EArchiveFileStorageProvider;
import eu.domibus.core.jms.MessageListenerContainerInitializer;
import eu.domibus.core.message.dictionary.StaticDictionaryService;
import eu.domibus.core.metrics.JmsQueueCountSetScheduler;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.initializer.PluginInitializerProvider;
import eu.domibus.core.plugin.routing.BackendFilterInitializerService;
import eu.domibus.core.plugin.routing.RoutingService;
import eu.domibus.core.property.DomibusPropertyValidatorService;
import eu.domibus.core.property.GatewayConfigurationValidator;
import eu.domibus.core.scheduler.DomibusQuartzStarter;
import eu.domibus.core.user.ui.UserManagementServiceImpl;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.xml.ws.Endpoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@RunWith(JMockit.class)
public class DomibusApplicationContextListenerTest {

    @Tested
    DomibusApplicationContextListener domibusApplicationContextListener;

    @Injectable
    protected BackendFilterInitializerService backendFilterInitializerService;

    @Injectable
    protected EncryptionService encryptionService;

    @Injectable
    protected MessageListenerContainerInitializer messageListenerContainerInitializer;

    @Injectable
    protected JmsQueueCountSetScheduler jmsQueueCountSetScheduler;

    @Injectable
    protected PayloadFileStorageProvider payloadFileStorageProvider;

    @Injectable
    protected RoutingService routingService;

    @Injectable
    protected DomibusQuartzStarter domibusQuartzStarter;

    @Injectable
    protected EArchiveFileStorageProvider eArchiveFileStorageProvider;

    @Injectable
    protected PluginInitializerProvider pluginInitializerProvider;

    @Injectable
    protected StaticDictionaryService staticDictionaryService;

    @Injectable
    protected DomainTaskExecutor domainTaskExecutor;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    GatewayConfigurationValidator gatewayConfigurationValidator;

    @Injectable
    MultiDomainCryptoService multiDomainCryptoService;

    @Injectable
    TLSCertificateManager tlsCertificateManager;

    @Injectable
    UserManagementServiceImpl userManagementService;

    @Injectable
    DomibusPropertyValidatorService domibusPropertyValidatorService;

    @Injectable
    BackendConnectorService backendConnectorService;

    @Injectable
    Endpoint mshEndpoint;

    @Test
    public void onApplicationEventThatShouldBeDiscarded(@Injectable ContextRefreshedEvent event,
                                                        @Injectable ApplicationContext applicationContext) {
        new Expectations() {{
            event.getApplicationContext();
            result = applicationContext;

            applicationContext.getParent();
            result = null;
        }};

        domibusApplicationContextListener.onApplicationEvent(event);

        new FullVerifications() {{
            encryptionService.handleEncryption();
            times = 0;

            backendFilterInitializerService.updateMessageFilters();
            times = 0;
        }};
    }

    @Test
    public void onApplicationEvent(@Injectable ContextRefreshedEvent event,
                                   @Injectable ApplicationContext applicationContext,
                                   @Injectable ApplicationContext parent) {

        domibusApplicationContextListener.executeSynchronized(true);

        new Verifications() {{
            tlsCertificateManager.saveStoresFromDBToDisk();
            times = 1;

            multiDomainCryptoService.saveStoresFromDBToDisk();
            times = 1;

            userManagementService.createDefaultUserIfApplicable();
            times = 1;

            staticDictionaryService.createStaticDictionaryEntries();
            times = 1;

            encryptionService.handleEncryption();
            times = 1;

            backendFilterInitializerService.updateMessageFilters();
            times = 1;

            domibusPropertyValidatorService.enforceValidation();
            times = 1;

        }};
    }

}
