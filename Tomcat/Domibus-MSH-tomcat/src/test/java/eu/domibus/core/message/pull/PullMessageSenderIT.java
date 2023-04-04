package eu.domibus.core.message.pull;

import eu.domibus.AbstractIT;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3SignalMessage;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.messaging.XmlProcessingException;
import mockit.*;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.MessageId;
import org.apache.activemq.command.ProducerId;
import org.apache.neethi.Policy;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class PullMessageSenderIT extends AbstractIT {
    @Autowired
    private PullMessageSender pullMessageSender;

    @Injectable
    private MSHDispatcher mshDispatcher;

    @Injectable
    private MessageUtil messageUtil;

//    @Before
//    public void setup(){
//        Deencapsulation.setField(pullMessageSender, mshDispatcher);
//        Deencapsulation.setField(pullMessageSender, messageUtil);
//    }

    @Test
    public void testProcessPullRequestWhenNoMessageForPullRequest(@Mocked SOAPMessage response, @Mocked Ebms3Messaging ebms3Messaging, @Mocked Ebms3SignalMessage signalMessage) throws IOException, XmlProcessingException, EbMS3Exception {
//        new Expectations(){{
//            mshDispatcher.dispatch((SOAPMessage)any, anyString, (Policy)any, (LegConfiguration)any, anyString);
//            result = response;
//
////            messageUtil.getMessage(response);
////            result = ebms3Messaging;
//
//            messageUtil.getMessage(response).getUserMessage();
//            result = null;
//            messageUtil.getMessage(response).getSignalMessage();
//            result = signalMessage;
//            signalMessage.getError();
//            result = Collections.emptySet();
//        }};

        uploadPmode(null, "dataset/pmode/PMode_pull.xml", null);
        ActiveMQMapMessage map = getActiveMQMapMessage();
        pullMessageSender.processPullRequest(map);
// ((DispatchImpl) dispatch).getClient().getEndpoint().getEndpointInfo().getAddress() //http://localhost:8080/domibus/services/msh
        new Verifications() {{

        }};
    }

    private static ActiveMQMapMessage getActiveMQMapMessage() throws IOException {
        ActiveMQMapMessage message = new ActiveMQMapMessage();
        message.setProperty("mpc", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/mpcPullSelfSend");
        message.setProperty("pmodKey", "blue_gw_pMK_SEP_blue_gw_pMK_SEP_testService18_pMK_SEP_tc18Action_pMK_SEP__pMK_SEP_pullTestcase18tc18Action");
        message.setProperty("DOMAIN", "default");
        message.setProperty("NOTIFY_BUSINNES_ON_ERROR", "false");
        message.setProperty("pullRequestId", "adc4bbdf-d20e-11ed-9818-06ea009aa4a1");
        message.setProperty("originalQueue", "domibus.internal.pull.queue");

        message.setMessageId(new MessageId("ID:a-1bt70jaqblv62-41929-1680518854306-6:22:1:1:99"));
        message.setProducerId(new ProducerId("ID:a-1bt70jaqblv62-41929-1680518854306-6:22:1:1"));
        message.setDestination(new ActiveMQQueue("queue://domibus.internal.pull.queue"));
//        message.setTransactionId();
        long timestamp = new Date().getTime();
        message.setTimestamp(timestamp);
        message.setBrokerInTime(timestamp);
        message.setBrokerOutTime(timestamp);
        message.setCommandId(148);
        message.setResponseRequired(false);
        message.setCompressed(false);
        message.setPersistent(true);
        return message;
    }


}