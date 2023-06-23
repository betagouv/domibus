package eu.domibus.ext.services;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;

import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)

@Disabled("EDELIVERY-6896")
public class DomibusPropertyExtServiceDelegateAbstractTest {

    public static final String KEY_1 = "key1";
    public static final String KEY_2 = "key2";
    @Tested
    protected DomibusPropertyExtServiceDelegateAbstract domibusPropertyExtServiceDelegateAbstract;

    @Injectable
    protected DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    protected DomainExtService domainExtService;

    @Injectable
    protected DomibusConfigurationExtService domibusConfigurationExtService;

    Map<String, DomibusPropertyMetadataDTO> props = Stream.of(new String[][]{
            {KEY_1, "value1"},
            {KEY_2, "value2"},
    }).collect(Collectors.collectingAndThen(
            Collectors.toMap(data -> data[0], data -> new DomibusPropertyMetadataDTO(data[1])),
            Collections::<String, DomibusPropertyMetadataDTO>unmodifiableMap));
    ;


    @Test
    @Disabled("EDELIVERY-6896")
    public void getKnownPropertyValue_global() {
        String propValue = "propValue";
        props.get(KEY_1).setStoredGlobally(true);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domibusPropertyExtService.getProperty(KEY_1);
            result = propValue;
        }};

        String result = domibusPropertyExtServiceDelegateAbstract.getKnownPropertyValue(KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    public void getKnownPropertyValue_local(

            @Mocked DomibusPropertyMetadataDTO propMeta) {
        String propValue = "propValue";
        props.get(KEY_1).setStoredGlobally(false);

        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domibusPropertyExtServiceDelegateAbstract.onGetLocalPropertyValue(KEY_1);
            result = propValue;
        }};

        String result = domibusPropertyExtServiceDelegateAbstract.getKnownPropertyValue(KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    public void getKnownIntegerPropertyValue_global() {
        Integer propValue = 1;
        props.get(KEY_1).setStoredGlobally(true);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domibusPropertyExtService.getIntegerProperty(KEY_1);
            result = propValue;
        }};

        Integer result = domibusPropertyExtServiceDelegateAbstract.getKnownIntegerPropertyValue(KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getKnownIntegerPropertyValue_local(

            @Mocked DomibusPropertyMetadataDTO propMeta) {
        Integer propValue = 1;
        props.get(KEY_1).setStoredGlobally(false);

        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domibusPropertyExtServiceDelegateAbstract.onGetLocalIntegerPropertyValue(KEY_1, propMeta);
            result = propValue;
        }};

        Integer result = domibusPropertyExtServiceDelegateAbstract.getKnownIntegerPropertyValue(KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    public void getKnownBooleanPropertyValue_local() {
        boolean propValue = true;

        props.get(KEY_1).setStoredGlobally(false);

        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;

            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;

            domibusPropertyExtServiceDelegateAbstract.onGetLocalBooleanPropertyValue(KEY_1, (DomibusPropertyMetadataDTO) any);
            result = propValue;
        }};

        boolean result = domibusPropertyExtServiceDelegateAbstract.getKnownBooleanPropertyValue(KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    public void getKnownBooleanPropertyValue_global() {
        boolean propValue = true;
        props.get(KEY_1).setStoredGlobally(propValue);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;

            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;

            domibusPropertyExtService.getBooleanProperty(KEY_1);
            result = propValue;
        }};

        boolean result = domibusPropertyExtServiceDelegateAbstract.getKnownBooleanPropertyValue(KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void setKnownPropertyValue_global(
            @Mocked DomibusPropertyMetadataDTO propMeta,
            @Mocked DomainDTO domain) {
        String propertyValue = "propValue";
        String domainCode = "domainCode";
        String propertyName = "propertyName";
        boolean broadcast = true;

        props.get(KEY_1).setStoredGlobally(true);

        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domainExtService.getDomain(domainCode);
            result = domain;
        }};

        domibusPropertyExtServiceDelegateAbstract.setKnownPropertyValue(domainCode, propertyName, propertyValue, broadcast);

        new Verifications() {{
            domainExtService.getDomain(domainCode);
            domibusPropertyExtService.setProperty(domain, propertyName, propertyValue, broadcast);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void setKnownPropertyValue_local() {
        String propertyValue = "propValue";
        String domainCode = "domainCode";
        String propertyName = "propertyName";
        boolean broadcast = true;
        props.get(KEY_1).setStoredGlobally(false);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
        }};

        domibusPropertyExtServiceDelegateAbstract.setKnownPropertyValue(domainCode, propertyName, propertyValue, broadcast);

        new Verifications() {{
            domibusPropertyExtServiceDelegateAbstract.onSetLocalPropertyValue(domainCode, propertyName, propertyValue, broadcast);
            domainExtService.getDomain(domainCode);
            times = 0;
        }};
    }

    @Test
    public void setKnownPropertyValue2_global() {
        String propertyValue = "propValue";
        String propertyName = "propertyName";
        props.get(KEY_1).setStoredGlobally(true);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
        }};

        domibusPropertyExtServiceDelegateAbstract.setKnownPropertyValue(KEY_1, propertyValue);

        new Verifications() {{
            domibusPropertyExtService.setProperty(KEY_1, propertyValue);
        }};
    }

    @Test
    public void setKnownPropertyValue2_local() {
        String propertyValue = "propValue";
        String propertyName = "propertyName";
        props.get(KEY_1).setStoredGlobally(false);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
        }};

        domibusPropertyExtServiceDelegateAbstract.setKnownPropertyValue(KEY_1, propertyValue);

        new Verifications() {{
            domibusPropertyExtServiceDelegateAbstract.onSetLocalPropertyValue(KEY_1, propertyValue);
            domibusPropertyExtService.setProperty(KEY_1, propertyValue);
            times = 0;
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getKnownPropertyValue_domain_global(@Mocked DomainDTO domain,

                                                    @Mocked DomibusPropertyMetadataDTO propMeta) {
        String propValue = "propValue";
        String domainCode = "domainCode";
        props.get(KEY_1).setStoredGlobally(true);

        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domainExtService.getDomain(domainCode);
            result = domain;
            domibusPropertyExtService.getProperty(domain, KEY_1);
            result = propValue;
        }};

        String result = domibusPropertyExtServiceDelegateAbstract.getKnownPropertyValue(domainCode, KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    public void getKnownPropertyValue_domain_local() {
        String propValue = "propValue";
        String domainCode = "domainCode";
        props.get(KEY_1).setStoredGlobally(false);
        new Expectations(domibusPropertyExtServiceDelegateAbstract) {{
            domibusPropertyExtServiceDelegateAbstract.hasKnownProperty(KEY_1);
            result = true;
            domibusPropertyExtServiceDelegateAbstract.getKnownProperties();
            result = props;
            domibusPropertyExtServiceDelegateAbstract.onGetLocalPropertyValue(domainCode, KEY_1);
            result = propValue;
        }};

        String result = domibusPropertyExtServiceDelegateAbstract.getKnownPropertyValue(domainCode, KEY_1);

        assertEquals(propValue, result);
    }

    @Test
    public void onGetLocalPropertyValue_domain() {
        String result = domibusPropertyExtServiceDelegateAbstract.onGetLocalPropertyValue("domain", "property");
        assertNull(result);
    }

    @Test
    public void onGetLocalIntegerPropertyValue(@Mocked DomibusPropertyMetadataDTO propMeta) {
        String property = "";
        Integer integer = domibusPropertyExtServiceDelegateAbstract.onGetLocalIntegerPropertyValue(property, propMeta);
        assertEquals(Integer.valueOf(0), integer);
    }

    @Test
    public void onGetLocalBooleanPropertyValue(@Mocked DomibusPropertyMetadataDTO propMeta) {
        String property = "";
        Boolean b = domibusPropertyExtServiceDelegateAbstract.onGetLocalBooleanPropertyValue(property, propMeta);
        assertNull(b);
    }

    @Test
    public void onGetLocalPropertyValue() {
        String result = domibusPropertyExtServiceDelegateAbstract.onGetLocalPropertyValue("property");
        assertNull(result);
    }

}
