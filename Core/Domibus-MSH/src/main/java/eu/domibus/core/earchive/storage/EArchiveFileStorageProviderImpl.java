package eu.domibus.core.earchive.storage;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author François Gautier
 * @since 5.0
 */
@Service
public class EArchiveFileStorageProviderImpl implements EArchiveFileStorageProvider {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EArchiveFileStorageProviderImpl.class);

    private final EArchiveFileStorageFactory storageFactory;

    private final DomainService domainService;

    private final DomainContextProvider domainContextProvider;

    private final Map<Domain, EArchiveFileStorage> instances = new HashMap<>();

    public EArchiveFileStorageProviderImpl(EArchiveFileStorageFactory storageFactory,
                                           DomainService domainService,
                                           DomainContextProvider domainContextProvider) {
        this.storageFactory = storageFactory;
        this.domainService = domainService;
        this.domainContextProvider = domainContextProvider;
    }

    @Override
    public void initialize() {
        createStorages();
    }

    @Override
    public void onDomainAdded(final Domain domain) {
        createStorage(domain);
    }

    @Override
    public void onDomainRemoved(Domain domain) {
        removeStorage(domain);
    }

    private void removeStorage(Domain domain) {
        if (!instances.containsKey(domain)) {
            LOG.info("No storage to remove for domain [{}]; exiting.", domain);
            return;
        }
        instances.remove(domain);
        LOG.info("eArchiving Storage removed for domain [{}]", domain);
    }

    private void createStorages() {
        for (Domain domain : domainService.getDomains()) {
            createStorage(domain);
        }
    }

    private void createStorage(Domain domain) {
        EArchiveFileStorage instance = storageFactory.create(domain);
        instances.put(domain, instance);
        LOG.info("eArchiving Storage initialized for domain [{}]", domain);
    }

    @Override
    public EArchiveFileStorage forDomain(Domain domain) {
        return instances.get(domain);
    }

    @Override
    public EArchiveFileStorage getCurrentStorage() {
        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        EArchiveFileStorage currentStorage = forDomain(currentDomain);
        LOG.debug("Retrieved eArchiving Storage for domain [{}]", currentDomain);
        if (currentStorage == null) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Could not retrieve eArchiving Storage for domain" + currentDomain + " is null");
        }
        return currentStorage;
    }
}
