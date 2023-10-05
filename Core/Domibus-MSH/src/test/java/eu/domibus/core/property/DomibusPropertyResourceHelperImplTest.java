package eu.domibus.core.property;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.property.*;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.rest.validators.DomibusPropertyValueValidator;
import eu.domibus.core.rest.validators.FieldBlacklistValidator;
import mockit.*;
import org.apache.commons.collections.map.HashedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.core.property.DomibusPropertyResourceHelperImpl.ACCEPTED_CHARACTERS_IN_PROPERTY_NAMES;

@ExtendWith(MockitoExtension.class)
public class DomibusPropertyResourceHelperImplTest {

    @Tested
    DomibusPropertyResourceHelperImpl configurationPropertyResourceHelper;

    @Injectable
    protected DomainContextProvider domainContextProvider;

    @Injectable
    protected DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private AuthUtils authUtils;

    @Mock
    @Injectable
    protected DomainTaskExecutor domainTaskExecutor;

    @Captor
    ArgumentCaptor argCaptor;

    @Injectable
    GlobalPropertyMetadataManager globalPropertyMetadataManager;

    @Injectable
    DomibusPropertyValueValidator domibusPropertyValueValidator;

    @Injectable
    private FieldBlacklistValidator propertyNameBlacklistValidator;

    Map<String, DomibusPropertyMetadata> props1, props2, allProps;
    List<DomibusPropertyMetadata> propertiesMetadataList;

    @BeforeEach
    public void setUp() {
        props1 = Arrays.stream(new DomibusPropertyMetadata[]{
                new DomibusPropertyMetadata(DOMIBUS_UI_TITLE_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, DomibusPropertyMetadata.Usage.DOMAIN, false),
                new DomibusPropertyMetadata(DOMIBUS_PLUGIN_PASSWORD_POLICY_PATTERN, DomibusPropertyMetadata.Usage.DOMAIN, true),
        }).collect(Collectors.toMap(x -> x.getName(), x -> x));

        props2 = Arrays.stream(new DomibusPropertyMetadata[]{
                new DomibusPropertyMetadata(DOMIBUS_UI_SUPPORT_TEAM_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PLUGIN_PASSWORD_POLICY_VALIDATION_MESSAGE, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_DEFAULT_PASSWORD_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_DONT_REUSE_LAST, DomibusPropertyMetadata.Usage.DOMAIN, true),
        }).collect(Collectors.toMap(x -> x.getName(), x -> x));

        allProps = new HashedMap();
        allProps.putAll(props1);
        allProps.putAll(props2);

        propertiesMetadataList = props1.values().stream().collect(Collectors.toList());
    }

    @Test
    public void getAllWritableProperties() {
        String name = "domibus.ui";
        String testValue = "My Domibus value";
        Boolean showDomain = true;

        DomibusPropertiesFilter filter = new DomibusPropertiesFilter();
        filter.setName(name);
        filter.setShowDomain(showDomain);
        filter.setWritable(true);

        List<DomibusProperty> properties = propertiesMetadataList.stream().map(el -> {
            DomibusProperty res = new DomibusProperty();
            res.setMetadata(el);
            res.setValue(testValue);
            return res;
        }).collect(Collectors.toList());

        new Expectations(configurationPropertyResourceHelper) {{
            globalPropertyMetadataManager.getAllProperties();
            result = allProps;
            configurationPropertyResourceHelper.filterProperties(allProps, filter);
            result = propertiesMetadataList;
            configurationPropertyResourceHelper.getPropertyValues(propertiesMetadataList);
            result = properties;
        }};

        filter.setType(testValue);
        List<DomibusProperty> actual = configurationPropertyResourceHelper.getAllProperties(filter);

        Assertions.assertEquals(3, actual.size());
        Assertions.assertEquals(true, actual.stream().anyMatch(el -> el.getMetadata().getName().equals(DOMIBUS_UI_TITLE_NAME)));
        Assertions.assertEquals(testValue, actual.stream().filter(el -> el.getMetadata().getName().equals(DOMIBUS_UI_TITLE_NAME)).findFirst().get().getValue());
    }

