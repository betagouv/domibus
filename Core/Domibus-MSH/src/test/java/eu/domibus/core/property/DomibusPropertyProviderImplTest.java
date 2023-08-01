package eu.domibus.core.property;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.encryption.PasswordDecryptionService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMAIN_TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ion Perpegel
 */
@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class DomibusPropertyProviderImplTest {

    @Tested
    private DomibusPropertyProviderImpl domibusPropertyProvider;

    @Injectable
    @Qualifier("domibusProperties")
    private Properties domibusProperties;

    @Injectable
    @Qualifier("domibusDefaultProperties")
    private Properties domibusDefaultProperties;

    @Injectable
    private GlobalPropertyMetadataManager globalPropertyMetadataManager;

    @Injectable
    private ConfigurableEnvironment environment;

    @Injectable
    private PropertyProviderDispatcher propertyProviderDispatcher;

    @Injectable
    private PrimitivePropertyTypesManager primitivePropertyTypesManager;

    @Injectable
    private NestedPropertiesManager nestedPropertiesManager;

    @Injectable
    private PropertyProviderHelper propertyProviderHelper;

    @Injectable
    private PasswordDecryptionService passwordDecryptionService;

    @Injectable
    private AnnotationConfigWebApplicationContext rootContext;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private DomibusLocalCacheService domibusLocalCacheService;

    private String propertyName = "domibus.property.name";

    private String propertyValue = "domibus.property.value";

    private Domain domain = new Domain("domain1", "Domain 1");

    @Test
    public void getProperty() {
        new Expectations(domibusPropertyProvider) {{
            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, null);
            result = propertyValue;
        }};

        String result = domibusPropertyProvider.getProperty(propertyName);
        assertEquals(propertyValue, result);

        new Verifications() {{
            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, null);
        }};
    }

    @Test
    public void getPropertyWithDomain() {
        new Expectations(domibusPropertyProvider) {{
            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, domain);
            result = propertyValue;
        }};

        String result = domibusPropertyProvider.getProperty(domain, propertyName);
        assertEquals(propertyValue, result);

        new Verifications() {{
            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, domain);
        }};
    }

    @Test
    public void setProperty() {
        domibusPropertyProvider.setProperty(propertyName, propertyValue);

        new Verifications() {{
            propertyProviderDispatcher.setInternalOrExternalProperty(null, propertyName, propertyValue, true);
        }};
    }

    @Test
    public void setPropertyWithDomain() {
        domibusPropertyProvider.setProperty(domain, propertyName, propertyValue, true);

        new Verifications() {{
            propertyProviderDispatcher.setInternalOrExternalProperty(domain, propertyName, propertyValue, true);
        }};
    }

    @Test
    void getDomainProperty_NullDomain() {
        Assertions.assertThrows(DomibusPropertyException.class, () -> domibusPropertyProvider.getProperty(null, propertyName));
        ;

        new Verifications() {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            times = 0;
        }};
    }

    @Test
    public void getIntegerProperty(@Injectable DomibusPropertyMetadata propMeta) {
        String val = "2";
        Integer intVal = Integer.valueOf(val);
        new Expectations(domibusPropertyProvider) {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = propMeta;
            propMeta.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.POSITIVE_INTEGER;
            domibusPropertyProvider.getProperty(propertyName);
            result = val;
            primitivePropertyTypesManager.getIntegerInternal(propertyName, val);
            result = intVal;
        }};

        Integer res = domibusPropertyProvider.getIntegerProperty(propertyName);

        assertEquals(intVal, res);
    }

    @Test
    public void getLongProperty(@Injectable DomibusPropertyMetadata propMeta) {
        String val = "2";
        Long longVal = Long.valueOf(val);

        new Expectations(domibusPropertyProvider) {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = propMeta;
            propMeta.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.NUMERIC;
            domibusPropertyProvider.getProperty(propertyName);
            result = val;
            primitivePropertyTypesManager.getLongInternal(propertyName, val);
            result = longVal;
        }};

        Long res = domibusPropertyProvider.getLongProperty(propertyName);

        assertEquals(longVal, res);
    }

    @Test
    public void getBooleanProperty(@Injectable DomibusPropertyMetadata propMeta) {
        String val = "true";
        Boolean boolVal = Boolean.valueOf(val);

        new Expectations(domibusPropertyProvider) {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = propMeta;
            propMeta.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.BOOLEAN;
            domibusPropertyProvider.getProperty(propertyName);
            result = val;
            primitivePropertyTypesManager.getBooleanInternal(propertyName, val);
            result = boolVal;
        }};

        Boolean res = domibusPropertyProvider.getBooleanProperty(propertyName);

        assertEquals(boolVal, res);
    }

    @Test
    public void getBooleanDomainProperty(@Injectable DomibusPropertyMetadata propMeta) {
        String val = "true";
        Boolean boolVal = Boolean.valueOf(val);

        new Expectations(domibusPropertyProvider) {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = propMeta;
            propMeta.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.BOOLEAN;
            domibusPropertyProvider.getProperty(domain, propertyName);
            result = val;
            primitivePropertyTypesManager.getBooleanInternal(propertyName, val);
            result = boolVal;
        }};

        Boolean res = domibusPropertyProvider.getBooleanProperty(domain, propertyName);

        assertEquals(boolVal, res);
    }

    @Test
    public void getPropertyValue(@Mocked DomibusPropertyMetadata meta) {
        new Expectations() {{
            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, domain);
            result = propertyValue;
            meta.isEncrypted();
            result = true;
            passwordDecryptionService.isValueEncrypted(anyString);
            result = true;
        }};

        domibusPropertyProvider.getPropertyValue(propertyName, domain);

        new Verifications() {{
            passwordDecryptionService.decryptProperty(domain, propertyName, propertyValue);
        }};
    }

    @Test
    public void getCommaSeparatedPropertyValues_throwsExceptionForPropertiesNotHavingTheCommaSeparatedListType(
            @Injectable DomibusPropertyMetadata domibusPropertyMetadata) {
        // GIVEN
        new Expectations() {{
            domibusPropertyMetadata.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.NUMERIC;

            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = domibusPropertyMetadata;
        }};

        // THEN
        DomibusPropertyException exception = Assertions.assertThrows(
                DomibusPropertyException.class,
                () -> {
                    // WHEN
                    domibusPropertyProvider.getCommaSeparatedPropertyValues(propertyName);
                }, "Should have thrown a DomibusPropertyException when retrieving the comma separated property " +
                        "values for a property not having a comma separated list type");
        Assertions.assertEquals("Should have thrown a DomibusPropertyException indicating the property type is " +
                        "not a comma separated list type",
                "Cannot get the individual parts for property " + propertyName + " because its type "
                        + DomibusPropertyMetadata.Type.NUMERIC + " is not a comma separated list one",
                exception.getMessage());
    }


    @Test
    public void getCommaSeparatedPropertyValues_returnsEmptyListForPropertyHavingTheCommaSeparatedListTypeWhenItsValueIsNull(
            @Injectable DomibusPropertyMetadata domibusPropertyMetadata) {
        final String value = null;

        new Expectations() {{
            domibusPropertyMetadata.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.COMMA_SEPARATED_LIST;

            domibusPropertyMetadata.isEncrypted();
            result = false;

            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = domibusPropertyMetadata;

            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, (Domain) any);
            result = value;
        }};

        // WHEN
        List<String> result = domibusPropertyProvider.getCommaSeparatedPropertyValues(propertyName);

        // THEN
        Assertions.assertEquals(Collections.emptyList(),
                result, "Should have returned an empty list when getting the comma separated property " +
                        "values for a property having a comma separated list type and a null value");
    }

    @Test
    public void getCommaSeparatedPropertyValues_returnsEmptyListForPropertyHavingTheCommaSeparatedListTypeWhenItsValueIsEmpty(
            @Injectable DomibusPropertyMetadata domibusPropertyMetadata) {
        final String value = "";

        new Expectations() {{
            domibusPropertyMetadata.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.COMMA_SEPARATED_LIST;

            domibusPropertyMetadata.isEncrypted();
            result = false;

            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = domibusPropertyMetadata;

            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, (Domain) any);
            result = value;
        }};

        // WHEN
        List<String> result = domibusPropertyProvider.getCommaSeparatedPropertyValues(propertyName);

        // THEN
        Assertions.assertEquals(Collections.emptyList(),
                result, "Should have returned an empty list when getting the comma separated property " +
                        "values for a property having a comma separated list type and an empty value");
    }

    @Test
    public void getCommaSeparatedPropertyValues_returnsEmptyListForPropertyHavingTheCommaSeparatedListTypeWhenItsValueIsBlank(
            @Injectable DomibusPropertyMetadata domibusPropertyMetadata) {
        final String value = "   ";

        new Expectations() {{
            domibusPropertyMetadata.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.COMMA_SEPARATED_LIST;

            domibusPropertyMetadata.isEncrypted();
            result = false;

            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = domibusPropertyMetadata;

            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, (Domain) any);
            result = value;
        }};

        // WHEN
        List<String> result = domibusPropertyProvider.getCommaSeparatedPropertyValues(propertyName);

        // THEN
        Assertions.assertEquals(Collections.emptyList(),
                result, "Should have returned an empty list when getting the comma separated property " +
                        "values for a property having a comma separated list type and a blank value");
    }

    @Test
    public void getCommaSeparatedPropertyValues_returnsListContainingAllIndividualElementsForPropertyHavingTheCommaSeparatedListTypeWhenItsValueIsValid(
            @Injectable DomibusPropertyMetadata domibusPropertyMetadata) {
        final String value = "value1,value2,value3,,value4, value5 ,";

        new Expectations() {{
            domibusPropertyMetadata.getTypeAsEnum();
            result = DomibusPropertyMetadata.Type.COMMA_SEPARATED_LIST;

            domibusPropertyMetadata.isEncrypted();
            result = false;

            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = domibusPropertyMetadata;

            propertyProviderDispatcher.getInternalOrExternalProperty(propertyName, (Domain) any);
            result = value;
        }};

        // WHEN
        List<String> result = domibusPropertyProvider.getCommaSeparatedPropertyValues(propertyName);

        // THEN
        Assertions.assertEquals(Arrays.asList("value1", "value2", "value3", "value4", "value5"),
                result, "Should have returned a list containing all the individual elements when getting " +
                        "the comma separated property values for a property having a comma separated list type and a valid value");
    }

    @Test
    public void getDomainTitle(@Injectable Domain domain) {
        String domainTitle = StringUtils.repeat("X", 51);
        new Expectations(domibusPropertyProvider) {{
            domibusPropertyProvider.getProperty(domain, DOMAIN_TITLE);
            result = domainTitle;
        }};
        try {
            domibusPropertyProvider.getDomainTitle(domain);
            Assertions.fail();
        } catch (DomibusPropertyException propertyException) {
            Assertions.assertTrue(propertyException.getMessage().contains("Cannot change domain name to [XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX] because it is greater than the maximum allowed length [50]"));
        }
    }
}