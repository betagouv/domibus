package eu.domibus.plugin.ws.property.listeners;

import eu.domibus.ext.services.DomibusSchedulerExtService;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.plugin.ws.property.WSPluginPropertyManager.DISPATCHER_CRON_EXPRESSION;
import static eu.domibus.plugin.ws.property.listeners.WSPluginDispatcherCronExpressionChangeListener.SEND_RETRY_JOB_NAME;

/**
 * @author François Gautier
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class WSPluginDispatcherCronExpressionChangeListenerTest {

    public static final String DEFAULT = "default";
    public static final String REGEX = "*";
    @Injectable
    private DomibusSchedulerExtService domibusSchedulerExtService;

    protected WSPluginDispatcherCronExpressionChangeListener listener;

    @BeforeEach
    public void setUp() {
        listener = new WSPluginDispatcherCronExpressionChangeListener(domibusSchedulerExtService);
    }

    @Test
    public void handlesProperty_true() {
        Assertions.assertTrue(listener.handlesProperty(DISPATCHER_CRON_EXPRESSION));
    }

    @Test
    public void handlesProperty_false() {
        Assertions.assertFalse(listener.handlesProperty("I hate pickles"));
    }

    @Test
    public void propertyValueChanged() {
        listener.propertyValueChanged(DEFAULT, DISPATCHER_CRON_EXPRESSION, REGEX);

        new FullVerifications() {{
            domibusSchedulerExtService.rescheduleJob(DEFAULT, SEND_RETRY_JOB_NAME, REGEX);
            times = 1;
        }};
    }
}
