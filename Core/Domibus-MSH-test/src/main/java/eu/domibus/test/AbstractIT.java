package eu.domibus.test;

import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.model.UserMessageLogDto;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.message.UserMessageDefaultService;
import eu.domibus.core.message.UserMessageDefaultServiceHelper;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.api.proxy.DomibusProxyService;
import eu.domibus.core.spring.DomibusApplicationContextListener;
import eu.domibus.core.spring.DomibusRootConfiguration;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.test.common.DomibusMTTestDatasourceConfiguration;
import eu.domibus.test.common.DomibusTestDatasourceConfiguration;
import eu.domibus.web.spring.DomibusWebConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertyOverrideContextInitializer.class,
        classes = {DomibusRootConfiguration.class, DomibusWebConfiguration.class,
                DomibusMTTestDatasourceConfiguration.class, DomibusTestMocksConfiguration.class})
public abstract class AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractIT.class);

    @Autowired
    protected UserMessageDefaultServiceHelper userMessageDefaultServiceHelper;

    @Autowired
    protected UserMessageDefaultService userMessageDefaultService;

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusProxyService domibusProxyService;

    @Autowired
    DomibusApplicationContextListener domibusApplicationContextListener;

    @Autowired
    protected DomibusConditionUtil domibusConditionUtil;
    @Autowired
    protected ConfigurableEnvironment environment;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    private static boolean springContextInitialized = false;

    @BeforeClass
    public static void init() throws IOException {
        if (springContextInitialized) {
            return;
        }

        final File domibusConfigLocation = new File("target/test-classes");
        String absolutePath = domibusConfigLocation.getAbsolutePath();
        System.setProperty("domibus.config.location", absolutePath);

        final File projectRoot = new File("").getAbsoluteFile().getParentFile();

        copyActiveMQFile(domibusConfigLocation, projectRoot);
        copyKeystores(domibusConfigLocation, projectRoot);
        copyPolicies(domibusConfigLocation, projectRoot);
        copyDomibusProperties(domibusConfigLocation, projectRoot);

        FileUtils.deleteDirectory(new File("target/temp"));

        //we are using randomly available port in order to allow run in parallel
        int activeMQConnectorPort = SocketUtils.findAvailableTcpPort(2000, 3100);
        int activeMQBrokerPort = SocketUtils.findAvailableTcpPort(61616, 62690);
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_CONNECTOR_PORT, String.valueOf(activeMQConnectorPort)); // see EDELIVERY-10294 and check if this can be removed
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_TRANSPORT_CONNECTOR_URI, "vm://localhost:" + activeMQBrokerPort + "?broker.persistent=false&create=false"); // see EDELIVERY-10294 and check if this can be removed
        LOG.info("activeMQBrokerPort=[{}]", activeMQBrokerPort);
    }

    @Before
    public void initInstance() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test_user",
                        "test_password",
                        Collections.singleton(new SimpleGrantedAuthority(eu.domibus.api.security.AuthRole.ROLE_ADMIN.name()))));

        domibusConditionUtil.waitUntilDatabaseIsInitialized();

        if (!springContextInitialized) {
            LOG.info("Executing the ApplicationContextListener initialization");
            try {
                domibusApplicationContextListener.doInitialize();
            } catch (Exception ex) {
                LOG.warn("Domibus Application Context initialization failed", ex);
            } finally {
                springContextInitialized = true;
            }
        }
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
    }

    private static void copyPolicies(File domibusConfigLocation, File projectRoot) throws IOException {
        final File policiesDirectory = new File(projectRoot, "../Core/Domibus-MSH/src/main/conf/domibus/policies");
        final File destPoliciesDirectory = new File(domibusConfigLocation, "policies");
        FileUtils.forceMkdir(destPoliciesDirectory);
        FileUtils.copyDirectory(policiesDirectory, destPoliciesDirectory);
    }

    private static void copyDomibusProperties(File domibusConfigLocation, File projectRoot) throws IOException {
        final File domibusPropertiesFile = new File(projectRoot, "../Core/Domibus-MSH-test/src/main/conf/domibus.properties");
        final File destDomibusPropertiesFile = new File(domibusConfigLocation, "domibus.properties");
        FileUtils.copyFile(domibusPropertiesFile, destDomibusPropertiesFile);
    }

    private static void copyKeystores(File domibusConfigLocation, File projectRoot) throws IOException {
        final File keystoresDirectory = new File(projectRoot, "../Tomcat/Domibus-MSH-tomcat/src/test/resources/keystores");
        final File destKeystoresDirectory = new File(domibusConfigLocation, "keystores");
        FileUtils.forceMkdir(destKeystoresDirectory);
        FileUtils.copyDirectory(keystoresDirectory, destKeystoresDirectory);
    }

    private static void copyActiveMQFile(File domibusConfigLocation, File projectRoot) throws IOException {
        final File activeMQFile = new File(projectRoot, "../Tomcat/Domibus-MSH-tomcat/src/main/conf/domibus/internal/activemq.xml");
        final File internalDirectory = new File(domibusConfigLocation, "internal");
        FileUtils.forceMkdir(internalDirectory);
        final File destActiveMQ = new File(internalDirectory, "activemq.xml");
        FileUtils.copyFile(activeMQFile, destActiveMQ);
    }

    public void deleteAllMessages(String... messageIds) {
        List<UserMessageLogDto> allMessages = new ArrayList<>();
        for (String messageId : messageIds) {
            if (StringUtils.isNotBlank(messageId)) {
                UserMessageLog byMessageId = userMessageLogDao.findByMessageId(messageId);
                if (byMessageId != null) {

                    UserMessageLogDto userMessageLogDto = new UserMessageLogDto(byMessageId.getUserMessage().getEntityId(), byMessageId.getUserMessage().getMessageId(), byMessageId.getBackend(), null);
                    userMessageLogDto.setProperties(userMessageDefaultServiceHelper.getProperties(byMessageId.getUserMessage()));
                    allMessages.add(userMessageLogDto);
                } else {
                    LOG.warn("MessageId [{}] not found", messageId);
                }
            }
        }
        if (allMessages.isEmpty()) {
            userMessageDefaultService.deleteMessages(allMessages);
        }
    }
}