    @Test
    public void setPropertyValue() {
        String name = DOMIBUS_UI_TITLE_NAME;
        boolean isDomain = true;
        String value = "propValue";

        new Expectations(configurationPropertyResourceHelper) {{
            configurationPropertyResourceHelper.validateProperty(name, value);
        }};

        configurationPropertyResourceHelper.setPropertyValue(name, isDomain, value);

        new Verifications() {{
            domibusPropertyProvider.setProperty(name, value);
        }};
    }

    @Test
    void setPropertyValueError() {
        String name = DOMIBUS_UI_TITLE_NAME;
        boolean isDomain = false;
        String value = "propValue";

        new Expectations(configurationPropertyResourceHelper) {{
            configurationPropertyResourceHelper.validateProperty(name, value);
            authUtils.isSuperAdmin();
            result = false;
        }};

        Assertions.assertThrows(DomibusPropertyException.class,
                () -> configurationPropertyResourceHelper.setPropertyValue(name, isDomain, value));

        new Verifications() {{
            domibusPropertyProvider.setProperty(name, value);
            times = 0;
        }};
    }

    @Test
    public void setPropertyValueGlobal() throws Exception {
        String name = "some.global.property";
        boolean isDomain = false;
        String value = "propValue";

        new Expectations(configurationPropertyResourceHelper) {{
            configurationPropertyResourceHelper.validateProperty(name, value);
            authUtils.isSuperAdmin();
            result = true;
        }};

        configurationPropertyResourceHelper.setPropertyValue(name, isDomain, value);
        mockExecutorSubmitVoid();

        new Verifications() {{
            domibusPropertyProvider.setProperty(name, value);
            times = 1;
        }};
    }

    @Test
    public void getPropertyValues() {
        new Expectations(configurationPropertyResourceHelper) {{
            domibusPropertyProvider.getProperty(propertiesMetadataList.get(0).getName());
            result = "val1";
        }};

        List<DomibusProperty> actual = configurationPropertyResourceHelper.getPropertyValues(propertiesMetadataList);

        Assertions.assertEquals(3, actual.size());
        Assertions.assertEquals("val1", actual.get(0).getValue());

        new Verifications() {{
            domibusPropertyProvider.getProperty(propertiesMetadataList.get(0).getName());
            domibusPropertyProvider.getProperty(propertiesMetadataList.get(1).getName());
            domibusPropertyProvider.getProperty(propertiesMetadataList.get(2).getName());
        }};
    }

    @Test
    public void getNestedProperties(@Mocked DomibusPropertyMetadata propMeta,
                                    @Mocked DomibusProperty prop1,
                                    @Mocked DomibusProperty prop2,
                                    @Mocked DomibusProperty parentProp) {

        List<String> nestedProps = Arrays.asList("prop1", "prop2");

        new Expectations(configurationPropertyResourceHelper) {{
            propMeta.getName();
            result = "domibus.property.name";
            configurationPropertyResourceHelper.getProperty(propMeta.getName());
            result = parentProp;
            domibusPropertyProvider.getNestedProperties(propMeta.getName());
            result = nestedProps;
            configurationPropertyResourceHelper.getProperty(propMeta.getName() + ".prop1");
            result = prop1;
            configurationPropertyResourceHelper.getProperty(propMeta.getName() + ".prop2");
            result = prop2;
        }};

        List<DomibusProperty> actual = configurationPropertyResourceHelper.getNestedProperties(propMeta);

        Assertions.assertEquals(3, actual.size());
        Assertions.assertEquals(parentProp, actual.get(0));
        Assertions.assertEquals(prop1, actual.get(1));
        Assertions.assertEquals(prop2, actual.get(2));
    }

