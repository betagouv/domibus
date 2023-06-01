package eu.domibus;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.proxy.DomibusProxyService;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.ConfigurationRaw;
import eu.domibus.core.crypto.TruststoreDao;
import eu.domibus.core.crypto.TruststoreEntity;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.dictionary.StaticDictionaryService;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.core.pmode.ConfigurationRawDAO;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.spring.DomibusApplicationContextListener;
import eu.domibus.core.spring.DomibusRootConfiguration;
import eu.domibus.core.user.ui.UserRoleDao;
import eu.domibus.core.util.WarningUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.common.DomibusMTTestDatasourceConfiguration;
import eu.domibus.web.spring.DomibusWebConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.SocketUtils;
import org.w3c.dom.Document;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.awaitility.Awaitility.with;

/**
 * Created by feriaad on 02/02/2016.
 */
@EnableMethodSecurity
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PropertyOverrideContextInitializer.class,
        classes = {DomibusRootConfiguration.class, DomibusWebConfiguration.class,
                DomibusMTTestDatasourceConfiguration.class, DomibusTestMocksConfiguration.class})
public abstract class AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractIT.class);

    protected static final int SERVICE_PORT = 8892;

    public static final String TEST_PLUGIN_USERNAME = "admin";

    public static final String TEST_PLUGIN_PASSWORD = "123456";
    public ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected ITTestsService itTestsService;

    @Autowired
    protected DomibusApplicationContextListener domibusApplicationContextListener;

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected ConfigurationDAO configurationDAO;

    @Autowired
    protected ConfigurationRawDAO configurationRawDAO;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusProxyService domibusProxyService;

    @Autowired
    protected TruststoreDao truststoreDao;

    @Autowired
    protected UserRoleDao userRoleDao;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    public static boolean springContextInitialized = false;

    @Autowired
    private StaticDictionaryService staticDictionaryService;

    @BeforeClass
    public static void init() throws IOException {
        if (springContextInitialized) {
            return;
        }
        LOG.info(WarningUtil.warnOutput("Initializing Spring context"));

        FileUtils.deleteDirectory(new File("target/temp"));
        System.setProperty("domibus.config.location", new File("target/test-classes").getAbsolutePath());

        //we are using randomly available port in order to allow run in parallel
        int activeMQConnectorPort = SocketUtils.findAvailableTcpPort(2000, 3100);
        int activeMQBrokerPort = SocketUtils.findAvailableTcpPort(61616, 62690);
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_CONNECTOR_PORT, String.valueOf(activeMQConnectorPort)); // see EDELIVERY-10294 and check if this can be removed
        System.setProperty(DomibusPropertyMetadataManagerSPI.ACTIVE_MQ_TRANSPORT_CONNECTOR_URI, "vm://localhost:" + activeMQBrokerPort + "?broker.persistent=false&create=false"); // see EDELIVERY-10294 and check if this can be removed
        LOG.info("activeMQBrokerPort=[{}]", activeMQBrokerPort);
        LOG.info("activeMQConnectorPort=[{}]", activeMQConnectorPort);

        springContextInitialized = true;
    }

    @Before
    public void Init() {
        domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
        waitUntilDatabaseIsInitialized();
        staticDictionaryService.createStaticDictionaryEntries();

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "test_user",
                        "test_password",
                        Collections.singleton(new SimpleGrantedAuthority(eu.domibus.api.security.AuthRole.ROLE_ADMIN.name()))));
    }


    protected void uploadPMode(Integer redHttpPort) throws IOException, XmlProcessingException {
        uploadPMode(redHttpPort, null);
    }

    protected void uploadPMode(Integer redHttpPort, Map<String, String> toReplace) throws IOException, XmlProcessingException {
        uploadPMode(redHttpPort, "dataset/pmode/PModeTemplate.xml", toReplace);
    }

    protected void uploadPMode(Integer redHttpPort, String pModeFilepath, Map<String, String> toReplace) throws IOException, XmlProcessingException {
        final InputStream inputStream = new ClassPathResource(pModeFilepath).getInputStream();

        String pmodeText = IOUtils.toString(inputStream, UTF_8);
        if (toReplace != null) {
            pmodeText = replace(pmodeText, toReplace);
        }
        if (redHttpPort != null) {
            LOG.info("Using wiremock http port [{}]", redHttpPort);
            pmodeText = pmodeText.replace(String.valueOf(SERVICE_PORT), String.valueOf(redHttpPort));
        }

        byte[] bytes = pmodeText.getBytes(UTF_8);
        final Configuration pModeConfiguration = pModeProvider.getPModeConfiguration(bytes);
        configurationDAO.updateConfiguration(pModeConfiguration);
        final ConfigurationRaw configurationRaw = new ConfigurationRaw();
        configurationRaw.setConfigurationDate(Calendar.getInstance().getTime());
        configurationRaw.setXml(bytes);
        configurationRaw.setDescription("upload Pmode for testing on port: " + redHttpPort);
        configurationRawDAO.create(configurationRaw);
        pModeProvider.refresh();
    }

    protected void uploadPMode() throws IOException, XmlProcessingException {
        uploadPMode(null);
    }

    protected UserMessage getUserMessageTemplate() throws IOException {
        Resource userMessageTemplate = new ClassPathResource("dataset/messages/UserMessageTemplate.json");
        String jsonStr = new String(IOUtils.toByteArray(userMessageTemplate.getInputStream()), UTF_8);
        return new ObjectMapper().readValue(jsonStr, UserMessage.class);

    }


    protected void waitUntilDatabaseIsInitialized() {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(120, TimeUnit.SECONDS).until(databaseIsInitialized());
    }

    protected void waitUntilMessageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(5, TimeUnit.SECONDS)
                .until(messageHasStatus(messageId, mshRole, messageStatus));
    }

    protected void waitUntilMessageIsAcknowledged(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.ACKNOWLEDGED);
    }

    protected void waitUntilMessageIsReceived(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.RECEIVING, MessageStatus.RECEIVED);
    }

    protected void waitUntilMessageIsInWaitingForRetry(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.WAITING_FOR_RETRY);
    }

    protected Callable<Boolean> messageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        return () -> {
            domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
            return messageStatus == userMessageLogDao.getMessageStatus(messageId, mshRole);
        };
    }

    protected Callable<Boolean> databaseIsInitialized() {
        return () -> {
            try {
                return userRoleDao.listRoles().size() > 0;
            } catch (Exception e) {
                LOG.error("Could not get the roles list", e);
                return false;
            }
        };
    }

    /**
     * Convert the given file to a string
     */
    protected String getAS4Response(String file) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream is = getClass().getClassLoader().getResourceAsStream("dataset/as4/" + file);
            Document doc = db.parse(is);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception exc) {
            Assert.fail(exc.getMessage());
            exc.printStackTrace();
        }
        return null;
    }

    public void prepareSendMessage(String responseFileName) {
        prepareSendMessage(responseFileName, null);
    }

    public void prepareSendMessage(String responseFileName, Map<String, String> toReplace) {
        String body = getAS4Response(responseFileName);
        if (toReplace != null) {
            body = replace(body, toReplace);
        }

        // Mock the response from the recipient MSH
        stubFor(post(urlEqualTo("/domibus/services/msh"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/soap+xml")
                        .withBody(body)));
    }

    protected String replace(String body, Map<String, String> toReplace) {
        for (String key : toReplace.keySet()) {
            body = body.replaceAll(key, toReplace.get(key));
        }
        return body;
    }

    protected void createStore(String storeName, String filePath) {
        if (truststoreDao.existsWithName(storeName)) {
            LOG.info("truststore already created");
            return;
        }
        LOG.info("create truststore [{}]", storeName);
        try {
            TruststoreEntity domibusTruststoreEntity = new TruststoreEntity();
            domibusTruststoreEntity.setName(storeName);
            domibusTruststoreEntity.setType("JKS");
            domibusTruststoreEntity.setPassword("test123");
            try (InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(filePath)) {
                if (resourceAsStream == null) {
                    throw new IllegalStateException("File not found :" + filePath);
                }
                byte[] trustStoreBytes = IOUtils.toByteArray(resourceAsStream);
                domibusTruststoreEntity.setContent(trustStoreBytes);
                truststoreDao.create(domibusTruststoreEntity);
            }
        } catch (Exception ex) {
            LOG.info("Error creating store entity [{}]", storeName, ex);
        }
    }

    public static MockMultipartFile getMultiPartFile(String originalFilename, InputStream resourceAsStream) throws IOException {
        return new MockMultipartFile("file", originalFilename, "octetstream", IOUtils.toByteArray(resourceAsStream));
    }

    public String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAllMessages(String... messageIds) {
        itTestsService.deleteAllMessages(messageIds);
    }
}
