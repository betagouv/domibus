package eu.domibus.test;

import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.util.DbSchemaUtil;
import eu.domibus.api.util.FaultyDatabaseSchemaNameException;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.user.ui.UserRoleDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;

@Service
public class DomibusConditionUtil {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusConditionUtil.class);
    public static final String SELECT_FROM = "SELECT * FROM ";

    @Autowired
    protected UserRoleDao userRoleDao;

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    protected DbSchemaUtil dbSchemaUtil;

    @Qualifier(DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE)
    @Autowired
    private DataSource dataSource;

    public void waitUntilDatabaseIsInitialized() {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(120, TimeUnit.SECONDS).until(databaseIsInitialized());
    }

    public Callable<Boolean> databaseIsInitialized() {
        return () -> {
            boolean defaultOk = doIsDatabaseSchemaReady(dbSchemaUtil.getDatabaseSchema(new Domain("default", "default")));
            boolean redOk = doIsDatabaseSchemaReady(dbSchemaUtil.getDatabaseSchema(new Domain("red", "default")));
            boolean generalOk = doIsDatabaseSchemaReady(dbSchemaUtil.getGeneralSchema(), true);
            boolean result = defaultOk && redOk && generalOk;
            String msg =
                    "general schema is " + getModifier(generalOk) + " ready, " +
                            "default schema is " + getModifier(defaultOk) + " ready, " +
                            "red schema is " + getModifier(redOk) + " ready. ";
            if (!result) {
                LOG.warn(msg);
            } else {
                LOG.info(msg);
            }
            return result;
        };
    }

    private Boolean doIsDatabaseSchemaReady(String databaseSchema) {
        return doIsDatabaseSchemaReady(databaseSchema, false);
    }

    private Boolean doIsDatabaseSchemaReady(String databaseSchema, boolean general) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                dbSchemaUtil.setSchema(connection, databaseSchema);
            } catch (PersistenceException | FaultyDatabaseSchemaNameException e) {
                LOG.warn("Could not set database schema [{}], so it is not a proper schema.", databaseSchema);
                return false;
            }

            try {
                if (general) {
                    checkDatabase(databaseSchema, connection, "TB_USER_DOMAIN_AUD");
                } else {
                    checkDatabase(databaseSchema, connection, "TB_MESSAGES_TO_RESEND");
                }
                return true;
            } catch (final Exception e) {
                LOG.warn("Could not find last table created for schema [{}]", databaseSchema);
                return false;
            }

        } catch (SQLException e) {
            LOG.warn("Could not create a connection for schema [{}].", databaseSchema);
            return false;
        }
    }

    private static void checkDatabase(String databaseSchema, Connection connection, String... tables) throws SQLException {
        for (String table : tables) {
            try (final Statement statement = connection.createStatement()) {
                String query = SELECT_FROM + table;
                statement.execute(query);
                LOG.trace("Executed statement [{}] for schema [{}]", query, databaseSchema);
            }
        }

    }

    private static String getModifier(boolean redOk) {
        return redOk ? "" : "not";
    }

    public void waitUntilMessageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        Awaitility.with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(20, TimeUnit.SECONDS)
                .until(messageHasStatus(messageId, mshRole, messageStatus));
    }

    public void waitUntilMessageIsAcknowledged(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.ACKNOWLEDGED);
    }

    public void waitUntilMessageIsReceived(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.RECEIVING, MessageStatus.RECEIVED);
    }

    public void waitUntilMessageIsInWaitingForRetry(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.WAITING_FOR_RETRY);
    }

    public Callable<Boolean> messageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        return () -> messageStatus == userMessageLogDao.getMessageStatus(messageId, mshRole);
    }
}
