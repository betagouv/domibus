package eu.domibus.core.user.multitenancy;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.functions.AuthenticatedProcedure;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.user.UserService;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobExecutionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class ActivateSuspendedSuperUsersJobTest {

    @Tested
    ActivateSuspendedSuperUsersJob activateSuspendedSuperUsersJob;


    @Injectable
    private UserService userManagementService;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    protected AuthUtils authUtils;

    @Test
    public void executeJob(@Mocked JobExecutionContext context) {

        activateSuspendedSuperUsersJob.executeJob(context);

        new FullVerifications() {{
            AuthenticatedProcedure function;
            AuthRole authRole;
            boolean forceSecurityContext;
            authUtils.runWithDomibusSecurityContext(function = withCapture(), authRole = withCapture(), forceSecurityContext = withCapture());

            assertEquals(AuthRole.ROLE_AP_ADMIN, authRole);
            assertTrue(forceSecurityContext);
            assertNotNull(function);
        }};
    }

}
