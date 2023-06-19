package eu.domibus.plugin.ws.webservice;


import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.ws.AbstractBackendWSIT;
import eu.domibus.plugin.ws.LoggerUtil;
import eu.domibus.plugin.ws.generated.SubmitMessageFault;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author idragusa
 * @since 5.0
 */
@Disabled("EDELIVERY-6896")
public class ReceiveInvalidSoapFaultResponseIT extends AbstractBackendWSIT {

    @Autowired
    LoggerUtil loggerUtil;

    @BeforeEach
    public void before(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, XmlProcessingException {
        uploadPmode(wmRuntimeInfo.getHttpPort());
        loggerUtil.addByteArrayOutputStreamAppender();
    }

    @AfterEach
    public void cleanupLogger() {
        loggerUtil.cleanupByteArrayOutputStreamAppender();
    }

    @Test
    @Disabled("[EDELIVERY-8828] WSPLUGIN: tests for rest methods ignored")
    public void testReceiveValidSoapFault() throws SubmitMessageFault {
//        submitMessage(MessageStatus.WAITING_FOR_RETRY, "InvalidSOAPFaultResponse.xml");
        Assertions.assertTrue(loggerUtil.verifyLogging("SOAPFaultException: Invalid SOAP fault content"));
    }
}
