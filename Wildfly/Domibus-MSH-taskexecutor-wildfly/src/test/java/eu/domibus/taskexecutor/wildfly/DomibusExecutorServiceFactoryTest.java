package eu.domibus.taskexecutor.wildfly;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cosmin Baciu
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class DomibusExecutorServiceFactoryTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusExecutorServiceFactoryTest.class);

    @Tested
    DomibusExecutorServiceFactory domibusExecutorServiceFactory;

    @Test
    public void testGetObjectWithGlobalExecutorService(final @Injectable ManagedExecutorService managedExecutorService) throws Exception {
        new Expectations(domibusExecutorServiceFactory) {{
            domibusExecutorServiceFactory.getGlobalExecutorService();
            result = managedExecutorService;
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.getObject();
        new Verifications() {{
            assertSame(managedExecutorService, returnedExecutorService);
        }};
    }

    @Test
    public void testGetObjectWithDefaultExecutorService(final @Injectable ManagedExecutorService managedExecutorService) throws Exception {
        new Expectations(domibusExecutorServiceFactory) {{
            domibusExecutorServiceFactory.getGlobalExecutorService();
            result = null;

            domibusExecutorServiceFactory.getDefaultExecutorService();
            result = managedExecutorService;
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.getObject();
        new Verifications() {{
            domibusExecutorServiceFactory.getDefaultExecutorService();
            assertSame(managedExecutorService, returnedExecutorService);
        }};
    }

    @Test
    public void testGetObjectWithNoExecutorService(final @Injectable ManagedExecutorService managedExecutorService) throws Exception {
        new Expectations(domibusExecutorServiceFactory) {{
            domibusExecutorServiceFactory.getGlobalExecutorService();
            result = null;

            domibusExecutorServiceFactory.getDefaultExecutorService();
            result = null;
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.getObject();
        new Verifications() {{
            domibusExecutorServiceFactory.getGlobalExecutorService();
            domibusExecutorServiceFactory.getDefaultExecutorService();
            assertNull(returnedExecutorService);
        }};
    }

    @Test
    public void testLookupExecutorService(@Mocked InitialContext initialContext, final @Injectable ManagedExecutorService managedExecutorService) throws Exception {
        final String jndiName = "myname";
        new Expectations() {{
            InitialContext.doLookup(jndiName);
            result = managedExecutorService;
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.lookupExecutorService(jndiName);

        new Verifications() {{
            assertSame(managedExecutorService, returnedExecutorService);
        }};
    }

    @Test
    public void testLookupWorkManagerWhenLookupExceptionIsRaised(@Mocked InitialContext initialContext, final @Injectable ManagedExecutorService managedExecutorService) throws Exception {
        final String jndiName = "myname";
        new Expectations() {{
            InitialContext.doLookup(jndiName);
            result = new NamingException();
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.lookupExecutorService(jndiName);

        new Verifications() {{
            assertNull(returnedExecutorService);
        }};
    }

    @Test
    public void testGetDefaultExecutorService(final @Injectable ManagedExecutorService managedExecutorService) {
        new Expectations(domibusExecutorServiceFactory) {{
            domibusExecutorServiceFactory.lookupExecutorService(DomibusExecutorServiceFactory.DEFAULT_EXECUTOR_SERVICE);
            result = managedExecutorService;
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.getDefaultExecutorService();

        new Verifications() {{
            domibusExecutorServiceFactory.lookupExecutorService(DomibusExecutorServiceFactory.DEFAULT_EXECUTOR_SERVICE);
            assertSame(managedExecutorService, returnedExecutorService);
        }};
    }

    @Test
    public void testGetGlobalWorkManager(final @Injectable ManagedExecutorService managedExecutorService) {
        final String executorServiceJndi = "myjndi";
        domibusExecutorServiceFactory.setExecutorServiceJndiName(executorServiceJndi);
        new Expectations(domibusExecutorServiceFactory) {{
            domibusExecutorServiceFactory.lookupExecutorService(executorServiceJndi);
            result = managedExecutorService;
        }};

        final ManagedExecutorService returnedExecutorService = domibusExecutorServiceFactory.getGlobalExecutorService();

        new Verifications() {{
            domibusExecutorServiceFactory.lookupExecutorService(executorServiceJndi);
            assertSame(managedExecutorService, returnedExecutorService);
        }};
    }

}
