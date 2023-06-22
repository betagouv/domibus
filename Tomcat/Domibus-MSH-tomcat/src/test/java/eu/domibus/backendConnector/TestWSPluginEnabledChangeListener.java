package eu.domibus.backendConnector;

import eu.domibus.ext.services.BackendConnectorProviderExtService;
import eu.domibus.plugin.property.DefaultEnabledChangeListener;
import org.springframework.stereotype.Component;

import static eu.domibus.backendConnector.TestFSPluginMock.TEST_FS_PLUGIN;
import static eu.domibus.backendConnector.TestFSPluginPropertyManager.TEST_FSPLUGIN_DOMAIN_ENABLED;
import static eu.domibus.backendConnector.TestWSPluginMock.TEST_WS_PLUGIN;
import static eu.domibus.backendConnector.TestWSPluginPropertyManager.TEST_WSPLUGIN_DOMAIN_ENABLED;

/**
 * @author Ion Perpegel
 * @since 5.2
 * <p>
 * Handles enabling/disabling of ws-plugin for the current domain.
 */
@Component
public class TestWSPluginEnabledChangeListener extends DefaultEnabledChangeListener {

    public TestWSPluginEnabledChangeListener(BackendConnectorProviderExtService backendConnectorProviderExtService) {
        super(backendConnectorProviderExtService);
    }

    @Override
    protected String getEnabledPropertyName() {
        return TEST_WSPLUGIN_DOMAIN_ENABLED;
    }

    @Override
    protected String getPluginName() {
        return TEST_WS_PLUGIN;
    }
}
