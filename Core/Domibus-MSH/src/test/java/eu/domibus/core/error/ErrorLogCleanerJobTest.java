package eu.domibus.core.error;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.util.DatabaseUtil;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobExecutionContext;

/**
 * @since 5.0
 * @author Catalin Enache
 */
@ExtendWith(JMockitExtension.class)
public class ErrorLogCleanerJobTest {

    @Tested
    ErrorLogCleanerJob errorLogCleanerJob;

    @Injectable
    private ErrorLogService errorLogService;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private AuthUtils authUtils;

    @Test
    public void executeJob(@Injectable JobExecutionContext context, @Injectable Domain domain) throws  Exception {

        errorLogCleanerJob.executeJob(context, domain);

        new FullVerifications() {{
            errorLogService.deleteErrorLogWithoutMessageIds();
        }};
    }
}
