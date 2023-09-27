package eu.domibus.api.cluster;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kochc01
 * @author Cosmin Baciu
 */
public class Command {

    public static final String COMMAND = "COMMAND";
    public static final String DELETE_PMODE_PARTIES = "DELETE_PMODE_PARTIES";
    public static final String DELETE_FINAL_RECIPIENTS_CACHE = "DELETE_FINAL_RECIPIENTS_CACHE";
    public static final String RELOAD_PMODE = "RELOAD_PMODE";
    public static final String EVICT_CACHES = "EVICT_CACHES";
    public static final String EVICT_2LC_CACHES = "EVICT_2LC_CACHES";
    public static final String RELOAD_TRUSTSTORE = "RELOAD_TRUSTSTORE";
    public static final String RELOAD_KEYSTORE = "RELOAD_KEYSTORE";
    public static final String LOGGING_RESET = "LOGGING_RESET";
    public static final String LOGGING_SET_LEVEL = "LOGGING_SET_LEVEL";
    public static final String DOMIBUS_PROPERTY_CHANGE = "DOMIBUS_PROPERTY_CHANGE";
    public static final String MESSAGE_FILTER_UPDATE = "MESSAGE_FILTER_UPDATE";
    public static final String USER_SESSION_INVALIDATION = "USER_SESSION_INVALIDATION";
    public static final String RELOAD_TLS_TRUSTSTORE = "RELOAD_TLS_TRUSTSTORE";
    public static final String DOMAIN_ADDED = "DOMAIN_ADDED";
    public static final String DOMAIN_REMOVED = "DOMAIN_REMOVED";

    private long entityId;
    protected String commandName;
    protected String serverName;
    protected Date creationTime;

    private Map<String, String> commandProperties = new HashMap<>();

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public Map<String, String> getCommandProperties() {
        return commandProperties;
    }

    public void setCommandProperties(Map<String, String> commandProperties) {
        this.commandProperties = commandProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Command command = (Command) o;

        return new EqualsBuilder()
                .append(commandName, command.commandName)
                .append(serverName, command.serverName)
                .append(commandProperties, command.commandProperties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(commandName)
                .append(serverName)
                .append(commandProperties)
                .toHashCode();
    }
}
