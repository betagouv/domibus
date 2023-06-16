package eu.domibus.backendConnector;

import eu.domibus.ext.services.DomibusPropertyManagerExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.domibus.backendConnector.TestWSPluginPropertyManager.TEST_WSPLUGIN_DOMAIN_ENABLED;

/**
 * @author Ion perpegel
 * @since 5.0
 */
@Service
public class TestWSPluginMock extends BackendConnectorBaseMock {

    public static final String TEST_WS_PLUGIN = "wsPlugin";

    @Autowired
    TestWSPluginPropertyManager testWSPluginPropertyManager;

    public TestWSPluginMock() {
        super(TEST_WS_PLUGIN);
    }

    @Override
    public boolean isEnabled(final String domainCode) {
        return doIsEnabled(domainCode);
    }

    @Override
    public String getDomainEnabledPropertyName() {
        return TEST_WSPLUGIN_DOMAIN_ENABLED;
    }

    @Override
    public DomibusPropertyManagerExt getPropertyManager() {
        return testWSPluginPropertyManager;
    }

    @Override
    public void setEnabled(final String domainCode, final boolean enabled) {
        doSetEnabled(domainCode, enabled);
    }

}
