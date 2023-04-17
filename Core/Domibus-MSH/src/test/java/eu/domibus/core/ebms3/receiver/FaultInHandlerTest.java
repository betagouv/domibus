package eu.domibus.core.ebms3.receiver;

import eu.domibus.api.model.MSHRole;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.pmode.NoMatchingPModeFoundException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Test;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import java.security.cert.CertificateException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static eu.domibus.common.ErrorCode.EbMS3ErrorCode.*;
import static eu.domibus.core.ebms3.receiver.FaultInHandler.UNKNOWN_ERROR_OCCURRED;
import static org.apache.wss4j.common.ext.WSSecurityException.ErrorCode.*;
import static org.junit.Assert.assertEquals;

public class FaultInHandlerTest {

    public static final String MESSAGE_ID = "123";
    private FaultInHandler faultInHandler = new FaultInHandler();

    @Test(expected = MissingResourceException.class)
    public void testHandleFaultNullContext(){
        faultInHandler.handleFault(null);
    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsNoMatchingPModeFoundException(){
        NoMatchingPModeFoundException cause = new NoMatchingPModeFoundException(MESSAGE_ID);

        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);

        assertExceptionIsCorrect(new ExpectedFields(EBMS_0010, cause, cause.getMessage()), ebms3Exception);

    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsWebServiceException(){
        String rootCauseMessage = "root cause message";
        Exception rootCause = new Exception(rootCauseMessage);
        WebServiceException cause = new WebServiceException(rootCause);

        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);

        assertExceptionIsCorrect(new ExpectedFields(EBMS_0004, rootCause, rootCauseMessage), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsCertificateException(){
        String message = "some message";
        CertificateException cause = new CertificateException(message);

        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);

        assertExceptionIsCorrect(new ExpectedFields(EBMS_0101, cause, message), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsWSSecurityExceptionFailedCheck(){
        WSSecurityException cause = new WSSecurityException(FAILED_CHECK);
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0102, cause, cause.getMessage()), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsWSSecurityExceptionFailedAuthentication(){
        WSSecurityException cause = new WSSecurityException(FAILED_AUTHENTICATION);
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0101, cause, cause.getMessage()), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsWSSecurityExceptionOther(){
        WSSecurityException cause = new WSSecurityException(FAILURE);
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0103, cause, cause.getMessage()), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenCauseIsUnknownException(){
        Exception cause = new IllegalArgumentException();
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(new Exception(cause), MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0004, cause, UNKNOWN_ERROR_OCCURRED), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenPolicyExceptionWithoutCause(){
        String message = "some message";
        PolicyException exception = new PolicyException(new Message(message, (ResourceBundle) null));
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(exception, MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0103, exception, message), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenSoapFaultWithoutCause(){
        String message = "some message";
        SoapFault exception = new SoapFault(message, new QName(
                XMLConstants.NULL_NS_URI,
                "",
                XMLConstants.DEFAULT_NS_PREFIX));
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(exception, MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0004, exception, message), ebms3Exception);
    }

    @Test
    public void testGetEBMS3ExceptionWhenUnknownExceptionWithoutCause(){
        Exception exception = new IllegalArgumentException();
        EbMS3Exception ebms3Exception = faultInHandler.getEBMS3Exception(exception, MESSAGE_ID);
        assertExceptionIsCorrect(new ExpectedFields(EBMS_0004, null, UNKNOWN_ERROR_OCCURRED), ebms3Exception);
    }

    private static void assertExceptionIsCorrect(ExpectedFields expectedFields, EbMS3Exception ebms3Exception) {
        assertEquals("Incorrect error code", expectedFields.getErrorCode(), ebms3Exception.getErrorCode());
        assertEquals("Incorrect message", expectedFields.getMessage(), ebms3Exception.getMessage());
        assertEquals("Incorrect error detail", expectedFields.getMessage(), ebms3Exception.getErrorDetail());
        assertEquals("Incorrect message id", MESSAGE_ID, ebms3Exception.getRefToMessageId());
        assertEquals("Incorrect exception cause", expectedFields.getCause(), ebms3Exception.getCause());
        assertEquals("Incorrect msh role", MSHRole.RECEIVING, ebms3Exception.getMshRole());
    }

    private static class ExpectedFields {
        private final Exception cause;
        private final ErrorCode.EbMS3ErrorCode errorCode;
        private final String causeMessage;

        public ExpectedFields(ErrorCode.EbMS3ErrorCode errorCode, Exception cause, String causeMessage) {
            this.cause = cause;
            this.errorCode = errorCode;
            this.causeMessage = causeMessage;
        }

        public Exception getCause() {
            return cause;
        }

        public ErrorCode.EbMS3ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return causeMessage;
        }
    }
}