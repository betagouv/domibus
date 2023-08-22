package eu.domibus.core.spring;

import eu.domibus.api.crypto.TLSCertificateManager;
import eu.domibus.api.encryption.EncryptionService;
import eu.domibus.api.multitenancy.DomainTaskException;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.multitenancy.lock.DomibusSynchronizationException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.core.earchive.storage.EArchiveFileStorageProvider;
import eu.domibus.core.ebms3.receiver.MSHWebserviceConfiguration;
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
import eu.domibus.core.user.UserService;
import eu.domibus.core.user.multitenancy.SuperUserManagementServiceImpl;
import eu.domibus.core.user.ui.UserManagementServiceImpl;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.initialize.PluginInitializer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.xml.ws.Endpoint;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Cosmin Baciu
 * @author Ion Perpegel
 * @since 4.1
 */
@Component
public class DomibusApplicationContextListener {

    private final static DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusApplicationContextListener.class);

    private static final Object initLock = new Object();

    public static final String SYNC_LOCK_KEY = "bootstrap-synchronization.lock";

    protected final EncryptionService encryptionService;

    protected final BackendFilterInitializerService backendFilterInitializerService;

    protected final StaticDictionaryService messageDictionaryService;

    protected final DomibusConfigurationService domibusConfigurationService;

    protected final DomainTaskExecutor domainTaskExecutor;

    protected final GatewayConfigurationValidator gatewayConfigurationValidator;

    protected final MultiDomainCryptoService multiDomainCryptoService;

    protected final TLSCertificateManager tlsCertificateManager;

    protected final UserManagementServiceImpl userManagementService;

    final SuperUserManagementServiceImpl superUserManagementService;

    protected final DomibusPropertyValidatorService domibusPropertyValidatorService;

    protected final BackendConnectorService backendConnectorService;

    protected final MessageListenerContainerInitializer messageListenerContainerInitializer;

    protected JmsQueueCountSetScheduler jmsQueueCountSetScheduler;

    protected PayloadFileStorageProvider payloadFileStorageProvider;

    protected RoutingService routingService;

    protected DomibusQuartzStarter domibusQuartzStarter;

    protected EArchiveFileStorageProvider eArchiveFileStorageProvider;

    protected PluginInitializerProvider pluginInitializerProvider;

    protected Endpoint mshEndpoint;

    public DomibusApplicationContextListener(EncryptionService encryptionService,
                                             BackendFilterInitializerService backendFilterInitializerService,
                                             StaticDictionaryService messageDictionaryService,
                                             DomibusConfigurationService domibusConfigurationService,
                                             DomainTaskExecutor domainTaskExecutor,
                                             GatewayConfigurationValidator gatewayConfigurationValidator,
                                             MultiDomainCryptoService multiDomainCryptoService,
                                             TLSCertificateManager tlsCertificateManager,
                                             UserManagementServiceImpl userManagementService,
                                             SuperUserManagementServiceImpl superUserManagementService,
                                             DomibusPropertyValidatorService domibusPropertyValidatorService,
                                             BackendConnectorService backendConnectorService,
                                             MessageListenerContainerInitializer messageListenerContainerInitializer,
                                             JmsQueueCountSetScheduler jmsQueueCountSetScheduler,
                                             PayloadFileStorageProvider payloadFileStorageProvider,
                                             RoutingService routingService,
                                             DomibusQuartzStarter domibusQuartzStarter,
                                             EArchiveFileStorageProvider eArchiveFileStorageProvider,
                                             PluginInitializerProvider pluginInitializerProvider,
                                             @Qualifier(MSHWebserviceConfiguration.MSH_BEAN_NAME) Endpoint mshEndpoint) {
        this.encryptionService = encryptionService;
        this.backendFilterInitializerService = backendFilterInitializerService;
        this.messageDictionaryService = messageDictionaryService;
        this.domibusConfigurationService = domibusConfigurationService;
        this.domainTaskExecutor = domainTaskExecutor;
        this.gatewayConfigurationValidator = gatewayConfigurationValidator;
        this.multiDomainCryptoService = multiDomainCryptoService;
        this.tlsCertificateManager = tlsCertificateManager;
        this.userManagementService = userManagementService;
        this.superUserManagementService = superUserManagementService;
        this.domibusPropertyValidatorService = domibusPropertyValidatorService;
        this.backendConnectorService = backendConnectorService;
        this.messageListenerContainerInitializer = messageListenerContainerInitializer;
        this.jmsQueueCountSetScheduler = jmsQueueCountSetScheduler;
        this.payloadFileStorageProvider = payloadFileStorageProvider;
        this.routingService = routingService;
        this.domibusQuartzStarter = domibusQuartzStarter;
        this.eArchiveFileStorageProvider = eArchiveFileStorageProvider;
        this.pluginInitializerProvider = pluginInitializerProvider;
        this.mshEndpoint = mshEndpoint;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOG.info("Start processing ContextRefreshedEvent");

        final ApplicationContext applicationContext = event.getApplicationContext();
        if (applicationContext.getParent() == null) {
            LOG.info("Skipping event: we are processing only the web application context event");
            return;
        }

        initialize();

        LOG.info("Finished processing ContextRefreshedEvent");
    }

    public void initialize() {
        try {
            executeWithLock(() -> executeSynchronized(true));
        } catch (DomainTaskException | DomibusSynchronizationException ex) {
            Throwable cause = ExceptionUtils.getRootCause(ex);
            LOG.error("Error executing application initialization code:", cause);
        }
        executeNonSynchronized(true);
    }

    public void initializeForTests() {
        executeSynchronized(false);
        executeNonSynchronized(false);
    }

    /**
     * Method executed in a serial/sync mode (if in a cluster environment)
     * Add code that needs to be executed with regard to other nodes in the cluster
     */
    protected void executeSynchronized(boolean completeInitialization) {
        messageDictionaryService.createStaticDictionaryEntries();
        multiDomainCryptoService.saveStoresFromDBToDisk();
        tlsCertificateManager.saveStoresFromDBToDisk();
        domibusPropertyValidatorService.enforceValidation();
        encryptionService.handleEncryption();
        getUserService().createDefaultUserIfApplicable();

        if (completeInitialization) {
            backendFilterInitializerService.updateMessageFilters();
        }

        initializePluginsWithLockIfNeeded();
    }

    private void initializePluginsWithLockIfNeeded() {
        final List<PluginInitializer> pluginInitializers = pluginInitializerProvider.getPluginInitializersForEnabledPlugins();
        for (PluginInitializer pluginInitializer : pluginInitializers) {
            try {
                pluginInitializer.initializeWithLockIfNeeded();
            } catch (Exception e) {
                LOG.error("Error executing plugin initializer [{}] with lock", pluginInitializer.getName(), e);
            }
        }
    }

    /**
     * Method executed in a parallel/not sync mode (in any environment)
     * Add code that does not need to be executed with regard to other nodes in the cluster
     */
    protected void executeNonSynchronized(boolean completeInitialization) {
        messageListenerContainerInitializer.initialize();
        jmsQueueCountSetScheduler.initialize();
        payloadFileStorageProvider.initialize();
        routingService.initialize();

        eArchiveFileStorageProvider.initialize();

        if (completeInitialization) {
            //this is added on purpose in the non-synchronized area; the initialize method has a more complex logic to decide if it executes in synchronized way
            domibusQuartzStarter.initialize();
        }

        gatewayConfigurationValidator.validateConfiguration();
        backendConnectorService.ensureValidConfiguration();

        initializePluginsNonSynchronized();

        LOG.info("Publishing the /msh endpoint");
        mshEndpoint.publish("/msh");
    }

    private void initializePluginsNonSynchronized() {
        final List<PluginInitializer> pluginInitializers = pluginInitializerProvider.getPluginInitializersForEnabledPlugins();
        for (PluginInitializer pluginInitializer : pluginInitializers) {
            try {
                pluginInitializer.initializeNonSynchronized();
            } catch (Exception e) {
                LOG.error("Error executing plugin initializer [{}]", pluginInitializer.getName(), e);
            }
        }
    }

    protected void executeWithLock(Runnable task) {
        Runnable errorHandler = () -> {
            LOG.warn("An error has occurred while initializing Domibus (executing task [{}]). " +
                    "This does not necessarily mean that Domibus did not start correctly. Please check the Domibus logs for more info.", task);
        };
        Callable<Boolean> wrappedTask = () -> {
            task.run();
            return true;
        };
        domainTaskExecutor.executeWithLock(wrappedTask, SYNC_LOCK_KEY, initLock, errorHandler);
    }


    UserService getUserService() {
        if (domibusConfigurationService.isMultiTenantAware()) {
            return superUserManagementService;
        } else {
            return userManagementService;
        }
    }

    UserService getUserService() {
        if (domibusConfigurationService.isMultiTenantAware()) {
            return superUserManagementService;
        } else {
            return userManagementService;
        }
    }

}
