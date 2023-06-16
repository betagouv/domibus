package eu.domibus.property;

import eu.domibus.AbstractIT;
import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.property.GlobalPropertyMetadataManager;
import eu.domibus.core.property.PropertyChangeManager;
import eu.domibus.core.property.PropertyProviderDispatcher;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.core.property.PropertyChangeManager.PROPERTY_VALUE_DELIMITER;
import static eu.domibus.property.ExternalTestModulePropertyManager.*;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
public class DomibusPropertyProviderIT extends AbstractIT {

    @Autowired
    org.springframework.cache.CacheManager cacheManager;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    PropertyProviderDispatcher propertyProviderDispatcher;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    GlobalPropertyMetadataManager globalPropertyMetadataManager;

    @Autowired
    DomibusConfigurationService domibusConfigurationService;

    @Autowired
    PropertyChangeManager propertyChangeManager;

    Domain defaultDomain = new Domain("default", "Default");
    File propertyFile;
    List<String> originalContent;

    @BeforeEach
    public void prepare() {
        cacheManager.getCache(DomibusLocalCacheService.DOMIBUS_PROPERTY_CACHE).clear();
        domainContextProvider.setCurrentDomain(defaultDomain);

        propertyFile = getPropertyFile();
        try {
            originalContent = Files.readAllLines(propertyFile.toPath());
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    @AfterEach
public void clean() {
        try {
            Files.write(propertyFile.toPath(), originalContent);
        } catch (IOException e) {
            Assertions.fail();
        }
    }

    @Test
    public void testCacheDomain() {
        //test a domain property here
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        //not in cache now
        String cachedValue = getCachedValue(defaultDomain, propertyName);
        //add to cache
        String actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        Assertions.assertNotEquals(actualValue, cachedValue);

        //gets the cached value now
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assertions.assertEquals(actualValue, cachedValue);
    }

    @Test
    public void testCacheNoDomain() {
        //test a global property here
        String propertyName = DOMIBUS_PROPERTY_LENGTH_MAX;

        domainContextProvider.clearCurrentDomain();

        //not in cache now
        String cachedValue = getCachedValue(propertyName);
        //add to cache
        String actualValue = domibusPropertyProvider.getProperty(propertyName);
        Assertions.assertNotEquals(actualValue, cachedValue);

        //gets the cached value now
        cachedValue = getCachedValue(propertyName);
        Assertions.assertEquals(actualValue, cachedValue);
    }

    @Test
    public void testCacheEvictDomainProperty() {
        String propertyName = DOMIBUS_UI_SUPPORT_TEAM_NAME;

        String cachedValue = getCachedValue(defaultDomain, propertyName);
        Assertions.assertNull(cachedValue);
        //add to cache
        String actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        //gets the cached value now
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assertions.assertNotNull(cachedValue);
        Assertions.assertEquals(cachedValue, actualValue);

        String newValue = actualValue + "MODIFIED";
        //evicts from cache
        domibusPropertyProvider.setProperty(defaultDomain, propertyName, newValue);
        //so not in cache
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assertions.assertNull(cachedValue);

        //add to cache again
        actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        //finds it there
        cachedValue = getCachedValue(defaultDomain, propertyName);
        Assertions.assertEquals(newValue, actualValue);
    }

    @Test
    public void testCacheEvictGlobalProperty() {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        String cachedValue = getCachedValue(propertyName);
        Assertions.assertNull(cachedValue);
        //add to cache
        String originalValue = domibusPropertyProvider.getProperty(propertyName);
        String actualValue = originalValue;
        //gets the cached value now
        cachedValue = getCachedValue(propertyName);
        Assertions.assertNotNull(cachedValue);
        Assertions.assertEquals(cachedValue, actualValue);

        String newValue = actualValue + "MODIFIED";
        //evicts from cache
        domibusPropertyProvider.setProperty(propertyName, newValue);
        //so not in cache
        cachedValue = getCachedValue(propertyName);
        Assertions.assertNull(cachedValue);

        //add to cache again
        actualValue = domibusPropertyProvider.getProperty(propertyName);
        //finds it there
        cachedValue = getCachedValue(propertyName);
        Assertions.assertEquals(newValue, actualValue);

        domibusPropertyProvider.setProperty(propertyName, originalValue);
    }

    @Test
    public void getPropertyValue_non_existing() {
        String propName = EXTERNAL_NOT_EXISTENT;
        DomibusPropertyMetadata result = globalPropertyMetadataManager.getPropertyMetadata(propName);
        Assertions.assertEquals(DomibusPropertyMetadata.Usage.ANY.getValue(), result.getUsage());
    }

    @Test
    public void getPropertyValue_existing_storedLocally_notHandled() {
        String propName = EXTERNAL_MODULE_EXISTENT_NOT_HANDLED;
        Domain currentDomain = domainContextProvider.getCurrentDomain();
        String propValue = "true";

        String result = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assertions.assertEquals(null, result);

        domibusPropertyProvider.setProperty(currentDomain, propName, propValue);
        String result2 = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assertions.assertEquals(null, result2);
    }

    @Test
    public void getPropertyValue_existing_storedLocally_handled() {
        String propName = EXTERNAL_MODULE_EXISTENT_LOCALLY_HANDLED;
        Domain currentDomain = domainContextProvider.getCurrentDomain();
        String propValue = propName + ".value";

        String result = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assertions.assertEquals(propValue, result);

        String newValue = "newValue";
        domibusPropertyProvider.setProperty(currentDomain, propName, newValue);
        String result2 = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assertions.assertEquals(newValue, result2);
    }

    @Test
    public void getPropertyValue_existing_storedGlobally() {
        String propName = EXTERNAL_MODULE_EXISTENT_GLOBALLY_HANDLED;
        Domain currentDomain = domainContextProvider.getCurrentDomain();
        String propValue = null;

        String result = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assertions.assertEquals(propValue, result);

        String newValue = "newValue";
        domibusPropertyProvider.setProperty(currentDomain, propName, newValue);
        String result2 = domibusPropertyProvider.getProperty(currentDomain, propName);
        Assertions.assertEquals(newValue, result2);
    }

    @Test
    public void getPropertyWithUTF8SpecialCharacters() throws IOException {

        InputStream input = getClass().getClassLoader().getResourceAsStream("properties/test.properties");
        String utf8String = "Message status change:PL|ąćęłńóżź|ALPHA: α |LATIN SMALL LETTER E WITH ACUTE:ê";

        Properties properties = new Properties();
        properties.load(input);
        String mailSubject = properties.getProperty(DOMIBUS_ALERT_MSG_COMMUNICATION_FAILURE_MAIL_SUBJECT);

        //Default encoding for properties file reading is ISO-8859-1. So we get different value.
        Assertions.assertNotEquals(mailSubject, utf8String);

        Domain currentDomain = domainContextProvider.getCurrentDomain();
        domibusPropertyProvider.setProperty(currentDomain, mailSubject, utf8String);
        String uft8MailSubject = domibusPropertyProvider.getProperty(currentDomain, mailSubject);

        //Domibus property configuration set the encoding to UTF8-8 . So we get same string with utf8 characters.
        Assertions.assertEquals(uft8MailSubject, utf8String);
        input.close();
    }

    @Test
    public void testSetPropertyReplacesOK() throws IOException {
        testSetPropertyWithName(DOMIBUS_UI_TITLE_NAME);
    }

    @Test
    public void testSetPropertyReplacesCommentedOK() throws IOException {
        String propertyName = "domibus.plugin.login.maximum.attempt";

        testSetPropertyWithName(propertyName);
    }

    @Test
    public void testSetPropertyAddsAtTheEndOK() throws IOException {
        String propertyValue = "TeamA";
        domibusPropertyProvider.setProperty(defaultDomain, DOMIBUS_UI_SUPPORT_TEAM_NAME, propertyValue);

        File propertyFile = getPropertyFile();
        List<String> lines = Files.readAllLines(propertyFile.toPath());
        String lastLine = lines.get(lines.size() - 1);
        Assertions.assertEquals("default." + DOMIBUS_UI_SUPPORT_TEAM_NAME + "=" + propertyValue, lastLine);
    }

    @Test
    void testCallInvalidTypeMethod() {
        Assertions.assertThrows(DomibusPropertyException. class,
        () -> domibusPropertyProvider.getBooleanProperty(DOMIBUS_MESSAGE_DOWNLOAD_MAX_SIZE));
    }

    @Test
    void testCallInvalidTypeMethod2() {
        Assertions.assertThrows(DomibusPropertyException. class,() -> domibusPropertyProvider.getIntegerProperty(DOMIBUS_UI_SUPPORT_TEAM_NAME));
    }

    private void testSetPropertyWithName(String propertyName) throws IOException {
        File propertyFile = getPropertyFile();

        String originalValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);
        String actualValue = originalValue;

        String persistedPropValue = findPropertyInFile(propertyName, propertyFile);
        Assertions.assertEquals(actualValue, persistedPropValue);

        String newValue = actualValue + "MODIFIED";
        domibusPropertyProvider.setProperty(defaultDomain, propertyName, newValue);

        actualValue = domibusPropertyProvider.getProperty(defaultDomain, propertyName);

        persistedPropValue = findPropertyInFile(propertyName, propertyFile);
        Assertions.assertEquals(actualValue, persistedPropValue);

        Assertions.assertEquals(newValue, persistedPropValue);

        domibusPropertyProvider.setProperty(defaultDomain, propertyName, originalValue);
    }

    private String findPropertyInFile(String propertyName, File propertyFile) throws IOException {
        Domain currentD = domainContextProvider.getCurrentDomainSafely();
        if (currentD != null) {
            propertyName = currentD.getCode() + "." + propertyName;
        }
        List<String> lines = Files.readAllLines(propertyFile.toPath());
        int lineNr = propertyChangeManager.findLineWithProperty(propertyName, lines);
        if (lineNr >= 0) {
            String persistedProperty = lines.get(lineNr);
            return StringUtils.substringAfter(persistedProperty, PROPERTY_VALUE_DELIMITER);
        }
        return null;
    }

    private File getPropertyFile(Domain domain) {
        String configurationFileName = domibusConfigurationService.getConfigurationFileName(domain);

        Path fullName = Paths.get(domibusConfigurationService.getConfigLocation(), configurationFileName);

        return new File(fullName.toString());
    }

    private File getPropertyFile() {
        return getPropertyFile(defaultDomain);
    }

    private String getCachedValue(Domain domain, String propertyName) {
        String key = propertyProviderDispatcher.getCacheKeyValue(domain, propertyName);
        return cacheManager.getCache(DomibusLocalCacheService.DOMIBUS_PROPERTY_CACHE).get(key, String.class);
    }

    private String getCachedValue(String propertyName) {
        return getCachedValue(null, propertyName);
    }
}
