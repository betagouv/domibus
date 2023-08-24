package eu.domibus.plugin.ws.webservice.interceptor;

import eu.domibus.plugin.ws.generated.RetrieveMessageFault;
import eu.domibus.plugin.ws.webservice.ErrorCode;
import eu.domibus.plugin.ws.webservice.WebServiceExceptionFactory;
import eu.domibus.plugin.ws.webservice.WebServiceImpl;
import eu.domibus.plugin.ws.webservice.WebServiceOperation;
import eu.domibus.test.common.SoapSampleUtil;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.UnexpectedRollbackException;

import javax.persistence.OptimisticLockException;
import javax.xml.namespace.QName;

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
    private final static String messageId = "43bb6883-77d2-4a41-bac4-52a485d50084@domibus.eu";

    private SoapFault soapFault;

    private SoapMessage message;

    @BeforeEach
    void beforeAll() throws Exception {
        soapFault = new SoapFault("Test", QName.valueOf("TEST"));
        message = new SoapSampleUtil().createSoapMessage("SOAPMessage2.xml", messageId);
    }

    @Test
    public void handleMessageWithNoException() {
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getExceptionContent(message);
            result = null;
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleMessage(message);

        new FullVerifications() {
        };
    }

    @Test
    public void handleMessageWithException_forbiddenCode() {
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

        new FullVerifications() {
        };
    }

    @Test
    public void handleMessageWithException_UnknownMethod() {
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

        new FullVerifications() {
        };
    }

    @Test
    public void handleMessageWithException_RETRIEVE_MESSAGE() {
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

        new FullVerifications() {
        };
    }

    @Test
    public void soapFaultHasForbiddenCode(@Injectable org.apache.cxf.common.i18n.Message message) {
        ReflectionTestUtils.setField(soapFault, "message", message);
        new Expectations() {{
            message.getCode();
            result = "TEST";
        }};
        boolean result = wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);

        Assertions.assertFalse(result);
    }

    @Test
    public void soapFaultHasForbiddenCode_XML_STREAM_EXC(@Injectable org.apache.cxf.common.i18n.Message message) {
        ReflectionTestUtils.setField(soapFault, "message", message);
        new Expectations() {{
            message.getCode();
            result = "XML_STREAM_EXC";
        }};
        boolean result = wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);

        assertTrue(result);
    }

    @Test
    public void soapFaultHasForbiddenCode_XML_WRITE_EXC(@Injectable org.apache.cxf.common.i18n.Message message) {
        ReflectionTestUtils.setField(soapFault, "message", message);
        new Expectations() {{
            message.getCode();
            result = "XML_WRITE_EXC";
        }};
        boolean result = wsPluginFaultOutInterceptor.soapFaultHasForbiddenCode(soapFault);

        assertTrue(result);
    }


    @Test
    public void handleRetrieveMessage() {
        UnexpectedRollbackException cause = new UnexpectedRollbackException("TEST");
        Exception exception = new Exception(cause);

        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.handleRetrieveMessageUnexpectedRollbackException(message, exception, cause);
            times = 1;
        }};

        wsPluginFaultOutInterceptor.handleRetrieveMessage(message, exception);

        new FullVerifications() {
        };
    }

    @Test
    public void handleRetrieveMessageUnexpectedRollbackException() {
        UnexpectedRollbackException cause = new UnexpectedRollbackException("TEST");

        Exception exception = new Exception("Exception Message");
        String errorMessage = "customMessage";
        new Expectations(wsPluginFaultOutInterceptor) {{
            wsPluginFaultOutInterceptor.getRetrieveMessageErrorMessage(cause, anyString);
            result = errorMessage;

            webServicePluginExceptionFactory.createFault(ErrorCode.WS_PLUGIN_0001, "Error retrieving message");
            result = WebServiceImpl.WEBSERVICE_OF.createFaultDetail();
        }};

        wsPluginFaultOutInterceptor.handleRetrieveMessageUnexpectedRollbackException(message, exception, cause);

        Exception fault = message.getContent(Exception.class);
        assertTrue(fault.getCause() instanceof RetrieveMessageFault);

        new FullVerifications() {
        };
    }

    @Test
    public void getRetrieveMessageErrorMessage() {
        String messageId = "123";
        UnexpectedRollbackException unexpectedRollbackException = new UnexpectedRollbackException("TEST", new OptimisticLockException());


        String retrieveMessageErrorMessage = wsPluginFaultOutInterceptor.getRetrieveMessageErrorMessage(unexpectedRollbackException, messageId);
        assertTrue(retrieveMessageErrorMessage.contains("An attempt was made to download message"));
        new FullVerifications() {
        };
    }

    @Test
    public void getMethodName(@Injectable BindingOperationInfo bindingOperationInfo, @Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        ExchangeImpl e = new ExchangeImpl();
        message.setExchange(e);
        ReflectionTestUtils.setField(e, "bindingOp", bindingOperationInfo);
        new Expectations() {{
            bindingOperationInfo.getOperationInfo();
            result = operationInfo;

            operationInfo.getInputName();
            result = methodName;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertEquals(methodName, result);
        new FullVerifications() {
        };
    }

    @Test
    public void getMethodName_exchangeNull(@Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        new Expectations() {{
            message.getExchange();
            result = null;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertNull( result);
        new FullVerifications() {
        };
    }

    @Test
    public void getMethodName_bindingOperationInfoNull(@Injectable OperationInfo operationInfo) {
        String methodName = "myMethodName";

        new Expectations() {{
            message.getExchange().getBindingOperationInfo();
            result = null;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertNull( result);
        new FullVerifications() {
        };
    }

    @Test
    public void getMethodName_operationInfoNull(@Injectable BindingOperationInfo bindingOperationInfo) {
        ExchangeImpl e = new ExchangeImpl();
        message.setExchange(e);
        ReflectionTestUtils.setField(e, "bindingOp", bindingOperationInfo);

        new Expectations() {{
            bindingOperationInfo.getOperationInfo();
            result = null;
        }};

        String result = wsPluginFaultOutInterceptor.getMethodName(message);
        Assertions.assertNull(result);
        new FullVerifications() {
        };
    }

    @Test
    public void getExceptionContent() {
        Exception exception = new Exception();

        message.setContent(Exception.class, exception);

        Exception exceptionContent = wsPluginFaultOutInterceptor.getExceptionContent(message);

        assertEquals(exception, exceptionContent);
        new FullVerifications() {
        };
    }
}
