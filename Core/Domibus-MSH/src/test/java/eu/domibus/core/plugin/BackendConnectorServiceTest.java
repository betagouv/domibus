package eu.domibus.core.plugin;

import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.plugin.BackendConnector;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class BackendConnectorServiceTest {

    @Injectable
    DomainService domainService;

    @Test
    public void getBackendConnector_empty() {
        BackendConnectorProviderImpl backendConnectorProvider = new BackendConnectorProviderImpl(new ArrayList<>(), domainService);

        BackendConnector<?, ?> backendConnector = backendConnectorProvider.getBackendConnector("mybackend");

        assertNull(backendConnector);
    }

    @Test
    public void getBackendConnector(@Injectable BackendConnector<?, ?> b1,
                                    @Injectable BackendConnector<?, ?> b2,
                                    @Injectable BackendConnector<?, ?> b3) {
        BackendConnectorProviderImpl backendConnectorProvider = new BackendConnectorProviderImpl(asList(b1, b2, b3), domainService);
        String backendName = "mybackend";

        new Expectations() {{
            b1.getName();
            result = "b1";

            b2.getName();
            result = "b2";

            b3.getName();
            result = backendName;
        }};

        BackendConnector<?, ?> backendConnector = backendConnectorProvider.getBackendConnector(backendName);

        assertEquals(b3, backendConnector);
    }

}
