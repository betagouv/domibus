package eu.domibus.core.ebms3.receiver;

import eu.domibus.AbstractIT;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3UserMessage;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.ErrorResult;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.NoMatchingPModeFoundException;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.messaging.XmlProcessingException;
import mockit.*;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import static eu.domibus.common.ErrorCode.EbMS3ErrorCode.EBMS_0010;
import static eu.domibus.messaging.MessageConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class FaultInHandlerIT extends AbstractIT {
    public static final String MESSAGE_ID = "123";

    @Autowired
    private FaultInHandler faultInHandler;

    @Injectable
    private SoapUtil soapUtil;

    @Injectable
    private ErrorLogService errorLogService;

    @Injectable
    private Ebms3Converter ebms3Converter;

    @Injectable
    private BackendNotificationService backendNotificationService;

    @Before
    public void before() throws XmlProcessingException, IOException {
        uploadPmode();

        Deencapsulation.setField(faultInHandler, soapUtil);
        Deencapsulation.setField(faultInHandler, errorLogService);
        Deencapsulation.setField(faultInHandler, ebms3Converter);
        Deencapsulation.setField(faultInHandler, backendNotificationService);
    }

    @Test(expected = MissingResourceException.class)
    public void testHandleFaultNullContext(){
        faultInHandler.handleFault(null);
    }

    @Test
    public void test(@Mocked PhaseInterceptorChain phaseInterceptorChain, @Mocked Message message, @Mocked SOAPMessageContext context, @Mocked Exchange exchange){
        NoMatchingPModeFoundException cause = new NoMatchingPModeFoundException(MESSAGE_ID);
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);

        new Expectations() {{
            phaseInterceptorChain.getCurrentMessage();
            result = message;

            message.getContextualProperty(anyString);
            result = MESSAGE_ID;

            context.get(Exception.class.getName());
            returns(ebms3Exception);

            message.getExchange().get(EMBS3_MESSAGING_OBJECT);
            result = new Ebms3Messaging();

            ebms3Converter.convertFromEbms3((Ebms3UserMessage) any);
            result = new UserMessage();
        }};

        faultInHandler.handleFault(context);

        new Verifications(){{
            SOAPMessage soapMessageWithEbMS3Error;
            context.setMessage(soapMessageWithEbMS3Error = withCapture());
            assertEquals("Incorrect error code", EBMS_0010, ebms3Exception.getErrorCode());

            soapUtil.logRawXmlMessageWhenEbMS3Error(soapMessageWithEbMS3Error);

            errorLogService.createErrorLog((Ebms3Messaging)any, MSHRole.RECEIVING, null);

            Map<String, String> properties;
            backendNotificationService.fillEventProperties((UserMessage) any, properties = withCapture());
            assertEquals(EBMS_0010.name(), properties.get(ERROR_CODE));
            assertTrue(properties.containsKey(ERROR_DETAIL));

            backendNotificationService.notifyMessageReceivedFailure((UserMessage) any, (ErrorResult) any);
        }};
    }
}
