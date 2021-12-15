
package eu.domibus.plugin.jms;


import eu.domibus.api.model.*;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.common.DeliverMessageEvent;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.NotificationType;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.message.MessagingService;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.core.message.dictionary.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.test.PModeUtil;
import eu.domibus.test.UserMessageSampleUtil;
import eu.domibus.test.common.JMSMessageUtil;
import eu.domibus.test.common.SoapSampleUtil;
import org.apache.activemq.ActiveMQXAConnection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.activation.DataHandler;
import javax.jms.*;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import static eu.domibus.messaging.MessageConstants.MESSAGE_ENTITY_ID;
import static eu.domibus.messaging.MessageConstants.MESSAGE_ID;

/**
 * This JUNIT implements the Test cases Download Message-03 and Download Message-04.
 * It uses the JMS backend connector.
 *
 * @author martifp
 */
public class DownloadMessageJMSIT extends AbstractBackendJMSIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DownloadMessageJMSIT.class);

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager em;

    @Autowired
    private ConnectionFactory jmsConnectionFactory;

    @Autowired
    JMSPluginImpl backendJms;

    @Autowired
    MessagingService messagingService;

    @Autowired
    UserMessageLogDefaultService userMessageLogService;

    @Autowired
    PModeUtil pModeUtil;

    @Autowired
    SoapSampleUtil soapSampleUtil;

    @Autowired
    UserMessageSampleUtil userMessageSampleUtil;
    @Autowired
    JMSMessageUtil jmsMessageUtil;
    @Autowired
    ActionDictionaryService actionDictionaryService;
    @Autowired
    ServiceDictionaryService serviceDictionaryService;
    @Autowired
    AgreementDictionaryService agreementDictionaryService;
    @Autowired
    MpcDictionaryService mpcDictionaryService;
    @Autowired
    PartyIdDictionaryService partyIdDictionaryService;
    @Autowired
    PartyRoleDictionaryService partyRoleDictionaryService;
    @Autowired
    MessagePropertyDictionaryService messagePropertyDictionaryService;

    @Autowired
    UserMessageService userMessageService;

    @Before
    public void before() throws IOException, XmlProcessingException {
        pModeUtil.uploadPmode();
    }

    /**
     * Negative test: the message is not found in the JMS queue and a specific exception is returned.
     */
    @Test(expected = RuntimeException.class)
    public void testDownloadMessageInvalidId() throws RuntimeException {

        // Prepare the request to the backend
        String messageId = "invalid@e-delivery.eu";

        DeliverMessageEvent deliverMessageEvent = new DeliverMessageEvent(123, messageId, new HashMap<>());
        backendJms.deliverMessage(deliverMessageEvent);

        Assert.fail("DownloadMessageFault was expected but was not raised");
    }

    /**
     * Tests that a message is found in the JMS queue and pushed to the business queue.
     */
    @Test
    public void testDownloadMessageOk() throws Exception {
        final UserMessage userMessage = getUserMessage();
        userMessageService.saveUserMessage(userMessage);
        javax.jms.Connection connection = jmsConnectionFactory.createConnection("domibus", "changeit");
        connection.start();

        pushQueueMessage(userMessage.getEntityId(), userMessage.getMessageId(), connection, JMS_NOT_QUEUE_NAME);

        Message message = jmsMessageUtil.popQueueMessageWithTimeout(connection, JMS_BACKEND_OUT_QUEUE_NAME, 5000);
        Assert.assertNotNull(message);

        connection.close();
    }

    private UserMessage getUserMessage() throws IOException {
        String pModeKey = soapSampleUtil.composePModeKey("blue_gw", "red_gw", "testService1",
                "tc1Action", "", "pushTestcase1tc2ActionWithPayload");
        final LegConfiguration legConfiguration = pModeProvider.getLegConfiguration(pModeKey);

        String messageId = "2809cef6-240f-4792-bec1-7cb300a34679@domibus.eu";
        final UserMessage userMessage = userMessageSampleUtil.getUserMessageTemplate();
        userMessage.setAction(actionDictionaryService.findOrCreateAction(userMessage.getActionValue()));
        userMessage.setService(serviceDictionaryService.findOrCreateService(userMessage.getService().getValue(), userMessage.getService().getType()));
        userMessage.setAgreementRef(agreementDictionaryService.findOrCreateAgreement(userMessage.getAgreementRef().getValue(), userMessage.getAgreementRef().getType()));
        userMessage.setMpc(mpcDictionaryService.findOrCreateMpc(userMessage.getMpcValue()));
        userMessage.getPartyInfo().getTo().setToPartyId(partyIdDictionaryService.findOrCreateParty("toPartyValue", "toPartyType"));
        userMessage.getPartyInfo().getTo().setToRole(partyRoleDictionaryService.findOrCreateRole("toRole"));
        userMessage.getPartyInfo().getFrom().setFromPartyId(partyIdDictionaryService.findOrCreateParty("fromPartyValue", "fromPartyType"));
        userMessage.getPartyInfo().getFrom().setFromRole(partyRoleDictionaryService.findOrCreateRole("fromRole"));
        HashSet<MessageProperty> messageProperties = new HashSet<>();
        MessageProperty e = new MessageProperty();
        e.setName("name");
        e.setValue("value");
        e.setType("type");
        messageProperties.add(messagePropertyDictionaryService.findOrCreateMessageProperty("name",
                "value",
                "type"));
        userMessage.setMessageProperties(messageProperties);
        String messagePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<hello>world</hello>";
        userMessage.setMessageId(messageId);
        ArrayList<PartInfo> partInfoList = new ArrayList<>();
        PartInfo partInfo = new PartInfo();
        partInfo.setBinaryData(messagePayload.getBytes());
        partInfo.setMime("text/xml");
        partInfo.setPayloadDatahandler(new DataHandler(new ByteArrayDataSource(messagePayload.getBytes(), "text/xml")));

        partInfoList.add(partInfo);
        messagingService.storeMessagePayloads(userMessage, partInfoList, MSHRole.RECEIVING, legConfiguration, "backendWebservice");

        UserMessageLog userMessageLog = new UserMessageLog();
        MessageStatusEntity messageStatus = new MessageStatusEntity();
        messageStatus.setMessageStatus(MessageStatus.RECEIVED);
        userMessageLog.setMessageStatus(messageStatus);
        userMessageLog.setUserMessage(userMessage);
        MSHRoleEntity mshRole = new MSHRoleEntity();
        mshRole.setRole(MSHRole.RECEIVING);
        userMessageLog.setMshRole(mshRole);
        userMessageLog.setReceived(new Date());
        userMessageLogService.save(userMessage, eu.domibus.common.MessageStatus.RECEIVED.name(), NotificationStatus.REQUIRED.name(), MSHRole.RECEIVING.name(), 1, "backendWebservice");

        LOG.info("userMessage.getEntityId() [{}]", userMessage.getEntityId());
        LOG.info("userMessage.getMessageId() [{}]", userMessage.getMessageId());
        return userMessage;
    }

    /**
     * The connection must be started and stopped before and after the method call.
     */
    protected void pushQueueMessage(long entityId, String messageId, Connection connection, String queueName) throws Exception {

        // set XA mode to Session.AUTO_ACKNOWLEDGE - test does not use XA transaction
        if (connection instanceof ActiveMQXAConnection) {
            ((ActiveMQXAConnection) connection).setXaAckMode(Session.AUTO_ACKNOWLEDGE);
        }
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queueName);
        MessageProducer producer = session.createProducer(destination);
        // Creates the Message using Spring MessageCreator
//        NotifyMessageCreator messageCreator = new NotifyMessageCreator(messageId, NotificationType.MESSAGE_RECEIVED);
        Message msg = session.createTextMessage();
        msg.setStringProperty(MessageConstants.DOMAIN, DomainService.DEFAULT_DOMAIN.getCode());
        msg.setStringProperty(MESSAGE_ID, messageId);
        msg.setStringProperty(MESSAGE_ENTITY_ID, entityId + "");
        msg.setObjectProperty(MessageConstants.NOTIFICATION_TYPE, NotificationType.MESSAGE_RECEIVED.name());
        msg.setStringProperty(MessageConstants.ENDPOINT, "backendInterfaceEndpoint");
        msg.setStringProperty(MessageConstants.FINAL_RECIPIENT, "testRecipient");
        producer.send(msg);
        LOG.info("Message [{}] [{}] sent in queue!", entityId, messageId);
        producer.close();
        session.close();

    }

}
