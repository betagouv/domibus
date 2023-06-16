package eu.domibus.core.payload;

import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.PartProperty;
import eu.domibus.api.model.UserMessage;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Payload;
import eu.domibus.common.model.configuration.PayloadProfile;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.pmode.provider.PModeProvider;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static eu.domibus.api.model.Property.MIME_TYPE;
import static eu.domibus.messaging.MessageConstants.COMPRESSION_PROPERTY_KEY;
import static eu.domibus.messaging.MessageConstants.COMPRESSION_PROPERTY_VALUE;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class PayloadProfileValidatorTest {
    private static final String MIME_TYPE_VALUE = "gzip";
    private static final String PMODE_KEY = "pmodeKey";
    private static final String PART_HREF = "cid:message";

    @Tested
    PayloadProfileValidator payloadProfileValidator;

    @Injectable
    PModeProvider pModeProvider;
    UserMessage userMessage;

    public PayloadProfileValidatorTest() {
        userMessage = new UserMessage();
        userMessage.setMessageId("messageId");
    }

    @Test
    void validateCompressPartInfoUnexpectedCompressionType() {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(null);
        }};
        PartInfo partInfo = new PartInfo();
        PartProperty property = new PartProperty();
        partInfo.setPartProperties(Collections.singleton(property));
        property.setName(COMPRESSION_PROPERTY_KEY);
        property.setValue("someOtherValue");

        Assertions.assertThrows(EbMS3Exception.class,
                () -> payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY));
    }

    @Test
    void validateCompressPartInfoMissingMimeType() {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(null);
        }};
        PartInfo partInfo = new PartInfo();
        PartProperty property = new PartProperty();
        partInfo.setPartProperties(Collections.singleton(property));
        property.setName(COMPRESSION_PROPERTY_KEY);
        property.setValue(COMPRESSION_PROPERTY_VALUE);

        Assertions.assertThrows(EbMS3Exception.class,
                () -> payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY));
    }

    @Test
    void validatePayloadProfileNotMatchingCid(@Injectable PayloadProfile payloadProfile) throws EbMS3Exception {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(payloadProfile);

            Payload payload = getPayload("", "");
            payloadProfile.getPayloads();
            result = Collections.singleton(payload);
        }};
        PartInfo partInfo = new PartInfo();
        PartProperty propertyCompression = new PartProperty();
        propertyCompression.setName(COMPRESSION_PROPERTY_KEY);
        propertyCompression.setValue(COMPRESSION_PROPERTY_VALUE);
        PartProperty propertyMimeType = new PartProperty();
        propertyMimeType.setName(MIME_TYPE);
        propertyMimeType.setValue(MIME_TYPE_VALUE);
        partInfo.setPartProperties(new HashSet<>(Arrays.asList(propertyCompression, propertyMimeType)));

        Assertions.assertThrows(EbMS3Exception.class,
                () -> payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY));
    }

    @Test
    void validatePayloadProfileNotMatchingMimeType(@Mocked PayloadProfile payloadProfile) {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(payloadProfile);

            payloadProfile.getPayloads();
            Payload payload = getPayload("", "otherMimeType");
            result = Collections.singleton(payload);
        }};
        PartInfo partInfo = buildPartInfo(PART_HREF);

        Assertions.assertThrows(EbMS3Exception.class,
                () -> payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY));
    }

    private static LegConfiguration getLegConfiguration(PayloadProfile payloadProfile) {
        LegConfiguration legConfiguration = new LegConfiguration();
        legConfiguration.setCompressPayloads(true);
        legConfiguration.setPayloadProfile(payloadProfile);
        return legConfiguration;
    }

    private static PartInfo buildPartInfo(String partHref) {
        PartInfo partInfo = new PartInfo();
        partInfo.setHref(partHref);
        PartProperty propertyCompression = new PartProperty();
        propertyCompression.setName(COMPRESSION_PROPERTY_KEY);
        propertyCompression.setValue(COMPRESSION_PROPERTY_VALUE);
        PartProperty propertyMimeType = new PartProperty();
        propertyMimeType.setName(MIME_TYPE);
        propertyMimeType.setValue(MIME_TYPE_VALUE);
        partInfo.setPartProperties(new HashSet<>(Arrays.asList(propertyCompression, propertyMimeType)));
        return partInfo;
    }

    @Test
    void validatePayloadProfileRequiredLegPayloadIsMissing(@Mocked PayloadProfile payloadProfile) {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(payloadProfile);

            payloadProfile.getPayloads();
            Payload payload = getPayload("mimeTypePayload", MIME_TYPE_VALUE);
            Payload requiredPartInfo = getPayload("requiredPayload", "");
            requiredPartInfo.setRequired(true);
            result = new HashSet<>(Arrays.asList(payload, requiredPartInfo));

        }};
        PartInfo partInfo = buildPartInfo(PART_HREF);

        Assertions.assertThrows(EbMS3Exception.class,
                () -> payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY));
    }

    @Test
    public void validatePayloadProfileNotRequiredLegPayloadIsMissing(@Mocked PayloadProfile payloadProfile) throws EbMS3Exception {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(payloadProfile);

            payloadProfile.getPayloads();
            Payload payload = getPayload("mimeTypePayload", MIME_TYPE_VALUE);
            Payload requiredPartInfo = getPayload("requiredPayload", "");
            requiredPartInfo.setRequired(false);
            result = new HashSet<>(Arrays.asList(payload, requiredPartInfo));

        }};
        PartInfo partInfo = buildPartInfo(PART_HREF);

        payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY);
    }

    @Test
    public void validatePayloadProfileNoPayloadIsMissing(@Mocked PayloadProfile payloadProfile) throws EbMS3Exception {
        new Expectations() {{
            pModeProvider.getLegConfiguration(PMODE_KEY);
            result = getLegConfiguration(payloadProfile);

            payloadProfile.getPayloads();
            Payload payload = getPayload("", MIME_TYPE_VALUE);
            result = Collections.singleton(payload);
        }};
        PartInfo partInfo = buildPartInfo(PART_HREF);

        payloadProfileValidator.validate(userMessage, Collections.singletonList(partInfo), PMODE_KEY);
    }

    private static Payload getPayload(String name, String mimeType) {
        Payload payload = new Payload();
        payload.setName(name);
        payload.setCid(PART_HREF);
        payload.setMimeType(mimeType);
        return payload;
    }

}
