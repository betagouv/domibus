package eu.domibus.core.payload.persistence.filesystem;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class StorageProviderImplTest {

    @Injectable
    protected PayloadFileStorageFactory storageFactory;

    @Injectable
    protected DomainService domainService;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    Map<Domain, PayloadFileStorage> instances = new HashMap<>();

    @Tested
    PayloadFileStorageProviderImpl storageProvider;

    @Test
    public void init(@Injectable PayloadFileStorage storage) {
        List<Domain> domains = new ArrayList<>();
        final Domain domain = DomainService.DEFAULT_DOMAIN;
        domains.add(domain);

        new Expectations() {{
            domainService.getDomains();
            result = domains;

            storageFactory.create(domain);
            result = storage;
        }};

        storageProvider.initialize();

        new Verifications() {{
            storageFactory.create(domain);
            times = 1;

            instances.put(domain, storage);
        }};
    }

    @Test
    public void forDomain() {
        final Domain domain = DomainService.DEFAULT_DOMAIN;

        storageProvider.forDomain(domain);

        new Verifications() {{
            instances.get(domain);
        }};
    }

    @Test
    public void getCurrentStorage(@Injectable PayloadFileStorage storage) {
        final Domain domain = DomainService.DEFAULT_DOMAIN;

        new Expectations(storageProvider) {{
            domainContextProvider.getCurrentDomainSafely();
            result = domain;

            storageProvider.forDomain(domain);
            result = storage;
        }};

        final PayloadFileStorage currentStorage = storageProvider.getCurrentStorage();
        Assertions.assertEquals(currentStorage, storage);
    }

    @Test
    public void savePayloadsInDatabase(@Injectable PayloadFileStorage storage) {
        new Expectations(storageProvider) {{
            storageProvider.getCurrentStorage();
            result = storage;

            storage.getStorageDirectory();
            result = null;
        }};

        Assertions.assertTrue(storageProvider.isPayloadsPersistenceInDatabaseConfigured());
    }

    @Test
    public void testSavePayloadsInDatabaseWithFileSystemStorage(@Injectable PayloadFileStorage storage,
                                                                @Injectable File file) {
        new Expectations(storageProvider) {{
            storageProvider.getCurrentStorage();
            result = storage;

            storage.getStorageDirectory();
            result = file;

            file.getName();
            result = "/home/storage";
        }};

        Assertions.assertFalse(storageProvider.isPayloadsPersistenceInDatabaseConfigured());
    }
}
