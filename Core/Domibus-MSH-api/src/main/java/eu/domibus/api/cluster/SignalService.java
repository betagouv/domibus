package eu.domibus.api.cluster;

import eu.domibus.api.multitenancy.Domain;

import java.util.List;
import java.util.Map;

/**
 * Interface for signal commands into a cluster configuration
 *
 * @author Catalin Enache
 * @since 4.1
 */
public interface SignalService {

    /**
     * signals trust store to be update on
     * @param domain
     */
    void signalTrustStoreUpdate(Domain domain);

    /**
     * signals PMode update to other servers in the cluster
     */
    void signalPModeUpdate();

    /**
     * Signals deleting of the specified parties from the Pmode parties list and from the responder parties of each process
     */
    void signalDeletePmodeParties(List<String> partyNames);

    /**
     * Signals deleting the specified final recipients from the cache
     */
    void signalDeleteFinalRecipientCache(List<String> finalRecipients);

    /**
     * signals Logging set level to other servers in the cluster
     *
     * @param name
     * @param level
     */
    void  signalLoggingSetLevel(final String name, final String level);

    /**
     * signals Logging reset to other servers in the cluster
     */
    void signalLoggingReset();

    /**
     * signals domibus property changed to other servers in the cluster
     */
    void signalDomibusPropertyChange(String domainCode, String propertyName, String propertyValue);

    void sendMessage(Map<String, String> commandProperties);

    /**
     * Signals the update of the Message Filters
     */
    void signalMessageFiltersUpdated();

    /**
     * Signals the session invalidation for the specified user
     */
    void signalSessionInvalidation(String userName);

    /**
     * Signals the clearing of the Caches
     */
    void signalClearCaches();

    /**
     * Signals the clearing of the Second Level Caches
     */
    void signalClear2LCCaches();

    /**
     * Signals the change of the TLS truststore
     */
    void signalTLSTrustStoreUpdate(Domain currentDomain);

    /**
     * signals that domains were added to other servers in the cluster
     */
    void signalDomainsAdded(String domainCode);

    void signalKeyStoreUpdate(Domain domain);

    void signalDomainsRemoved(String domainCode);
}
