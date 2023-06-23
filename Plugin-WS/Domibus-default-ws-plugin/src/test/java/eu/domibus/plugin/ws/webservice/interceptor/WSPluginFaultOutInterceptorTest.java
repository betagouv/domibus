package eu.domibus.plugin.ws.webservice.interceptor;

import eu.domibus.plugin.ws.generated.RetrieveMessageFault;
import eu.domibus.plugin.ws.webservice.ErrorCode;
import eu.domibus.plugin.ws.webservice.WebServiceExceptionFactory;
import eu.domibus.plugin.ws.webservice.WebServiceImpl;
import eu.domibus.plugin.ws.webservice.WebServiceOperation;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.UnexpectedRollbackException;

import javax.persistence.OptimisticLockException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Cosmin Baciu
 * @since 4.1.4
 */
@SuppressWarnings({"ThrowableNotThrown", "ResultOfMethodCallIgnored"})
@ExtendWith(JMockitExtension.class)
public class WSPluginFaultOutInterceptorTest {

    @Tested
    private WebServiceFaultOutInterceptor wsPluginFaultOutInterceptor;

    @Injectable
    private WebServiceExceptionFactory webServicePluginExceptionFactory;

    @Test
    @Disabled("EDELIVERY-6896")
    public void handleMessageWithNoException(@Injectable SoapMessage message,
                                             @Injectable SoapFault soapFault) {
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getExceptionContent(message);
            result = null;
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleMessage(message);

        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void handleMessageWithException_forbiddenCode(@Injectable SoapMessage message,
                                                         @Injectable SoapFault soapFault) {
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getExceptionContent(message);
            //result = soapFault throw the exception instead of returning the object
            returns(soapFault, null);
            times = 1;

            wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);
            result = true;
            times = 1;

            soapFault.getCause();
            times = 1;

            message.setContent(Exception.class, any);
            times = 1;

            wsPluginFaultOutInterceptor.getMethodName(message);
            result = "Nope";
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleMessage(message);

        new FullVerifications() {};
    }
    @Test
    @Disabled("EDELIVERY-6896")
    public void handleMessageWithException_UnknownMethod(@Injectable SoapMessage message,
                                                         @Injectable SoapFault soapFault) {
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getExceptionContent(message);
            //result = soapFault throw the exception instead of returning the object
            returns(soapFault, null);
            times = 1;

            wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);
            result = false;
            times = 1;

            wsPluginFaultOutInterceptor.getMethodName(message);
            result = "Nope";
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleMessage(message);

        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void handleMessageWithException_RETRIEVE_MESSAGE(@Injectable SoapMessage message,
                                                         @Injectable SoapFault soapFault) {
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getExceptionContent(message);
            //result = soapFault throw the exception instead of returning the object
            returns(soapFault, null);
            times = 1;

            wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);
            result = false;
            times = 1;

            wsPluginFaultOutInterceptor.getMethodName(message);
            result = WebServiceOperation.RETRIEVE_MESSAGE;
            times = 1;

            wsPluginFaultOutInterceptor.handleRetrieveMessage(message, soapFault);
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleMessage(message);

        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void soapFaultHasForbiddenCode(@Injectable SoapFault soapFault) {
        new Expectations(){{
            soapFault.getCode();
            result = "TEST";
        }};
        boolean result = wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);

        Assertions.assertFalse(result);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void soapFaultHasForbiddenCode_XML_STREAM_EXC(@Injectable SoapFault soapFault) {
        new Expectations(){{
            soapFault.getCode();
            result = "XML_STREAM_EXC";
        }};
        boolean result = wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);

        assertTrue(result);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void soapFaultHasForbiddenCode_XML_WRITE_EXC(@Injectable SoapFault soapFault) {
        new Expectations(){{
            soapFault.getCode();
            result = "XML_WRITE_EXC";
        }};
        boolean result = wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);

        assertTrue(result);
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void handleRetrieveMessage(@Injectable SoapMessage message,
                                      @Injectable UnexpectedRollbackException cause) {
        Exception exception = new Exception(cause);

        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.handleRetrieveMessageUnexpectedRollbackException(message, exception, cause);
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleRetrieveMessage(message, exception);

        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void handleRetrieveMessageUnexpectedRollbackException(@Injectable SoapMessage message,
                                                                 @Injectable Exception exception,
                                                                 @Injectable UnexpectedRollbackException cause) {

        String errorMessage = "customMessage";
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getRetrieveMessageErrorMessage(cause, anyString);
            result = errorMessage;

            exception.getMessage();
            result = "Exception Message";

            exception.getStackTrace();
            result = null;

            exception.getCause();
            result = null;

            exception.getSuppressed();

            webServicePluginExceptionFactory.createFault(ErrorCode.WS_PLUGIN_0001, "Error retrieving message");
            result = WebServiceImpl.WEBSERVICE_OF.createFaultDetail();
        }};

        wsPluginFaultOutInterceptor.handleRetrieveMessageUnexpectedRollbackException(message, exception, cause);

        new FullVerifications() {{
            Fault fault;
            message.setContent(Exception.class, fault = withCapture());

            assertTrue(fault.getCause() instanceof RetrieveMessageFault);
        }};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getRetrieveMessageErrorMessage(@Injectable UnexpectedRollbackException unexpectedRollbackException) {
        String messageId = "123";

        new Expectations() {{
            unexpectedRollbackException.contains(OptimisticLockException.class);
            result = true;
        }};

        String retrieveMessageErrorMessage = wsPluginFaultOutInterceptor.getRetrieveMessageErrorMessage(unexpectedRollbackException, messageId);
        assertTrue(retrieveMessageErrorMessage.contains("An attempt was made to download message"));
        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getMethodName(@Injectable SoapMessage message,
                              @Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        new Expectations() {{
            message.getExchange().getBindingOperationInfo().getOperationInfo();
            result = operationInfo;

            operationInfo.getInputName();
            result = methodName;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertEquals(methodName, result);
        new FullVerifications() {};
    }
    @Test
    @Disabled("EDELIVERY-6896")
    public void getMethodName_exchangeNull(@Injectable SoapMessage message,
                              @Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        new Expectations() {{
            message.getExchange();
            result = null;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertNull(methodName, result);
        new FullVerifications() {};
    }
    @Test
    @Disabled("EDELIVERY-6896")
    public void getMethodName_bindingOperationInfoNull(@Injectable SoapMessage message,
                              @Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        new Expectations() {{
            message.getExchange().getBindingOperationInfo();
            result = null;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertNull(methodName, result);
        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getMethodName_operationInfoNull(@Injectable SoapMessage message,
                              @Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        new Expectations() {{
            message.getExchange().getBindingOperationInfo().getOperationInfo();
            result = null;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertNull(methodName, result);
        new FullVerifications() {};
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getExceptionContent(@Injectable SoapMessage message,
                                    @Injectable Exception exception) {

        new Expectations(){{
            message.getContent(Exception.class);
            //result = soapFault throw the exception instead of returning the object
            returns(exception, null);
            times = 1;
        }};

        Exception exceptionContent = wsPluginFaultOutInterceptor.getExceptionContent(message);

        assertEquals(exception, exceptionContent);
        new FullVerifications(){};
    }
}
