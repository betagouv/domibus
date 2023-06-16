package eu.domibus.core.message.dictionary;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.AgreementRefEntity;
import eu.domibus.api.model.PartProperty;
import eu.domibus.api.multitenancy.DomainService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Transactional
public class AbstractDictionaryServiceIT extends AbstractIT {

    @Autowired
    protected AgreementDictionaryService agreementDictionaryService;

    @Autowired
    protected PartPropertyDictionaryService partPropertyDictionaryService;


    @Test
    public void testFindOrCreateAgreement() throws ExecutionException {
        final String value1 = "value123";
        final String type1 = "type123";

        Callable<AgreementRefEntity> findOrCreateTask = () -> {
            domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
            return agreementDictionaryService.findOrCreateAgreement(value1, type1);
        };

        int nbThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
        List<Callable<AgreementRefEntity>> tasksList = new ArrayList<>();
        for (int i = 0; i < nbThreads; i++) {
            tasksList.add(findOrCreateTask);
        }

        try {
            List<Future<AgreementRefEntity>> results = executor.invokeAll(tasksList);

            AgreementRefEntity entity0 = results.get(0).get();
            Assertions.assertNotNull(entity0);

            for (Future<AgreementRefEntity> result : results) {
                Assertions.assertTrue(result.isDone());

                AgreementRefEntity entity = result.get();
                Assertions.assertEquals(entity0, entity);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testFindOrCreatePartProperty() throws ExecutionException {
        final String name1 = "name123";
        final String value1 = "value123";
        final String type1 = "type123";

        Callable<PartProperty> findOrCreateTask = () -> {
            domainContextProvider.setCurrentDomain(DomainService.DEFAULT_DOMAIN);
            return partPropertyDictionaryService.findOrCreatePartProperty(name1, value1, type1);
        };

        int nbThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
        List<Callable<PartProperty>> tasksList = new ArrayList<>();
        for (int i = 0; i < nbThreads; i++) {
            tasksList.add(findOrCreateTask);
        }

        try {
            List<Future<PartProperty>> results = executor.invokeAll(tasksList);

            PartProperty entity0 = results.get(0).get();
            for (Future<PartProperty> result : results) {
                Assertions.assertTrue(result.isDone());

                PartProperty entity = result.get();
                Assertions.assertEquals(entity0, entity);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
