package eu.domibus.core.earchive.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.earchive.DomibusEArchiveException;
import eu.domibus.api.earchive.EArchiveBatchStatus;
import eu.domibus.api.earchive.EArchiveRequestType;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.earchive.EArchiveBatchEntity;
import eu.domibus.core.earchive.EArchiveBatchUserMessage;
import eu.domibus.core.earchive.EArchiveBatchUtils;
import eu.domibus.core.earchive.EArchivingDefaultService;
import eu.domibus.core.proxy.DomibusProxy;
import eu.domibus.core.proxy.DomibusProxyService;
import eu.domibus.core.util.JmsUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.messaging.MessageConstants;
import gen.eu.domibus.archive.client.api.ArchiveWebhookApi;
import gen.eu.domibus.archive.client.invoker.ApiClient;
import gen.eu.domibus.archive.client.model.BatchNotification;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * @author François Gautier
 * @since 5.0
 */
@Component
public class EArchiveNotificationListener implements MessageListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveNotificationListener.class);

    private final DatabaseUtil databaseUtil;

    private final EArchivingDefaultService eArchiveService;

    private final JmsUtil jmsUtil;

    private final DomibusPropertyProvider domibusPropertyProvider;

    private final DomibusProxyService domibusProxyService;

    private final ObjectMapper objectMapper;

    private ArchiveWebhookApi earchivingClientApi;

    private final EArchiveBatchUtils eArchiveBatchUtils;

    private Object earchivingClientApiLock = new Object();

    public EArchiveNotificationListener(
            DatabaseUtil databaseUtil,
            EArchivingDefaultService eArchiveService,
            JmsUtil jmsUtil,
            DomibusPropertyProvider domibusPropertyProvider,
            DomibusProxyService domibusProxyService,
            @Qualifier("domibusJsonMapper") ObjectMapper objectMapper,
            EArchiveBatchUtils eArchiveBatchUtils) {
        this.databaseUtil = databaseUtil;
        this.eArchiveService = eArchiveService;
        this.jmsUtil = jmsUtil;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.domibusProxyService = domibusProxyService;
        this.objectMapper = objectMapper;
        this.eArchiveBatchUtils = eArchiveBatchUtils;
    }

    public ArchiveWebhookApi getEarchivingClientApi() {
        if (earchivingClientApi == null) {
            synchronized (earchivingClientApiLock) {
                if (earchivingClientApi == null) {
                    initialize();
                }
            }
        }
        return earchivingClientApi;
    }

    @Override
    public void onMessage(Message message) {
        LOG.putMDC(DomibusLogger.MDC_USER, databaseUtil.getDatabaseUserName());

        String batchId = jmsUtil.getStringPropertySafely(message, MessageConstants.BATCH_ID);
        Long entityId = jmsUtil.getLongPropertySafely(message, MessageConstants.BATCH_ENTITY_ID);
        LOG.putMDC(DomibusLogger.MDC_BATCH_ENTITY_ID, entityId + "");
        if (StringUtils.isBlank(batchId) || entityId == null) {
            LOG.error("Could not get the batchId [{}] and/or entityId [{}]", batchId, entityId);
            return;
        }
        jmsUtil.setDomain(message);

        EArchiveBatchStatus notificationType = EArchiveBatchStatus.valueOf(jmsUtil.getStringPropertySafely(message, MessageConstants.NOTIFICATION_TYPE));

        LOG.info("Notification of type [{}] for batchId [{}] and entityId [{}]", notificationType, batchId, entityId);

        EArchiveBatchEntity eArchiveBatch = eArchiveService.getEArchiveBatch(entityId, true);
        if (notificationType == EArchiveBatchStatus.FAILED) {
            LOG.info("Notification to the eArchive client for batch FAILED [{}] ", eArchiveBatch);
            getEarchivingClientApi().putStaleNotification(buildBatchNotification(eArchiveBatch), batchId);
            LOG.businessInfo(DomibusMessageCode.BUS_ARCHIVE_BATCH_NOTIFICATION_SENT, eArchiveBatch.getBatchId());
        }

        if (notificationType == EArchiveBatchStatus.EXPORTED) {
            LOG.info("Notification to the eArchive client for batch EXPORTED [{}] ", eArchiveBatch);
            getEarchivingClientApi().putExportNotification(buildBatchNotification(eArchiveBatch), batchId);
            LOG.businessInfo(DomibusMessageCode.BUS_ARCHIVE_BATCH_NOTIFICATION_SENT, eArchiveBatch.getBatchId());
        }
    }

    private void initialize() {
        String restUrl = domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_URL);
        if (StringUtils.isBlank(restUrl)) {
            throw new DomibusEArchiveException("eArchive client endpoint not configured");
        }
        LOG.debug("Initializing eArchive client api with endpoint [{}]...", restUrl);

        RestTemplate restTemplate = initRestTemplate();
        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(restUrl);

        earchivingClientApi = new ArchiveWebhookApi();
        earchivingClientApi.setApiClient(apiClient);

        String username = domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_USERNAME);
        String password = domibusPropertyProvider.getProperty(DOMIBUS_EARCHIVE_NOTIFICATION_PASSWORD);
        if (StringUtils.isNotBlank(username)) {
            earchivingClientApi.getApiClient().setUsername(username);
            earchivingClientApi.getApiClient().setPassword(password);
        }
    }

    protected RestTemplate initRestTemplate() {
        int timeout = domibusPropertyProvider.getIntegerProperty(DOMIBUS_EARCHIVE_NOTIFICATION_TIMEOUT);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create().setDefaultRequestConfig(config);

        Boolean useProxy = domibusPropertyProvider.getBooleanProperty(DOMIBUS_EARCHIVE_NOTIFICATION_USEPROXY);
        if (useProxy && domibusProxyService.useProxy()) {
            DomibusProxy domibusProxy = domibusProxyService.getDomibusProxy();

            LOG.debug("Using proxy at [{}:{}] to notify e-archiving client", domibusProxy.getHttpProxyHost(), domibusProxy.getHttpProxyPort());
            clientBuilder.setProxy(new HttpHost(domibusProxy.getHttpProxyHost(), domibusProxy.getHttpProxyPort()));

            if (BooleanUtils.isTrue(domibusProxyService.isProxyUserSet())) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        new AuthScope(domibusProxy.getHttpProxyHost(), domibusProxy.getHttpProxyPort()),
                        new UsernamePasswordCredentials(domibusProxy.getHttpProxyUser(), domibusProxy.getHttpProxyPassword())
                );
                clientBuilder.setDefaultCredentialsProvider(credsProvider);
            }
        }

        CloseableHttpClient client = clientBuilder.build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(client);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        restTemplate.getMessageConverters().add(0, converter);

        return restTemplate;
    }

    protected BatchNotification buildBatchNotification(EArchiveBatchEntity eArchiveBatch) {
        BatchNotification batchNotification = new BatchNotification();
        batchNotification.setBatchId(eArchiveBatch.getBatchId());
        batchNotification.setErrorCode(eArchiveBatch.getErrorCode());
        batchNotification.setErrorDescription(eArchiveBatch.getErrorMessage());
        batchNotification.setStatus(BatchNotification.StatusEnum.valueOf(eArchiveBatch.getEArchiveBatchStatus().name()));
        if (eArchiveBatch.getRequestType() == EArchiveRequestType.CONTINUOUS || eArchiveBatch.getRequestType() == EArchiveRequestType.SANITIZER) {
            batchNotification.setRequestType(BatchNotification.RequestTypeEnum.CONTINUOUS);
        } else if (eArchiveBatch.getRequestType() == EArchiveRequestType.MANUAL) {
            batchNotification.setRequestType(BatchNotification.RequestTypeEnum.MANUAL);
        }
        batchNotification.setTimestamp(OffsetDateTime.ofInstant(eArchiveBatch.getDateRequested().toInstant(), ZoneOffset.UTC));
        setStartDateAndEndDateInNotification(eArchiveBatch, batchNotification);
        batchNotification.setMessages(eArchiveBatch.geteArchiveBatchUserMessages().stream().map(EArchiveBatchUserMessage::getMessageId).collect(Collectors.toList()));

        return batchNotification;
    }

    protected BatchNotification setStartDateAndEndDateInNotification(EArchiveBatchEntity eArchiveBatch, BatchNotification batchNotification) {

        final Boolean isNotificationWithStartAndEndDate = domibusPropertyProvider.getBooleanProperty(DOMIBUS_EARCHIVING_NOTIFICATION_DETAILS_ENABLED);
        if (BooleanUtils.isNotTrue(isNotificationWithStartAndEndDate)) {
            LOG.debug("EArchive client with batch Id [{}] needs to receive notifications without message start date and end date [{}]", eArchiveBatch.getBatchId(), isNotificationWithStartAndEndDate);
            return batchNotification;
        }
        List<EArchiveBatchUserMessage> batchUserMessages = eArchiveBatch.geteArchiveBatchUserMessages();
        Long firstUserMessageEntityId = eArchiveBatchUtils.getMessageStartDate(batchUserMessages, 0);
        Long lastUserMessageEntityId = eArchiveBatchUtils.getMessageStartDate(batchUserMessages, eArchiveBatchUtils.getLastIndex(batchUserMessages));

        Date messageStartDate = eArchiveBatchUtils.getBatchMessageDate(firstUserMessageEntityId);
        Date messageEndDate = eArchiveBatchUtils.getBatchMessageDate(lastUserMessageEntityId);
        if (messageStartDate != null && messageEndDate != null) {
            batchNotification.setMessageStartDate(OffsetDateTime.ofInstant(messageStartDate.toInstant(), ZoneOffset.UTC));
            batchNotification.setMessageEndDate(OffsetDateTime.ofInstant(messageEndDate.toInstant(), ZoneOffset.UTC));
        }
        LOG.debug("EArchive batch messageStartDate [{}] and messageEndDate [{}] for batchId [{}]", messageStartDate, messageEndDate, eArchiveBatch.getBatchId());

        return batchNotification;
    }


}