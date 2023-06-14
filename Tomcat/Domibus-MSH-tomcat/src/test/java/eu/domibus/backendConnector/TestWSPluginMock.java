package eu.domibus.backendConnector;

import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.test.common.BackendConnectorMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Ion perpegel
 * @since 5.0
 */
@Service
public class TestWSPluginMock extends BackendConnectorMock {

    @Autowired
    TestWSPluginPropertyManager testWSPluginPropertyManager;

    public TestWSPluginMock() {
        super("backendWSPlugin");
    }

    @Override
    public boolean isEnabled(final String domainCode) {
        return doIsEnabled(domainCode);
    }

    @Override
    public String getDomainEnabledPropertyName() {
        return "testPlugin.domain.enabled";
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
