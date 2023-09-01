package eu.domibus.core.audit;

import eu.domibus.api.spring.SpringContextProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.core.audit.envers.ModificationType;
import eu.domibus.core.audit.envers.RevisionLog;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.hibernate.envers.RevisionType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@SuppressWarnings("unused")
@ExtendWith(JMockitExtension.class)
public class CustomRevisionEntityListenerTest {

    @Tested
    CustomRevisionEntityListener customRevisionEntityListener;

    @Test
    public void newRevisionWithAuthentication(@Mocked SecurityContextHolder securityContextHolder, @Mocked Authentication authentication)  {
        new Expectations() {{
            SecurityContextHolder.getContext().getAuthentication();
            result = authentication;
            authentication.getName();
            result = "Thomas";
        }};
        RevisionLog revision = new RevisionLog();
        customRevisionEntityListener.newRevision(revision);
        assertNotNull(revision.getRevisionDate());
        assertEquals("Thomas", revision.getUserName());
    }

    @Test
    public void newRevisionWithNullAuthentication(@Mocked SecurityContextHolder securityContextHolder) {
        new Expectations() {{
            SecurityContextHolder.getContext().getAuthentication();
            result = null;
        }};
        RevisionLog revision = new RevisionLog();
        customRevisionEntityListener.newRevision(revision);
        assertNotNull(revision.getRevisionDate());
        assertNull(revision.getUserName());
    }

    @Test
    public void newRevisionWithDatabaseAuthentication(@Mocked SecurityContextHolder securityContextHolder,
                                                      @Mocked SpringContextProvider springContextProvider,
                                                      @Mocked ApplicationContext applicationContext,
                                                      @Mocked DatabaseUtil databaseUtil) {
        String databaseUsername = "DatabaseUser";

        new Expectations() {{
            SpringContextProvider.getApplicationContext();
            result = applicationContext;
            applicationContext.getBean(DatabaseUtil.DATABASE_USER, DatabaseUtil.class);
            result = databaseUtil;
            databaseUtil.getDatabaseUserName();
            result = databaseUsername;

            SecurityContextHolder.getContext().getAuthentication();
            result = null;
        }};
        RevisionLog revision = new RevisionLog();
        customRevisionEntityListener.newRevision(revision);
        assertNotNull(revision.getRevisionDate());
        assertEquals(databaseUsername, revision.getUserName());
    }

    @Test
    public void entityChanged(@Mocked RevisionLog revisionEntity) {
        customRevisionEntityListener.entityChanged(Configuration.class, "eu.domibus.common.model.configuration.Configuration", 1, ADD, revisionEntity);
        new Verifications() {{
            revisionEntity.addEntityAudit("1", "eu.domibus.common.model.configuration.Configuration", "Pmode", ModificationType.ADD, 0);
            times = 1;
        }};
    }

    @Test
    public void getModificationType() {

        assertEquals(ModificationType.MOD, customRevisionEntityListener.getModificationType(MOD));
        assertEquals(ModificationType.DEL, customRevisionEntityListener.getModificationType(DEL));
    }

}
