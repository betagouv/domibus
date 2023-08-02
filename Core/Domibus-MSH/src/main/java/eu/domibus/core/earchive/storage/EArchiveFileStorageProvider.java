package eu.domibus.core.earchive.storage;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainsAware;

/**
 * @author François Gautier
 * @since 5.0
 */
public interface EArchiveFileStorageProvider extends DomainsAware {

    void initialize();

    EArchiveFileStorage forDomain(Domain domain);

    EArchiveFileStorage getCurrentStorage();

    void reset(Domain domain);
}
