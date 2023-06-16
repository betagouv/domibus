package eu.domibus.core.message.nonrepudiation;

import eu.domibus.api.model.UserMessage;
import eu.domibus.core.ebms3.sender.client.DispatchClientDefaultProvider;
import eu.domibus.core.message.UserMessageContextKeyProvider;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.soap.SOAPMessage;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@ExtendWith(JMockitExtension.class)
public class SaveRawEnvelopeInterceptorTest {

    @Tested
    SaveRawEnvelopeInterceptor saveRawEnvelopeInterceptor;

    @Injectable
    NonRepudiationService nonRepudiationService;

    @Injectable
    protected UserMessageContextKeyProvider userMessageContextKeyProvider;

    @Test
    @Disabled("EDELIVERY-6896")
    public void testHandleMessage(@Mocked SoapMessage message, @Mocked SOAPMessage jaxwsMessage) {
        Long userMessageEntityId = 123L;
        String userMessageId = "456";

        new Expectations() {{
            message.getContent(SOAPMessage.class);
            result = jaxwsMessage;

            message.getExchange().get(UserMessage.MESSAGE_ID_CONTEXT_PROPERTY);
            result = userMessageId;

            message.getExchange().get(UserMessage.USER_MESSAGE_ID_KEY_CONTEXT_PROPERTY);
            result = String.valueOf(userMessageEntityId);

            userMessageContextKeyProvider.getKeyFromTheCurrentMessage(UserMessage.USER_MESSAGE_DUPLICATE_KEY);
            result = "false";
        }};

        saveRawEnvelopeInterceptor.handleMessage(message);

        new Verifications() {{
            nonRepudiationService.saveResponse(jaxwsMessage, userMessageEntityId);
        }};
    }
}
