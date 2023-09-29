package eu.domibus.core.message.nonrepudiation;

import eu.domibus.api.model.UserMessage;
import eu.domibus.core.message.UserMessageContextKeyProvider;
import eu.domibus.test.common.SoapSampleUtil;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.binding.soap.SoapMessage;
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
    public void testHandleMessage() throws Exception {
        Long userMessageEntityId = 123L;
        String userMessageId = "456";

        SoapMessage message= new SoapSampleUtil().createSoapMessage("SOAPMessage2.xml", "id");


        message.getExchange().put(UserMessage.MESSAGE_ID_CONTEXT_PROPERTY, userMessageId);
        message.getExchange().put(UserMessage.USER_MESSAGE_ID_KEY_CONTEXT_PROPERTY, userMessageEntityId+"");

        new Expectations() {{
            userMessageContextKeyProvider.getKeyFromTheCurrentMessage(UserMessage.USER_MESSAGE_DUPLICATE_KEY);
            result = "false";
        }};

        saveRawEnvelopeInterceptor.handleMessage(message);

        new Verifications() {{
            nonRepudiationService.saveResponse((SOAPMessage) any, userMessageEntityId);
        }};
    }
}
