package eu.domibus.core.ebms3.receiver.interceptor;

import eu.domibus.core.ebms3.receiver.interceptor.DomibusReadyInterceptor;
import eu.domibus.core.status.DomibusStatusService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class DomibusReadyInterceptorTest {

    @Injectable
    private DomibusStatusService domibusStatusService;

    @Tested
    private DomibusReadyInterceptor domibusReadyInterceptor;

    @Test
    void isReady(){
        new Expectations(){{
            domibusStatusService.isNotReady();
            result=true;
        }};
        Assertions.assertThrows(Fault. class,() -> domibusReadyInterceptor.handleMessage(new MessageImpl()));
    }

}
