package eu.domibus.ext.delegate.services.cache;

import eu.domibus.api.cache.CacheService;
import eu.domibus.ext.exceptions.CacheExtServiceException;
import eu.domibus.ext.services.CacheExtService;
import org.springframework.stereotype.Service;

/**
 * Delegate external Cache service to core
 *
 * @author Soumya Chandran
 * @since 5.0
 */
@Service
public class CacheServiceDelegate implements CacheExtService {

    protected CacheService cacheService;

    public CacheServiceDelegate(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void evictCaches() throws CacheExtServiceException {
        cacheService.evictCaches();
    }

    @Override
    public void evict2LCaches() throws CacheExtServiceException {
        cacheService.evict2LCaches();
    }
}