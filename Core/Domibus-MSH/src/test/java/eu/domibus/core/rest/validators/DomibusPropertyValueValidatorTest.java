package eu.domibus.core.rest.validators;

import eu.domibus.api.property.DomibusProperty;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.property.validators.DomibusPropertyValidator;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_PROPERTY_VALIDATION_ENABLED;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class DomibusPropertyValueValidatorTest {

    @Tested
    DomibusPropertyValueValidator domibusPropertyValueValidator;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    FieldBlacklistValidator fieldBlacklistValidator;

    @Test
    public void shouldUseBlacklistValidatorForStrings(@Mocked DomibusProperty property) {
        new Expectations(domibusPropertyValueValidator) {{
            domibusPropertyValueValidator.getValidator(property.getMetadata());
            result = null;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROPERTY_VALIDATION_ENABLED);
            result = true;
        }};

        domibusPropertyValueValidator.validate(property);

        new Verifications() {{
            fieldBlacklistValidator.validate(property.getValue());
        }};
    }

    @Test
    public void shouldUsePropertyValueValidatorWhenAvailable(@Mocked DomibusProperty property, @Mocked DomibusPropertyValidator validator) {
        new Expectations(domibusPropertyValueValidator) {{
            domibusPropertyValueValidator.getValidator(property.getMetadata());
            result = validator;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROPERTY_VALIDATION_ENABLED);
            result = true;
            validator.isValid(property.getValue());
            result = true;
        }};

        domibusPropertyValueValidator.validate(property);

        new Verifications() {{
            fieldBlacklistValidator.validate(property.getValue());
            times = 0;
        }};
    }

    @Test
    public void shouldNotPerformValidationIfDisabled(@Mocked DomibusProperty property) {
        new Expectations(domibusPropertyValueValidator) {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_PROPERTY_VALIDATION_ENABLED);
            result = false;
        }};

        domibusPropertyValueValidator.validate(property);

        new FullVerifications() {{
        }};
    }

    @Test
    public void testValidatorOfUnknownPropertyType() {
        DomibusPropertyMetadata propertyMetadata = new DomibusPropertyMetadata();
        propertyMetadata.setType("UNKNOWN_TYPE");

        DomibusPropertyValidator validator = domibusPropertyValueValidator.getValidator(propertyMetadata);
        Assertions.assertNull(validator);
    }

    @Test
    public void testValidatorOfKnownPropertyType() {
        DomibusPropertyMetadata propertyMetadata = new DomibusPropertyMetadata();
        propertyMetadata.setType(String.valueOf(DomibusPropertyMetadata.Type.CONCURRENCY));

        DomibusPropertyValidator validator = domibusPropertyValueValidator.getValidator(propertyMetadata);
        Assertions.assertNotNull(validator);
    }

}
