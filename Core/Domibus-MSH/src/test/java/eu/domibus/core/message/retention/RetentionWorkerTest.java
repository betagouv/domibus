package eu.domibus.core.message.retention;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.functions.AuthenticatedProcedure;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.pmode.ConfigurationDAO;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Soumya Chandran
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class RetentionWorkerTest {


    RetentionWorker retentionWorker = new RetentionWorker();

    @Injectable
    private AuthUtils authUtils;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retentionWorker, "authUtils", authUtils);
    }

    @Test
    public void executeJob(@Mocked JobExecutionContext context, @Mocked Domain domain) throws JobExecutionException {

        new Expectations() {{
            authUtils.runWithSecurityContext((AuthenticatedProcedure) any, "retention_user", "retention_password");
        }};

        retentionWorker.executeJob(context, domain);

    }
}
