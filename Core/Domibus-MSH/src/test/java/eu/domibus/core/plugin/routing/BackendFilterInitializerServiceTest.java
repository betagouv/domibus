package eu.domibus.core.plugin.routing;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.functions.AuthenticatedProcedure;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.plugin.BackendConnector;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
public class BackendFilterInitializerServiceTest {

    @Tested
    BackendFilterInitializerService backendFilterInitializerService;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    protected BackendConnectorProvider backendConnectorProvider;

    @Injectable
    protected AuthUtils authUtils;

    @Injectable
    protected DomainTaskExecutor domainTaskExecutor;

    @Injectable
    protected DomainService domainService;

    @Injectable
    protected RoutingService routingService;

    

    @Test
    public void updateMessageFiltersSingleTenancy(@Injectable BackendConnector backendConnector) {
        List<BackendConnector> backendConnectors = new ArrayList<>();
        backendConnectors.add(backendConnector);

        new Expectations(routingService) {{
            backendConnectorProvider.getBackendConnectors();
            result = backendConnectors;

            domibusConfigurationService.isSingleTenantAware();
            result = true;

            authUtils.runWithDomibusSecurityContext((AuthenticatedProcedure) any, (AuthRole) any, anyBoolean);
        }};

        backendFilterInitializerService.updateMessageFilters();

        new FullVerifications(authUtils) {{
            AuthenticatedProcedure function;
            AuthRole role;
            boolean forceSetContext;
            authUtils.runWithDomibusSecurityContext(function = withCapture(),
                    role = withCapture(), forceSetContext = withCapture());
            Assertions.assertNotNull(function);
            Assertions.assertEquals(AuthRole.ROLE_ADMIN, role);
            Assertions.assertTrue(forceSetContext); // always true for audit reasons
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void updateMessageFiltersMultiTenancy(@Injectable BackendConnector backendConnector,
                                                 @Injectable Domain domain) {
        List<BackendConnector> backendConnectors = new ArrayList<>();
        backendConnectors.add(backendConnector);

        List<Domain> domains = new ArrayList<>();
        domains.add(domain);

        new Expectations(routingService) {{
            backendConnectorProvider.getBackendConnectors();
            result = backendConnectors;

            domibusConfigurationService.isSingleTenantAware();
            result = false;

            domainService.getDomains();
            result = Collections.singletonList(domain);

            backendFilterInitializerService.createBackendFilters(domain);
        }};

        backendFilterInitializerService.updateMessageFilters();

    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void testInit_noNotificationListenerBeanMap(@Injectable BackendConnectorProvider backendConnectorProvider,
                                                       @Injectable CriteriaFactory routingCriteriaFactory,
                                                       @Injectable BackendFilterEntity backendFilterEntity) {

        RoutingService routingService = new RoutingService();
        routingService.backendConnectorProvider = backendConnectorProvider;

        new Expectations(routingService) {{
        }};

        Assertions.assertThrows(ConfigurationException. class,() -> backendFilterInitializerService.updateMessageFilters());

        new FullVerifications() {
        };
    }
}
