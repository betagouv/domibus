package eu.domibus.plugin.fs.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.scheduler.DomibusScheduler;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.BackendConnectorProviderExtService;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusSchedulerExtService;
import eu.domibus.plugin.fs.FSPluginImpl;
import eu.domibus.plugin.fs.property.listeners.FSPluginEnabledChangeListener;
import eu.domibus.plugin.fs.property.listeners.OutQueueConcurrencyChangeListener;
import eu.domibus.plugin.fs.property.listeners.TriggerChangeListener;
import eu.domibus.plugin.fs.queue.FSSendMessageListenerContainer;
import eu.domibus.test.AbstractIT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.*;

/**
 * @author Ion Perpegel
 * @author Catalin Enache
 * @since 4.2
 */
public class ChangeListenersTestIT extends AbstractIT {

    @Autowired
    private DomibusSchedulerExtService domibusSchedulerExt;

    @Autowired
    private FSSendMessageListenerContainer messageListenerContainer;

    @Autowired
    private OutQueueConcurrencyChangeListener outQueueConcurrencyChangeListener;

    @Autowired
    private TriggerChangeListener triggerChangeListener;

    @Autowired
    private FSPluginEnabledChangeListener enabledChangeListener;

    @Autowired
    private DomainExtService domainExtService;

    @Autowired
    private FSPluginProperties fsPluginProperties;

    @Autowired
    BackendConnectorProviderExtService backendConnectorProviderExtService;

    @Autowired
    DomibusScheduler domibusScheduler;

    @Configuration
    static class ContextConfiguration {

        @Primary
        @Bean
        public DomibusSchedulerExtService domibusSchedulerExt() {
            return Mockito.mock(DomibusSchedulerExtService.class);
        }

        @Primary
        @Bean
        public DomibusScheduler domibusScheduler() {
            return Mockito.mock(DomibusScheduler.class);
        }

        @Primary
        @Bean
        public FSSendMessageListenerContainer messageListenerContainer() {
            return Mockito.mock(FSSendMessageListenerContainer.class);
        }

    }

    @Test
    public void testTriggerChangeListener() {
        boolean handlesWorkerInterval = triggerChangeListener.handlesProperty(SEND_WORKER_INTERVAL);
        Assertions.assertTrue(handlesWorkerInterval);

        try {
            triggerChangeListener.propertyValueChanged("default", SEND_WORKER_INTERVAL, "wrong-value");
            Assertions.fail("Expected exception not raised");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("Invalid"));
        }

        triggerChangeListener.propertyValueChanged("default", SEND_WORKER_INTERVAL, "3000");
        triggerChangeListener.propertyValueChanged("default", SENT_PURGE_WORKER_CRONEXPRESSION, "0 0/15 * * * ?");

        Mockito.verify(domibusSchedulerExt, Mockito.times(1)).rescheduleJob("default", "fsPluginSendMessagesWorkerJob", 3000);
        Mockito.verify(domibusSchedulerExt, Mockito.times(1)).rescheduleJob("default", "fsPluginPurgeSentWorkerJob", "0 0/15 * * * ?");
    }

    @Test
    public void testOutQueueConcurrencyChangeListener() {

        boolean handlesProperty = outQueueConcurrencyChangeListener.handlesProperty(OUT_QUEUE_CONCURRENCY);
        Assertions.assertTrue(handlesProperty);

        DomainDTO aDefault = domainExtService.getDomain("default");

        outQueueConcurrencyChangeListener.propertyValueChanged("default", OUT_QUEUE_CONCURRENCY, "1-2");
        Mockito.verify(messageListenerContainer, Mockito.times(1)).updateMessageListenerContainerConcurrency(aDefault, "1-2");
    }

    @Test
    public void testEnabledChangeListenerException() {
        boolean handlesProperty = enabledChangeListener.handlesProperty(DOMAIN_ENABLED);
        Assertions.assertTrue(handlesProperty);

        try {
            fsPluginProperties.setKnownPropertyValue(DOMAIN_ENABLED, "false");
            Assertions.fail();
        } catch (DomibusPropertyException ex) {
        }
    }

    @Test
    public void testEnabledChangeListener() {
        String domainCode = "default";
        Domain domain = new Domain(domainCode, domainCode);

        boolean handlesProperty = enabledChangeListener.handlesProperty(DOMAIN_ENABLED);
        Assertions.assertTrue(handlesProperty);

        if (!fsPluginProperties.getDomainEnabled(domainCode)) {
            Mockito.verify(domibusScheduler, Mockito.times(1)).resumeJobs(domain, FSPluginImpl.FSPLUGIN_JOB_NAMES);
            fsPluginProperties.setKnownPropertyValue(DOMAIN_ENABLED, "true");
            Mockito.verify(domibusScheduler, Mockito.times(0)).resumeJobs(domain, FSPluginImpl.FSPLUGIN_JOB_NAMES);
        } else {
            fsPluginProperties.setKnownPropertyValue(DOMAIN_ENABLED, "true");
            Mockito.verify(domibusScheduler, Mockito.times(0)).resumeJobs(domain, FSPluginImpl.FSPLUGIN_JOB_NAMES);
        }
    }
}