    @Test
    public void filterProperties() {
        String name = "domibus.ui";
        Boolean showDomain = true;

        DomibusPropertiesFilter filter = new DomibusPropertiesFilter();
        filter.setName(name);
        filter.setShowDomain(showDomain);
        filter.setWritable(true);

        new Expectations(configurationPropertyResourceHelper) {{
            domibusConfigurationService.isMultiTenantAware();
            result = true;
        }};

        List<DomibusPropertyMetadata> actual = configurationPropertyResourceHelper.filterProperties(props1, filter);

        Assertions.assertEquals(1, actual.size());
        Assertions.assertTrue(actual.stream().anyMatch(el -> el.getName().equals(DOMIBUS_UI_TITLE_NAME)));
    }

    @Test
    public void filterPropertiesSingleTenancy() {
        String name = "domibus.ui";
        Boolean showDomain = false;

        DomibusPropertiesFilter filter = new DomibusPropertiesFilter();
        filter.setName(name);
        filter.setShowDomain(showDomain);
        filter.setWritable(true);

        new Expectations(configurationPropertyResourceHelper) {{
            domibusConfigurationService.isMultiTenantAware();
            result = false;
        }};

        List<DomibusPropertyMetadata> actual = configurationPropertyResourceHelper.filterProperties(props1, filter);

        Assertions.assertEquals(1, actual.size());
        Assertions.assertTrue(actual.stream().anyMatch(el -> el.getName().equals(DOMIBUS_UI_TITLE_NAME)));
    }

    @Test
    public void getValueAndCreateProperty(@Mocked DomibusPropertyMetadata propMeta) {
        String propertyValue = "prop value";

        new Expectations(configurationPropertyResourceHelper) {{
            domibusPropertyProvider.getProperty(propMeta.getName());
            result = propertyValue;
        }};

        DomibusProperty actual = configurationPropertyResourceHelper.getValueAndCreateProperty(propMeta);

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(propertyValue, actual.getValue());
    }

