package eu.domibus.core.plugin;

import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.core.plugin.notification.AsyncNotificationConfigurationService;
import eu.domibus.plugin.AbstractBackendConnector;
import eu.domibus.plugin.BackendConnector;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class BackendConnectorHelperTest {

    @Tested
    BackendConnectorHelper backendConnectorHelper;

    @Injectable
    protected BackendConnectorService backendConnectorService;

    @Injectable
    protected AsyncNotificationConfigurationService asyncNotificationConfigurationService;


    @Test
    public void isAbstractBackendConnector(@Injectable AbstractBackendConnector abstractBackendConnector) {
        assertTrue(backendConnectorHelper.isAbstractBackendConnector(abstractBackendConnector));
    }

    @Test
    public void isAbstractBackendConnector(@Injectable BackendConnector backendConnector) {
        assertFalse(backendConnectorHelper.isAbstractBackendConnector(backendConnector));
    }

}
