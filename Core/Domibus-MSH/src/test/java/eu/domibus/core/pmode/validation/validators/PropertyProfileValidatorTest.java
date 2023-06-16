package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.model.Messaging;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Property;
import eu.domibus.common.model.configuration.PropertySet;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.UserMessageDefaultServiceHelper;
import eu.domibus.core.message.UserMessageServiceHelper;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.messaging.MessageConstants;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author idragusa
 * @since 4.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class PropertyProfileValidatorTest {

    public static final String valid4CornerMessagePath = "target/test-classes/eu/domibus/common/validators/valid4CornerMessage.xml";

    @Tested
    PropertyProfileValidator propertyProfileValidator;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    UserMessageServiceHelper userMessageDefaultServiceHelper = new UserMessageDefaultServiceHelper();

    @Test
    public void validateTest(@Injectable UserMessage userMessage,
                             @Injectable eu.domibus.api.model.MessageProperty property1,
                             @Injectable eu.domibus.api.model.MessageProperty property2,
                             @Injectable LegConfiguration legConfiguration,
                             @Injectable PropertySet propertySet) throws EbMS3Exception {
        Set<eu.domibus.api.model.MessageProperty> messageProperties = new HashSet<>();
        messageProperties.add(property1);
        messageProperties.add(property2);

        Set<eu.domibus.common.model.configuration.Property> properties = new HashSet<>();
        properties.add(createProperty(MessageConstants.ORIGINAL_SENDER));
        properties.add(createProperty(MessageConstants.FINAL_RECIPIENT));
        new Expectations() {{
            property1.getName();
            result = MessageConstants.ORIGINAL_SENDER;

            property2.getName();
            result = MessageConstants.FINAL_RECIPIENT;

            legConfiguration.getPropertySet();
            result = propertySet;

            propertySet.getProperties();
            result = properties;

            userMessage.getMessageProperties();
            result = messageProperties;
        }};

        propertyProfileValidator.validate(userMessage, "anyKey");
    }

    @Test
    public void validateWithMissingUserMessageProperties(@Injectable UserMessage userMessage,
                             @Injectable eu.domibus.api.model.MessageProperty property1,
                             @Injectable eu.domibus.api.model.MessageProperty property2,
                             @Injectable LegConfiguration legConfiguration,
                             @Injectable PropertySet propertySet) throws EbMS3Exception {

        Set<eu.domibus.common.model.configuration.Property> properties = new HashSet<>();
        properties.add(createProperty(MessageConstants.ORIGINAL_SENDER));
        properties.add(createProperty(MessageConstants.FINAL_RECIPIENT));
        new Expectations() {{
            legConfiguration.getPropertySet();
            result = propertySet;

            propertySet.getProperties();
            result = properties;

            userMessage.getMessageProperties();
            result = null;
        }};

        propertyProfileValidator.validate(userMessage, "anyKey");
    }


    @Test
    public void validateMissingPropertyTest(@Injectable UserMessage userMessage,
                                            @Injectable LegConfiguration legConfiguration,
                                            @Injectable PropertySet propertySet) throws EbMS3Exception {
        String pmodeKey = "anyKey";

        new Expectations() {{
            pModeProvider.getLegConfiguration(pmodeKey);
            result = legConfiguration;

            legConfiguration.getPropertySet();
            result = propertySet;
        }};

        propertyProfileValidator.validate(userMessage, pmodeKey);
    }


    @Test
    public void checkDuplicateMessagePropertiesTest(@Injectable Messaging messaging,
                                                    @Injectable Property profiledProperty,
                                                    @Injectable eu.domibus.api.model.MessageProperty messageProperty,
                                                    @Injectable eu.domibus.api.model.MessageProperty messageProperty1) {
        Set<Property> properties = new HashSet<>();
        properties.add(createProperty(MessageConstants.ORIGINAL_SENDER));
        String duplicateMessageProperty = "originalSender";
        Set<eu.domibus.api.model.MessageProperty> messagePropertiesSet = new HashSet<>();
        messagePropertiesSet.add(messageProperty);
        messagePropertiesSet.add(messageProperty1);
        final List<Property> modifiablePropertyList = new ArrayList<>(properties);

        new Expectations() {{
            messageProperty.getName();
            result = duplicateMessageProperty;

            messageProperty1.getName();
            result = duplicateMessageProperty;
        }};

        try {
            propertyProfileValidator.checkDuplicateMessageProperties(modifiablePropertyList, messagePropertiesSet);
            Assertions.fail();
        } catch (EbMS3Exception e) {
            Assertions.assertEquals(ErrorCode.EbMS3ErrorCode.EBMS_0052, e.getErrorCode());
            Assertions.assertEquals("Duplicate Message property found for property name [originalSender]", e.getMessage());
        }

    }

    private Property createProperty(String name) {
        Property property = new Property();
        property.setName(name);
        property.setRequired(false);
        property.setKey(name);
        property.setDatatype("String");
        return property;
    }
    }
