package eu.domibus.core.ebms3.receiver;

import eu.domibus.AbstractIT;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3UserMessage;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.common.Ebms3ErrorExt;
import eu.domibus.common.ErrorResult;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.NoMatchingPModeFoundException;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.UserMessageExtException;
import eu.domibus.messaging.XmlProcessingException;
import mockit.*;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.jaxws.handler.soap.SOAPMessageContextImpl;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.util.Map;
import java.util.MissingResourceException;

import static eu.domibus.common.ErrorCode.EbMS3ErrorCode.EBMS_0010;
import static eu.domibus.messaging.MessageConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class FaultInHandlerIT extends AbstractIT {
    public static final String MESSAGE_ID = "123";

    @Autowired
    private FaultInHandler faultInHandler;

    @Test
    void testHandleFaultNullContext() {
        Assertions.assertThrows(MissingResourceException.class, () -> faultInHandler.handleFault(null));
    }

    @Test
    public void getEBMS3ExceptionWithUserMessageExtException() {
        UserMessageExtException userMessageExtException = new UserMessageExtException(DomibusErrorCode.DOM_001, "my UserMessageExtException detail");

        Ebms3ErrorExt ebms3Error = new Ebms3ErrorExt();
        final String errorCode = "COS-1";
        final String errorDetail = "COS-1 detail";
        final String myCategory = "myCategory";
        final String mySeverity = "mySeverity";
        final String myOrigin = "myOrigin";
        final String myShortDescription = "myShortDescription";

        ebms3Error.setErrorCode(errorCode);
        ebms3Error.setErrorDetail(errorDetail);
        ebms3Error.setCategory(myCategory);
        ebms3Error.setSeverity(mySeverity);
        ebms3Error.setOrigin(myOrigin);
        ebms3Error.setShortDescription(myShortDescription);

        userMessageExtException.setEbmsError(ebms3Error);
        Exception myException = new Exception(userMessageExtException);
        String messageId = null;
        final EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(myException, messageId);
        assertEquals(errorCode, ebms3Exception.getErrorCode());
        assertEquals(errorDetail, ebms3Exception.getErrorDetail());
        assertEquals(myCategory, ebms3Exception.getCategory());
        assertEquals(mySeverity, ebms3Exception.getSeverity());
        assertEquals(myOrigin, ebms3Exception.getOrigin());
        assertEquals(myShortDescription, ebms3Exception.getShortDescription());
    }

}
