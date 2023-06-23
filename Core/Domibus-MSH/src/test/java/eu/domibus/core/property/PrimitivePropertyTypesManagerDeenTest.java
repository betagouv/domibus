package eu.domibus.core.property;

import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

/**
 * @author Sebastian-Ion TINCU
 */
@ExtendWith(JMockitExtension.class)
public class PrimitivePropertyTypesManagerDeenTest {

    private String customValue;

    private Object myResult;

    private String propertyName = "domibus.property.name";

    private Properties domibusDefaultProperties = new Properties();

    private PrimitivePropertyTypesManager primitivePropertyTypesManager = new PrimitivePropertyTypesManager(domibusDefaultProperties);

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(primitivePropertyTypesManager, "domibusDefaultProperties", domibusDefaultProperties);
    }

    @Test
    void throwsIllegalStateExceptionWhenRetrievingAnIntegerPropertyHavingBothItsCustomValueAndItsDefaultValueMissing() {
        givenMissingCustomValue();
        givenMissingDefaultValue();

        Assertions.assertThrows(IllegalStateException. class,() -> whenRetrievingTheIntegerProperty());
    }

    @Test
    void throwsIllegalStateExceptionWhenRetrievingAnIntegerPropertyHavingItsCustomValueMissingAndItsDefaultValueInvalid() {
        givenMissingCustomValue();
        givenDefaultValue("INVALID_INTEGER_VALUE");

        Assertions.assertThrows(IllegalStateException. class,() -> whenRetrievingTheIntegerProperty());
    }

    @Test
    public void returnsTheDefaultValueWhenRetrievingAnIntegerPropertyHavingItsCustomValueMissingAndItsDefaultValueValid() {
        givenMissingCustomValue();
        givenDefaultValue("42");

        whenRetrievingTheIntegerProperty();

        thenPropertyValueTakenFromDefaults("The integer property value should have been taken from the default properties when the custom value is missing", 42);
    }

    @Test
    public void returnsTheDefaultValueWhenRetrievingAnIntegerPropertyHavingItsCustomValueInvalidAndItsDefaultValueValid() {
        givenCustomValue("INVALID_INTEGER_VALUE");
        givenDefaultValue("-13");

        whenRetrievingTheIntegerProperty();

        thenPropertyValueTakenFromDefaults("The integer property value should have been taken from the default properties when the custom value is invalid", -13);
    }

    @Test
    public void returnsTheCustomValueWhenRetrievingAnIntegerPropertyHavingItsCustomValueValid_IgnoringValidDefaultValue() {
        givenCustomValue("1659");
        givenDefaultValue("0");

        whenRetrievingTheIntegerProperty();

        thenPropertyValueTakenFromDefaults(
                "The integer property value should have been taken from the custom properties when the custom value valid (ignores valid default value)", 1659);
    }

    @Test
    public void returnsTheCustomValueWhenRetrievingAnIntegerPropertyHavingItsCustomValueValid_IgnoringMissingDefaultValue() {
        givenCustomValue("1");
        givenMissingDefaultValue();

        whenRetrievingTheIntegerProperty();

        thenPropertyValueTakenFromDefaults(
                "The integer property value should have been taken from the custom properties when the custom value valid (ignores missing default value)", 1);
    }

    @Test
    public void returnsTheCustomValueWhenRetrievingAnIntegerPropertyHavingItsCustomValueValid_IgnoringInvalidDefaultValue() {
        givenCustomValue("-0712853");
        givenDefaultValue("INVALID_INTEGER_VALUE");

        whenRetrievingTheIntegerProperty();

        thenPropertyValueTakenFromDefaults(
                "The integer property value should have been taken from the custom properties when the custom value valid (ignores invalid default value)", -712853);
    }

    @Test
    void throwsIllegalStateExceptionWhenRetrievingABooleanPropertyHavingBothItsCustomValueAndItsDefaultValueMissing() {
        givenMissingCustomValue();
        givenMissingDefaultValue();

        Assertions.assertThrows(IllegalStateException. class,() -> whenRetrievingTheBooleanProperty());
    }

    @Test
    void throwsIllegalStateExceptionWhenRetrievingABooleanPropertyHavingItsCustomValueMissingAndItsDefaultValueInvalid() {
        givenMissingCustomValue();
        givenDefaultValue("INVALID_BOOLEAN_VALUE");

        Assertions.assertThrows(IllegalStateException. class,() -> whenRetrievingTheBooleanProperty());
    }

    @Test
    public void returnsTheDefaultValueWhenRetrievingABooleanPropertyHavingItsCustomValueMissingAndItsDefaultValueValid() {
        givenMissingCustomValue();
        givenDefaultValue("true");

        whenRetrievingTheBooleanProperty();

        thenPropertyValueTakenFromDefaults("The boolean property value should have been taken from the default properties when the custom value is missing", Boolean.TRUE);
    }

    @Test
    public void returnsTheDefaultValueWhenRetrievingABooleanPropertyHavingItsCustomValueInvalidAndItsDefaultValueValid() {
        givenCustomValue("INVALID_BOOLEAN_VALUE");
        givenDefaultValue("on");

        whenRetrievingTheBooleanProperty();

        thenPropertyValueTakenFromDefaults("The boolean property value should have been taken from the default properties when the custom value is invalid", Boolean.TRUE);
    }

    @Test
    public void returnsTheCustomValueWhenRetrievingAnBooleanPropertyHavingItsCustomValueValid_IgnoringValidDefaultValue() {
        givenCustomValue("T"); // "T" stands for "T[RUE]" so Boolean.TRUE --> check BooleanUtils#toBooleanObject(String)
        givenDefaultValue("0");

        whenRetrievingTheBooleanProperty();

        thenPropertyValueTakenFromDefaults(
                "The boolean property value should have been taken from the custom properties when the custom value valid (ignores valid default value)", true);
    }

    @Test
    public void returnsTheCustomValueWhenRetrievingAnBooleanPropertyHavingItsCustomValueValid_IgnoringMissingDefaultValue() {
        givenCustomValue("no");
        givenMissingDefaultValue();

        whenRetrievingTheBooleanProperty();

        thenPropertyValueTakenFromDefaults(
                "The boolean property value should have been taken from the custom properties when the custom value valid (ignores missing default value)", false);
    }

    @Test
    public void returnsTheCustomValueWhenRetrievingAnBooleanPropertyHavingItsCustomValueValid_IgnoringInvalidDefaultValue() {
        givenCustomValue("off");
        givenDefaultValue("INVALID_BOOLEAN_VALUE");

        whenRetrievingTheBooleanProperty();

        thenPropertyValueTakenFromDefaults(
                "The boolean property value should have been taken from the custom properties when the custom value valid (ignores invalid default value)", false);
    }


    private void givenMissingCustomValue() {
        givenCustomValue(null);
    }

    private void givenCustomValue(String customValue) {
        this.customValue = customValue;
    }

    private void givenMissingDefaultValue() {
        domibusDefaultProperties.remove(propertyName);
    }

    private void givenDefaultValue(String defaultValue) {
        domibusDefaultProperties.put(propertyName, defaultValue);
    }

    private void whenRetrievingTheIntegerProperty() {
        myResult = primitivePropertyTypesManager.getIntegerInternal(propertyName, customValue);
    }

    private void whenRetrievingTheBooleanProperty() {
        myResult = primitivePropertyTypesManager.getBooleanInternal(propertyName, customValue);
    }

    private void thenPropertyValueTakenFromDefaults(String message, Object expectedValue) {
        Assertions.assertEquals(expectedValue, myResult, message);
    }
}
