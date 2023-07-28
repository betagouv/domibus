package eu.domibus.plugin.fs.property;

import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO.Type;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO.Usage;
import eu.domibus.ext.domain.Module;
import eu.domibus.ext.services.DomibusPropertyMetadataManagerExt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ion Perpegel
 * @since 4.1.1
 * <p>
 * Property manager for the Default FS plugin properties.
 */
@Component
public class FSPluginPropertiesMetadataManagerImpl implements DomibusPropertyMetadataManagerExt {

    public static final String DOMAIN_ENABLED = "fsplugin.domain.enabled";

    public static final String LOCATION = "fsplugin.messages.location";

    public static final String SENT_ACTION = "fsplugin.messages.sent.action";

    public static final String SENT_PURGE_WORKER_CRONEXPRESSION = "fsplugin.messages.sent.purge.worker.cronExpression";

    protected static final String SENT_PURGE_EXPIRED = "fsplugin.messages.sent.purge.expired";

    protected static final String FAILED_ACTION = "fsplugin.messages.failed.action";

    public static final String FAILED_PURGE_WORKER_CRONEXPRESSION = "fsplugin.messages.failed.purge.worker.cronExpression";

    protected static final String FAILED_PURGE_EXPIRED = "fsplugin.messages.failed.purge.expired";

    protected static final String RECEIVED_PURGE_EXPIRED = "fsplugin.messages.received.purge.expired";

    public static final String OUT_QUEUE_CONCURRENCY = "fsplugin.send.queue.concurrency";

    protected static final String SEND_DELAY = "fsplugin.messages.send.delay";

    public static final String SEND_WORKER_INTERVAL = "fsplugin.messages.send.worker.repeatInterval";

    public static final String SEND_EXCLUDE_REGEX = "fsplugin.messages.send.exclude.regex";

    public static final String RECEIVED_PURGE_WORKER_CRONEXPRESSION = "fsplugin.messages.received.purge.worker.cronExpression";

    public static final String LOCKS_PURGE_WORKER_CRONEXPRESSION = "fsplugin.messages.locks.purge.worker.cronExpression";

    protected static final String LOCKS_PURGE_EXPIRED = "fsplugin.messages.locks.purge.expired";

    protected static final String USER = "fsplugin.messages.user";

    protected static final String PAYLOAD_ID = "fsplugin.messages.payload.id";

    // Sonar confuses this constant with an actual password
    @SuppressWarnings("squid:S2068")
    protected static final String PASSWORD = "fsplugin.messages.password";

    public static final String MESSAGE_NOTIFICATIONS = "fsplugin.messages.notifications";

    protected static final String AUTHENTICATION_USER = "fsplugin.authentication.user";

    // Sonar confuses this constant with an actual password
    @SuppressWarnings("squid:S2068")
    protected static final String AUTHENTICATION_PASSWORD = "fsplugin.authentication.password";

    protected static final String PAYLOAD_SCHEDULE_THRESHOLD = "fsplugin.messages.payload.schedule.threshold";

    public static final String PASSWORD_ENCRYPTION_ACTIVE = "fsplugin.password.encryption.active"; //NOSONAR

    public static final String FSPLUGIN_PASSWORD_ENCRYPTION_PROPERTIES = "fsplugin.password.encryption.properties"; //NOSONAR

    public static final String OUT_QUEUE = "fsplugin.send.queue";

    Map<String, DomibusPropertyMetadataDTO> knownProperties;

    public FSPluginPropertiesMetadataManagerImpl() {
        createMetadata();
    }

    @Override
    public Map<String, DomibusPropertyMetadataDTO> getKnownProperties() {
        return knownProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasKnownProperty(String name) {
        return this.getKnownProperties().containsKey(name);
    }

    protected void createMetadata() {
        knownProperties = Arrays.stream(new DomibusPropertyMetadataDTO[]{
                        //non-writable properties:
                        new DomibusPropertyMetadataDTO(PASSWORD_ENCRYPTION_ACTIVE, Type.BOOLEAN, Module.FS_PLUGIN, false, Usage.GLOBAL_AND_DOMAIN, false, true, false, false),
                        new DomibusPropertyMetadataDTO(FSPLUGIN_PASSWORD_ENCRYPTION_PROPERTIES, Type.COMMA_SEPARATED_LIST, Module.FS_PLUGIN, false, Usage.DOMAIN, false, true, false, false),
                        new DomibusPropertyMetadataDTO(OUT_QUEUE, Type.JNDI, Module.FS_PLUGIN, false, Usage.GLOBAL, true, true, false, false),
                        new DomibusPropertyMetadataDTO(SEND_EXCLUDE_REGEX, Type.REGEXP, Module.FS_PLUGIN, false, Usage.DOMAIN, true, true, false, false),

                        //writable properties
                        new DomibusPropertyMetadataDTO(MESSAGE_NOTIFICATIONS, Type.COMMA_SEPARATED_LIST, Module.FS_PLUGIN, Usage.GLOBAL),
                        new DomibusPropertyMetadataDTO(SEND_WORKER_INTERVAL, Type.NUMERIC, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(SENT_PURGE_WORKER_CRONEXPRESSION, Type.CRON, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(FAILED_PURGE_WORKER_CRONEXPRESSION, Type.CRON, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(RECEIVED_PURGE_WORKER_CRONEXPRESSION, Type.CRON, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(LOCKS_PURGE_WORKER_CRONEXPRESSION, Type.CRON, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        // without fallback
                        new DomibusPropertyMetadataDTO(AUTHENTICATION_USER, Module.FS_PLUGIN, Usage.DOMAIN, false),
                        new DomibusPropertyMetadataDTO(AUTHENTICATION_PASSWORD, Type.PASSWORD, Module.FS_PLUGIN, true, Usage.DOMAIN, false, true, true, false),
                        new DomibusPropertyMetadataDTO(USER, Module.FS_PLUGIN, Usage.DOMAIN, false),
                        new DomibusPropertyMetadataDTO(PASSWORD, Type.PASSWORD, Module.FS_PLUGIN, true, Usage.DOMAIN, false, true, true, false),
                        // with fallback - domain like in ST and full domain in MT
                        new DomibusPropertyMetadataDTO(LOCATION, Type.URI, Module.FS_PLUGIN, Usage.DOMAIN, false),
                        new DomibusPropertyMetadataDTO(DOMAIN_ENABLED, Type.BOOLEAN, Module.FS_PLUGIN, true, Usage.DOMAIN, true, true, false, false),
                        new DomibusPropertyMetadataDTO(SEND_DELAY, Type.NUMERIC, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(PAYLOAD_SCHEDULE_THRESHOLD, Type.NUMERIC, Module.FS_PLUGIN, Usage.GLOBAL, true),
                        new DomibusPropertyMetadataDTO(SENT_ACTION, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(FAILED_ACTION, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(SENT_PURGE_EXPIRED, Type.NUMERIC, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(FAILED_PURGE_EXPIRED, Type.NUMERIC, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(RECEIVED_PURGE_EXPIRED, Type.NUMERIC, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(LOCKS_PURGE_EXPIRED, Type.NUMERIC, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(PAYLOAD_ID, Type.URI, Module.FS_PLUGIN, Usage.DOMAIN, true),
                        new DomibusPropertyMetadataDTO(OUT_QUEUE_CONCURRENCY, Type.CONCURRENCY, Module.FS_PLUGIN, Usage.DOMAIN, true),
                })
                .collect(Collectors.toMap(DomibusPropertyMetadataDTO::getName, x -> x));
    }
}
