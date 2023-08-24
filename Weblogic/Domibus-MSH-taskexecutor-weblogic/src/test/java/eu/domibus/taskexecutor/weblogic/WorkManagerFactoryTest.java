package eu.domibus.taskexecutor.weblogic;

import commonj.work.WorkManager;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Cosmin Baciu
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class WorkManagerFactoryTest {

    @Tested
    WorkManagerFactory workManagerFactory;

    @Test
    public void testGetObjectWithGlobalWorkManager(final @Injectable WorkManager workManager) throws Exception {
        new Expectations(workManagerFactory) {{
            workManagerFactory.getGlobalWorkManager();
            result = workManager;
        }};

        final WorkManager returnedWorkManager = workManagerFactory.getObject();
        new Verifications() {{
            assertSame(workManager, returnedWorkManager);
        }};
    }

    @Test
    public void testGetObjectWithDefaultWorkManager(final @Injectable WorkManager workManager) throws Exception {
        new Expectations(workManagerFactory) {{
            workManagerFactory.getGlobalWorkManager();
            result = null;

            workManagerFactory.getDefaultWorkManager();
            result = workManager;
        }};

        final WorkManager returnedWorkManager = workManagerFactory.getObject();
        new Verifications() {{
            workManagerFactory.getDefaultWorkManager();
            assertSame(workManager, returnedWorkManager);
        }};
    }

    @Test
    public void testGetObjectWithNoWorkManager(final @Injectable WorkManager workManager) throws Exception {
        new Expectations(workManagerFactory) {{
            workManagerFactory.getGlobalWorkManager();
            result = null;

            workManagerFactory.getDefaultWorkManager();
            result = null;
        }};

        final WorkManager returnedWorkManager = workManagerFactory.getObject();
        new Verifications() {{
            workManagerFactory.getGlobalWorkManager();
            workManagerFactory.getDefaultWorkManager();
            assertNull(returnedWorkManager);
        }};
    }

    @Test
    public void testLookupWorkManager(@Mocked InitialContext initialContext, final @Injectable WorkManager workManager) throws Exception {
        final String jndiName = "myname";
        new Expectations() {{
            InitialContext.doLookup(jndiName);
            result = workManager;
        }};

        final WorkManager returnedWorkManager = workManagerFactory.lookupWorkManager(jndiName);

        new Verifications() {{
            assertSame(workManager, returnedWorkManager);
        }};
    }

    @Test
    public void testLookupWorkManagerWhenLookupExceptionIsRaised(@Mocked InitialContext initialContext, final @Injectable WorkManager workManager) throws Exception {
        final String jndiName = "myname";
        new Expectations() {{
            InitialContext.doLookup(jndiName);
            result = new NamingException();
        }};

        final WorkManager returnedWorkManager = workManagerFactory.lookupWorkManager(jndiName);

        new Verifications() {{
            assertNull(returnedWorkManager);
        }};
    }

    @Test
    public void testGetDefaultWorkManager(final @Injectable WorkManager workManager)  {
        new Expectations(workManagerFactory) {{
            workManagerFactory.lookupWorkManager(WorkManagerFactory.DEFAULT_WORK_MANAGER);
            result = workManager;
        }};

        final WorkManager returnedWorkManager = workManagerFactory.getDefaultWorkManager();

        new Verifications() {{
            workManagerFactory.lookupWorkManager(WorkManagerFactory.DEFAULT_WORK_MANAGER);
            assertSame(workManager, returnedWorkManager);
        }};
    }

    @Test
    public void testGetGlobalWorkManager(final @Injectable WorkManager workManager)  {
        final String workManagerJndiName = "myjndi";
        workManagerFactory.setWorkManagerJndiName(workManagerJndiName);
        new Expectations(workManagerFactory) {{
            workManagerFactory.lookupWorkManager(workManagerJndiName);
            result = workManager;
        }};

        final WorkManager returnedWorkManager = workManagerFactory.getGlobalWorkManager();

        new Verifications() {{
            workManagerFactory.lookupWorkManager(workManagerJndiName);
            assertSame(workManager, returnedWorkManager);
        }};
    }

}
