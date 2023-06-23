package eu.domibus.core.crypto;

import eu.domibus.api.cluster.Command;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.MultiDomainCryptoService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class ReloadTruststoreCommandTaskTest {

    @Tested
    ReloadTruststoreCommandTask reloadTruststoreCommandTask;

    @Injectable
    protected MultiDomainCryptoService multiDomainCryptoService;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Test
    public void canHandle() {
        assertTrue(reloadTruststoreCommandTask.canHandle(Command.RELOAD_TRUSTSTORE));
    }

    @Test
    public void canHandleWithDifferentCommand() {
        assertFalse(reloadTruststoreCommandTask.canHandle("anothercommand"));
    }

    @Test
    public void execute(@Injectable Domain domain) {
        new Expectations() {{
            domainContextProvider.getCurrentDomain();
            result = domain;
        }};

        reloadTruststoreCommandTask.execute(new HashMap<>());

        new Verifications() {{
            multiDomainCryptoService.resetTrustStore(domain);
        }};
    }

}
