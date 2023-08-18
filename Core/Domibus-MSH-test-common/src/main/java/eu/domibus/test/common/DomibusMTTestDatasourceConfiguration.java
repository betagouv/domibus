package eu.domibus.test.common;

import com.zaxxer.hikari.HikariDataSource;
import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.TimeZone;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_DATABASE_GENERAL_SCHEMA;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ion Perpegel
 * @since 5.1
 */
@Configuration
public class DomibusMTTestDatasourceConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusMTTestDatasourceConfiguration.class);

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Bean
    public AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext() {
        return new AnnotationConfigWebApplicationContext();
    }

    @Primary
    @Bean(name = {DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE, DataSourceConstants.DOMIBUS_JDBC_QUARTZ_DATA_SOURCE}, destroyMethod = "close")
    public DataSource domibusDatasource() {
        HikariDataSource dataSource = createDataSource();
        return dataSource;
    }

    private HikariDataSource createDataSource() {
        JdbcDataSource h2DataSource = createH2Datasource();

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setJdbcUrl(h2DataSource.getUrl());
        dataSource.setUsername(h2DataSource.getUser());
        dataSource.setPassword(h2DataSource.getPassword());
        dataSource.setAutoCommit(false);

        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setMaxLifetime(5 * 1000L);
        dataSource.setConnectionTimeout(5 * 1000L);
        dataSource.setIdleTimeout(5 * 1000L);
        dataSource.setMaximumPoolSize(100);
        return dataSource;
    }

    private JdbcDataSource createH2Datasource() {
        JdbcDataSource result = new JdbcDataSource();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        String schemaH2ScriptFullPath = writeScriptFromClasspathToLocalDirectory("schema-h2.sql", "config/database");

        //Enable logs for H2 with ';TRACE_LEVEL_FILE=4' at the end of databaseUrlTemplate
        final String databaseUrlTemplate = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE;" +
                "NON_KEYWORDS=DAY,VALUE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_LOCK_TIMEOUT=3000;INIT= "

                + createGeneralSchemaSqlScripts("test_general")
                + createDomainSQLScripts("test_domain1")
                + createDomainSQLScripts("test_domain2")

                + " runscript from '" + FilenameUtils.separatorsToUnix(schemaH2ScriptFullPath) + "'";

        final String generalSchema = domibusPropertyProvider.getProperty(DOMIBUS_DATABASE_GENERAL_SCHEMA);
        String databaseUrl = String.format(databaseUrlTemplate, generalSchema);
        LOG.info("Using database URL [{}]", databaseUrl);

        result.setUrl(databaseUrl);
        result.setUser("sa");
        result.setPassword("");
        return result;
    }

    private String createGeneralSchemaSqlScripts(String name) {
        String script = "DROP ALL OBJECTS;\n" +
                "DROP TABLE IF EXISTS SPRING_SESSION_ATTRIBUTES;\n" +
                "DROP TABLE IF EXISTS SPRING_SESSION;\n" +
                "CREATE SCHEMA IF NOT EXISTS " + name + ";\n" +
                "SET SCHEMA " + name + ";";
        String generalSchemaCreateScript = writeScriptToLocalDirectory(script, "create_" + name + "_schema.sql");
        String generalSchemaDDLScript = writeScriptFromClasspathToLocalDirectory("domibus-h2-multi-tenancy.sql", "test-sql-scripts");
        String generalSchemaDataScript = writeScriptFromClasspathToLocalDirectory("domibus-h2-multi-tenancy-data.sql", "test-sql-scripts");

        return createSQLScripts(generalSchemaCreateScript, generalSchemaDDLScript, generalSchemaDataScript);
    }

    private String createDomainSQLScripts(String name) {
        String script = "CREATE SCHEMA IF NOT EXISTS " + name + ";\n" +
                "SET SCHEMA " + name + ";";
        String domainSchemaCreateScript = writeScriptToLocalDirectory(script, "create_" + name + "_schema.sql");
        String domainSchemaDDLScript = writeScriptFromClasspathToLocalDirectory("domibus-h2.sql", "test-sql-scripts");
        String domainSchemaDataScript = writeScriptFromClasspathToLocalDirectory("domibus-h2-data.sql", "test-sql-scripts");

        return createSQLScripts(domainSchemaCreateScript, domainSchemaDDLScript, domainSchemaDataScript);
    }

    private String createSQLScripts(String createScript, String ddlScript, String dataScript) {
        return "runscript from '" + FilenameUtils.separatorsToUnix(createScript) + "'\\;"
                + "runscript from '" + FilenameUtils.separatorsToUnix(ddlScript) + "'\\;"
                + "runscript from '" + FilenameUtils.separatorsToUnix(dataScript) + "'\\;";
    }

    private String writeScriptToLocalDirectory(String script, String scriptName) {
        ByteArrayResource sourceRes = new ByteArrayResource(script.getBytes());

        return writeScript(scriptName, sourceRes);
    }

    private String writeScriptFromClasspathToLocalDirectory(String scriptName, String scriptDirectory) {
        String sourceScriptPath = scriptDirectory + "/" + scriptName;
        URL resource = this.getClass().getResource(sourceScriptPath);
        if (resource != null) {
            LOG.info("write script from sourceRes [{}]", resource.getPath());
        }
        ClassPathResource sourceRes = new ClassPathResource(sourceScriptPath);

        try {
            LOG.info("write script from sourceRes [{}]", sourceRes.getURL());
        } catch (IOException e) {
            LOG.error("resource not found [{}]", sourceScriptPath);
        }
        return writeScript(scriptName, sourceRes);
    }

    private static String writeScript(String scriptName, InputStreamSource sourceRes) {

        final File testSqlScriptsDirectory = new File("target/test-sql-scripts");
        final File domibusScript = new File(testSqlScriptsDirectory, scriptName);
        String scriptFullPath = null;
        try {
            scriptFullPath = domibusScript.getCanonicalPath();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            fail("Could not get the full path for script [" + domibusScript + "]");
        }

        try (InputStream inputStream = sourceRes.getInputStream()) {
            LOG.debug("Database: Writing file [{}]", domibusScript);
            final byte[] data = IOUtils.toByteArray(inputStream);
            FileUtils.writeByteArrayToFile(domibusScript, data);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            fail("Could not write script from resource [" + sourceRes + "] to the local file [" + domibusScript + "]");
        }
        return scriptFullPath;
    }


}
