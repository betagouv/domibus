package eu.domibus.backendConnector;

import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.test.common.BackendConnectorMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.domibus.backendConnector.TestFSPluginPropertyManager.TEST_FSPLUGIN_DOMAIN_ENABLED;

/**
 * @author Ion perpegel
 * @since 5.0
 */
@Service
public class TestFSPluginMock extends BackendConnectorMock {

    public static final String TEST_FS_PLUGIN = "testFSPlugin";

    @Autowired
    TestFSPluginPropertyManager testPluginPropertyManager;

    public TestFSPluginMock() {
        super(TEST_FS_PLUGIN);
    }

    @Override
    public boolean isEnabled(final String domainCode) {
        return doIsEnabled(domainCode);
    }

    @Override
    public String getDomainEnabledPropertyName() {
        return TEST_FSPLUGIN_DOMAIN_ENABLED;
    }

    @Override
    public DomibusPropertyManagerExt getPropertyManager() {
        return testPluginPropertyManager;
    }

    @Override
    public void setEnabled(final String domainCode, final boolean enabled) {
        doSetEnabled(domainCode, enabled);
    }

}