    @Test
    public void getPropertyMetadata(@Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";

        new Expectations(configurationPropertyResourceHelper) {{
            globalPropertyMetadataManager.hasKnownProperty(propertyName);
            result = true;
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            result = propMeta;
        }};

        DomibusPropertyMetadata result = configurationPropertyResourceHelper.getPropertyMetadata(propertyName);
        Assertions.assertEquals(propMeta, result);

        new Verifications() {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
        }};
    }

    @Test
    public void getPropertyMetadataComposable(@Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";

        new Expectations(configurationPropertyResourceHelper) {{
            globalPropertyMetadataManager.hasKnownProperty(propertyName);
            result = false;
            globalPropertyMetadataManager.getComposableProperty(propertyName);
            result = propMeta;
        }};

        DomibusPropertyMetadata result = configurationPropertyResourceHelper.getPropertyMetadata(propertyName);
        Assertions.assertEquals(propMeta, result);

        new Verifications() {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            times = 0;
        }};
    }

    @Test
    public void getPropertyMetadataNull(@Mocked DomibusProperty prop, @Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";

        new Expectations() {{
            globalPropertyMetadataManager.hasKnownProperty(propertyName);
            result = false;
            globalPropertyMetadataManager.getComposableProperty(propertyName);
            result = null;
        }};

        DomibusPropertyMetadata result = configurationPropertyResourceHelper.getPropertyMetadata(propertyName);
        Assertions.assertEquals(null, result);

        new Verifications() {{
            globalPropertyMetadataManager.getPropertyMetadata(propertyName);
            times = 0;
        }};
    }

    @Test
    public void validatePropertyName(@Mocked DomibusProperty prop, @Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";

        new Expectations() {{
            propMeta.isComposable();
            result = true;
        }};

        configurationPropertyResourceHelper.validatePropertyName(propMeta, propertyName);

        new Verifications() {{
            propertyNameBlacklistValidator.validate(propertyName, ACCEPTED_CHARACTERS_IN_PROPERTY_NAMES);
        }};
    }

    @Test
    public void validatePropertyNameSkip(@Mocked DomibusProperty prop, @Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";

        new Expectations() {{
            propMeta.isComposable();
            result = false;
        }};

        configurationPropertyResourceHelper.validatePropertyName(propMeta, propertyName);

        new Verifications() {{
            propertyNameBlacklistValidator.validate(propertyName, ACCEPTED_CHARACTERS_IN_PROPERTY_NAMES);
            times = 0;
        }};
    }

    @Test
    public void validatePropertyValue(@Mocked DomibusProperty prop, @Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";
        String propertyValue = "prop value";

        new Expectations(configurationPropertyResourceHelper) {{
            configurationPropertyResourceHelper.createProperty(propMeta, propertyValue, propertyValue);
            result = prop;
        }};

        configurationPropertyResourceHelper.validatePropertyValue(propertyValue, propMeta);

        new Verifications() {{
            domibusPropertyValueValidator.validate(prop);
        }};
    }

    @Test
    void validatePropertyLengthNotOK() {
        String propertyName = "propName";
        String propertyValue = "value too long";
        int maxLength = 10;

        new Expectations(configurationPropertyResourceHelper) {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROPERTY_LENGTH_MAX);
            result = maxLength;
        }};

        Assertions.assertThrows(DomibusPropertyException.class,
                () -> configurationPropertyResourceHelper.validatePropertyLength(propertyName, propertyValue));
    }

    @Test
    public void validatePropertyLengthOK() {
        String propertyName = "propName";
        String propertyValue = "short val";
        int maxLength = 10;

        new Expectations(configurationPropertyResourceHelper) {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROPERTY_LENGTH_MAX);
            result = maxLength;
        }};

        configurationPropertyResourceHelper.validatePropertyLength(propertyName, propertyValue);
    }

    @Test
    public void validatePropertyLengthItself() {
        String propertyName = DOMIBUS_PROPERTY_LENGTH_MAX;
        String propertyValue = "prop value 1";

        configurationPropertyResourceHelper.validatePropertyLength(propertyName, propertyValue);
    }

    @Test
    public void validatePropertyLengthNegativeLength() {
        String propertyName = "propName";
        String propertyValue = "prop value 1";

        new Expectations(configurationPropertyResourceHelper) {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_PROPERTY_LENGTH_MAX);
            result = 0;
        }};

        configurationPropertyResourceHelper.validatePropertyLength(propertyName, propertyValue);
    }

    @Test
    public void validateProperty(@Mocked DomibusProperty prop, @Mocked DomibusPropertyMetadata propMeta) {
        String propertyName = "propName";
        String propertyValue = "prop value";

        new Expectations(configurationPropertyResourceHelper) {{
            configurationPropertyResourceHelper.getPropertyMetadata(propertyName);
            result = propMeta;
            configurationPropertyResourceHelper.validatePropertyMetadata(propertyName, propMeta);
            configurationPropertyResourceHelper.validatePropertyName(propMeta, propertyName);
            configurationPropertyResourceHelper.validatePropertyLength(propertyName, propertyValue);
            configurationPropertyResourceHelper.validatePropertyValue(propertyValue, propMeta);
        }};

        configurationPropertyResourceHelper.validateProperty(propertyName, propertyValue);

        new Verifications() {{
            configurationPropertyResourceHelper.validatePropertyMetadata(propertyName, propMeta);
            configurationPropertyResourceHelper.validatePropertyName(propMeta, propertyName);
            configurationPropertyResourceHelper.validatePropertyLength(propertyName, propertyValue);
            configurationPropertyResourceHelper.validatePropertyValue(propertyValue, propMeta);
            configurationPropertyResourceHelper.validateNumericPropertyMinAndMaxValue(propertyValue, propMeta);
        }};
    }

    @Test
    void validatePropertyMetadataNotWritable(@Mocked DomibusPropertyMetadata prop) {
        String propertyName = "propName";

        new Expectations(configurationPropertyResourceHelper) {{
            prop.isWritable();
            result = false;
        }};

        Assertions.assertThrows(DomibusPropertyException.class,
                () -> configurationPropertyResourceHelper.validatePropertyMetadata(propertyName, prop));
    }

    @Test
    void validatePropertyMetadataComposable(@Mocked DomibusPropertyMetadata prop) {
        String propertyName = "propName";

        new Expectations(configurationPropertyResourceHelper) {{
            prop.isWritable();
            result = true;

            prop.getName();
            result = propertyName;

            prop.isComposable();
            result = true;
        }};

        Assertions.assertThrows(DomibusPropertyException.class,
                () -> configurationPropertyResourceHelper.validatePropertyMetadata(propertyName, prop));
    }

    @Test
    void validatePropertyMetadataNotExistent() {
        String propertyName = "propName";

        Assertions.assertThrows(DomibusPropertyException.class,
                () -> configurationPropertyResourceHelper.validatePropertyMetadata(propertyName, null));
    }

    @Test
    void getPropertyUnknown() {
        String propName = "some.unknown.property";
        new Expectations() {{
            globalPropertyMetadataManager.hasKnownProperty(propName);
            result = false;
        }};

        Assertions.assertThrows(DomibusPropertyException.class, () -> configurationPropertyResourceHelper.getProperty(propName));
    }

    @Test
    public void initSortMap() {
        configurationPropertyResourceHelper.initSortMap();

        Assertions.assertEquals(configurationPropertyResourceHelper.sortingComparatorsMap.size(), 8);
        Assertions.assertTrue(configurationPropertyResourceHelper.sortingComparatorsMap.containsKey(new DomibusPropertyResourceHelperImpl.SortMapKey("name", true)));
        Assertions.assertTrue(configurationPropertyResourceHelper.sortingComparatorsMap.containsKey(new DomibusPropertyResourceHelperImpl.SortMapKey("type", true)));
        Assertions.assertTrue(configurationPropertyResourceHelper.sortingComparatorsMap.containsKey(new DomibusPropertyResourceHelperImpl.SortMapKey("module", true)));
        Assertions.assertTrue(configurationPropertyResourceHelper.sortingComparatorsMap.containsKey(new DomibusPropertyResourceHelperImpl.SortMapKey("usageText", true)));
    }

    private <T> T mockExecutorSubmit() throws Exception {
        Mockito.verify(domainTaskExecutor).submit((Callable) argCaptor.capture());
        Callable<T> callable = (Callable<T>) argCaptor.getValue();
        return callable.call();
    }

    private void mockExecutorSubmitVoid() {
        Mockito.verify(domainTaskExecutor).submit((Runnable) argCaptor.capture());
        Runnable runnable = (Runnable) argCaptor.getValue();
        runnable.run();
    }


    @Test
    public void validatePositiveDecimalMaxValue(@Injectable DomibusPropertyMetadata propMeta) {
        String propValue = "2147483647.99";
        try {
            configurationPropertyResourceHelper.validatePositiveDecimalMaxValue(propValue, propMeta);
        } catch (DomibusPropertyException ex) {
            Assertions.assertEquals(ex.getMessage(), "Invalid property value. The value [2147483647.99] is greater than the maximum decimal value allowed");
        }
    }

    @Test
    public void validatePositiveIntegerMaxValue(@Injectable DomibusPropertyMetadata propMeta) {
        String propValue = "2147483649";
        try {
            configurationPropertyResourceHelper.validatePositiveIntegerMaxValue(propValue, propMeta);
        } catch (DomibusPropertyException ex) {
            Assertions.assertEquals(ex.getMessage(), "Invalid property value. The value [2147483649] is greater than the maximum integer value allowed");
        }
    }

    @Test
    public void validateNumericPropertyValueRange(@Injectable DomibusPropertyMetadata propMeta) {
        String propValue = "-9223372036854775809";

        try {
            configurationPropertyResourceHelper.validateNumericPropertyValueRange(propValue, propMeta);
        } catch (DomibusPropertyException ex) {
            Assertions.assertEquals(ex.getMessage(), "Invalid property value. The value [-9223372036854775809] is not in the long value range");
        }
    }
}
