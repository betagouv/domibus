package eu.domibus.plugin.ws;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.common.JPAConstants;
import eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.*;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.webService.generated.BackendInterface;
import eu.domibus.plugin.webService.generated.LargePayloadType;
import eu.domibus.plugin.webService.generated.SubmitRequest;
import eu.domibus.plugin.webService.generated.SubmitResponse;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogDao;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageStatus;
import eu.domibus.plugin.ws.backend.dispatch.WSPluginDispatchClientProvider;
import eu.domibus.plugin.ws.generated.WebServicePluginInterface;
import eu.domibus.plugin.ws.message.WSMessageLogDao;
import eu.domibus.test.AbstractIT;
import eu.domibus.test.DomibusConditionUtil;
import eu.domibus.test.PModeUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.w3c.dom.Document;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.PersistenceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by draguio on 18/02/2016.
 */
@WireMockTest
public abstract class AbstractBackendWSIT extends AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractBackendWSIT.class);

    public static final String STRING_TYPE = "string";

    public static final String WS_NOT_QUEUE = "domibus.notification.webservice";

    // TODO: François Gautier 03-02-21 @deprecated to be removed when deprecated endpoint /backend is removed
    @Autowired
    protected BackendInterface backendWebService;

    @Autowired
    private WSBackendMessageLogDao wsBackendMessageLogDao;

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    protected javax.persistence.EntityManager em;

    @Autowired
    protected WebServicePluginInterface webServicePluginInterface;

    @Autowired
    WSMessageLogDao wsMessageLogDao;

    @Autowired
    DomibusConditionUtil domibusConditionUtil;
    @Autowired
    PModeUtil pModeUtil;

    @Configuration
    static class ContextConfiguration {
        @Primary
        @Bean
        public WSPluginDispatchClientProvider wsPluginDispatchClientProvider() {
            return Mockito.mock(WSPluginDispatchClientProvider.class);
        }

        @Primary
        @Bean
        public BackendConnectorProvider backendConnectorProvider() {
            return Mockito.mock(BackendConnectorProvider.class);
        }
    }

    protected void verifySendMessageAck(eu.domibus.plugin.ws.generated.body.SubmitResponse response) {
        final List<String> messageID = response.getMessageID();
        assertNotNull(response);
        assertNotNull(messageID);
        assertEquals(1, messageID.size());
        final String messageId = messageID.iterator().next();

        domibusConditionUtil.waitUntilMessageIsAcknowledged(messageId);

        verify(postRequestedFor(urlMatching("/domibus/services/msh"))
                .withRequestBody(matching(".*"))
                .withHeader("Content-Type", notMatching("application/soap+xml")));

        final MessageStatus messageStatus = userMessageLogDao.getMessageStatus(messageId, MSHRole.SENDING);
        Assertions.assertEquals(MessageStatus.ACKNOWLEDGED, messageStatus);

    }

    /**
     * @deprecated to be removed when deprecated endpoint /backend is removed
     */
    @Deprecated
    protected void verifySendMessageAck(SubmitResponse response) {
        final List<String> messageID = response.getMessageID();
        assertNotNull(response);
        assertNotNull(messageID);
        assertEquals(1, messageID.size());
        final String messageId = messageID.iterator().next();

        domibusConditionUtil.waitUntilMessageIsAcknowledged(messageId);

        verify(postRequestedFor(urlMatching("/domibus/services/msh"))
                .withRequestBody(matching(".*"))
                .withHeader("Content-Type", notMatching("application/soap+xml")));

        final MessageStatus messageStatus = userMessageLogDao.getMessageStatus(messageId, MSHRole.SENDING);
        Assertions.assertEquals(MessageStatus.ACKNOWLEDGED, messageStatus);

    }

    protected eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging createMessageHeaderWs(String payloadHref) {
        return createMessageHeaderWs(payloadHref, "text/xml");
    }

    protected eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging createMessageHeaderWs(String payloadHref, String mimeType) {
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging ebMSHeaderInfo = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging();
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage userMessage = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage();
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo messageInfo = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo();
        userMessage.setMpc(Ebms3Constants.DEFAULT_MPC);
        messageInfo.setMessageId(UUID.randomUUID() + "@domibus.eu");
        userMessage.setMessageInfo(messageInfo);
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo collaborationInfo = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo();
        collaborationInfo.setAction("TC1Leg1");
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service service = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service();
        service.setValue("bdx:noprocess");
        service.setType("tc1");
        collaborationInfo.setService(service);
        userMessage.setCollaborationInfo(collaborationInfo);
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageProperties messageProperties = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageProperties();
        messageProperties.getProperty().add(createPropertyWs("originalSender", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1", STRING_TYPE));
        messageProperties.getProperty().add(createPropertyWs("finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4", STRING_TYPE));
        userMessage.setMessageProperties(messageProperties);
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo partyInfo = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo();
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From from = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From();
        from.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId sender = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId();
        sender.setType("urn:oasis:names:tc:ebcore:partyid-type:unregistered");
        sender.setValue("domibus-blue");
        from.setPartyId(sender);
        partyInfo.setFrom(from);
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To to = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To();
        to.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder");
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId receiver = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId();
        receiver.setType("urn:oasis:names:tc:ebcore:partyid-type:unregistered");
        receiver.setValue("domibus-red");
        to.setPartyId(receiver);
        partyInfo.setTo(to);
        userMessage.setPartyInfo(partyInfo);
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo payloadInfo = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo();
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo partInfo = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo();
        partInfo.setHref(payloadHref);
        if (mimeType != null) {
            eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties partProperties = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties();
            partProperties.getProperty().add(createPropertyWs("MimeType", mimeType, STRING_TYPE));
            partInfo.setPartProperties(partProperties);
        }

        payloadInfo.getPartInfo().add(partInfo);
        userMessage.setPayloadInfo(payloadInfo);
        ebMSHeaderInfo.setUserMessage(userMessage);
        return ebMSHeaderInfo;
    }

    protected eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property createPropertyWs(String name, String value, String type) {
        eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property aProperty = new eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property();
        aProperty.setName(name);
        aProperty.setValue(value);
        aProperty.setType(type);
        return aProperty;
    }

    /**
     * @deprecated to be removed when deprecated endpoint /backend is removed
     */
    @Deprecated
    protected Messaging createMessageHeader(String payloadHref) {
        return createMessageHeader(payloadHref, "text/xml");
    }

    /**
     * @deprecated to be removed when deprecated endpoint /backend is removed
     */
    @Deprecated
    protected Messaging createMessageHeader(String payloadHref, String mimeType) {
        Messaging ebMSHeaderInfo = new Messaging();
        UserMessage userMessage = new UserMessage();
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessageId(UUID.randomUUID() + "f@domibus.eu");
        userMessage.setMessageInfo(messageInfo);
        CollaborationInfo collaborationInfo = new CollaborationInfo();
        collaborationInfo.setAction("TC1Leg1");
        Service service = new Service();
        service.setValue("bdx:noprocess");
        service.setType("tc1");
        collaborationInfo.setService(service);
        userMessage.setCollaborationInfo(collaborationInfo);
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getProperty().add(createProperty("originalSender", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1", STRING_TYPE));
        messageProperties.getProperty().add(createProperty("finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4", STRING_TYPE));
        userMessage.setMessageProperties(messageProperties);
        PartyInfo partyInfo = new PartyInfo();
        From from = new From();
        from.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
        PartyId sender = new PartyId();
        sender.setType("urn:oasis:names:tc:ebcore:partyid-type:unregistered");
        sender.setValue("domibus-blue");
        from.setPartyId(sender);
        partyInfo.setFrom(from);
        To to = new To();
        to.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder");
        PartyId receiver = new PartyId();
        receiver.setType("urn:oasis:names:tc:ebcore:partyid-type:unregistered");
        receiver.setValue("domibus-red");
        to.setPartyId(receiver);
        partyInfo.setTo(to);
        userMessage.setPartyInfo(partyInfo);
        PayloadInfo payloadInfo = new PayloadInfo();
        PartInfo partInfo = new PartInfo();
        partInfo.setHref(payloadHref);
        if (mimeType != null) {
            PartProperties partProperties = new PartProperties();
            partProperties.getProperty().add(createProperty("MimeType", mimeType, STRING_TYPE));
            partInfo.setPartProperties(partProperties);
        }

        payloadInfo.getPartInfo().add(partInfo);
        userMessage.setPayloadInfo(payloadInfo);
        ebMSHeaderInfo.setUserMessage(userMessage);
        return ebMSHeaderInfo;
    }

    /**
     * @deprecated to be removed when deprecated endpoint /backend is removed
     */
    @Deprecated
    protected Property createProperty(String name, String value, String type) {
        Property aProperty = new Property();
        aProperty.setName(name);
        aProperty.setValue(value);
        aProperty.setType(type);
        return aProperty;
    }

    /**
     * @deprecated to be removed when deprecated endpoint /backend is removed
     */
    @Deprecated
    protected SubmitRequest createSubmitRequest(String payloadHref) {
        final SubmitRequest submitRequest = new SubmitRequest();
        LargePayloadType largePayload = new LargePayloadType();
        final DataHandler messageHandler = new DataHandler(new ByteArrayDataSource(Base64.decodeBase64("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=".getBytes()), "text/xml"));
        largePayload.setPayloadId(payloadHref);
        largePayload.setContentType("text/xml");
        largePayload.setValue(messageHandler);
        submitRequest.getPayload().add(largePayload);
        return submitRequest;
    }

    protected eu.domibus.plugin.ws.generated.body.SubmitRequest createSubmitRequestWs(String payloadHref) {
        final eu.domibus.plugin.ws.generated.body.SubmitRequest submitRequest = new eu.domibus.plugin.ws.generated.body.SubmitRequest();
        eu.domibus.plugin.ws.generated.body.LargePayloadType largePayload = new eu.domibus.plugin.ws.generated.body.LargePayloadType();
        final DataHandler messageHandler = new DataHandler(new ByteArrayDataSource(Base64.decodeBase64("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPGhlbGxvPndvcmxkPC9oZWxsbz4=".getBytes()), "text/xml"));
        largePayload.setPayloadId(payloadHref);
        largePayload.setContentType("text/xml");
        largePayload.setValue(messageHandler);
        submitRequest.getPayload().add(largePayload);
        return submitRequest;
    }

    protected void waitForMessages(int count) {
        with().pollInterval(200, TimeUnit.MILLISECONDS).await().atMost(2, TimeUnit.SECONDS).until(findAllHasCount(count));
    }

    protected Callable<Boolean> findAllHasCount(int count) {
        return () -> wsMessageLogDao.findAll().size() == count;
    }

    protected void waitForMessage(String messageId) {
        try {
            with().pollInterval(200, TimeUnit.MILLISECONDS).await().atMost(2, TimeUnit.SECONDS).until(findByMessageIdReturnMessage(messageId));
        } catch (ConditionTimeoutException e) { //workaround as awaitility has no (yet) implemented a callable for onTimeout()
            LOG.debug("ConditionTimeoutException: ", e);
        }
    }

    protected Callable<Boolean> findByMessageIdReturnMessage(String messageId) {
        return () -> wsMessageLogDao.findByMessageId(messageId) == null;
    }

    protected void uploadPmode(Integer redHttpPort) throws IOException, XmlProcessingException {
        pModeUtil.uploadPmode(redHttpPort);
    }

    protected String replace(String body, Pair<String, String>... replace) {
        for (Pair<String, String> key : replace) {
            body = body.replaceAll(key.getLeft(), key.getRight());
        }
        return body;
    }

    public void prepareSendMessage(String responseFileName, Pair<String, String>... replace) {
        String body = getAS4Response(responseFileName);
        if (replace != null) {
            body = replace(body, replace);
        }

        // Mock the response from the recipient MSH
        stubFor(post(urlEqualTo("/domibus/services/msh"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/soap+xml")
                        .withBody(body)));
    }

    /**
     * Convert the given file to a string
     */
    protected String getAS4Response(String file) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputStream is = getClass().getClassLoader().getResourceAsStream("dataset/as4/" + file);
            Document doc = db.parse(is);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = null;
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception exc) {
            Assertions.fail(exc.getMessage());
            exc.printStackTrace();
        }
        return null;
    }

    protected void waitUntilMessageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        with().pollInterval(500, TimeUnit.MILLISECONDS).await().atMost(120, TimeUnit.SECONDS)
                .until(messageHasStatus(messageId, mshRole, messageStatus));
    }

    protected Callable<Boolean> messageHasStatus(String messageId, MSHRole mshRole, MessageStatus messageStatus) {
        return () -> messageStatus == userMessageLogDao.getMessageStatus(messageId, mshRole);
    }

    protected void waitUntilMessageIsInWaitingForRetry(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.SENDING, MessageStatus.WAITING_FOR_RETRY);
    }

    public void waitUntilMessageIsReceived(String messageId) {
        waitUntilMessageHasStatus(messageId, MSHRole.RECEIVING, MessageStatus.RECEIVED);
    }

    public void createEntityAndFlush(List<WSBackendMessageLogEntity> entities) {
        for (WSBackendMessageLogEntity entity : entities) {
            wsBackendMessageLogDao.create(entity);
        }
        em.flush();
    }

    @SuppressWarnings("SameParameterValue")
    public WSBackendMessageLogEntity create(WSBackendMessageStatus status) {
        return create(status, null, null, null);
    }

    public WSBackendMessageLogEntity create(WSBackendMessageStatus status, LocalDateTime localDateTime, String originalSender, String finalRecipient) {
        WSBackendMessageLogEntity entity = new WSBackendMessageLogEntity();
        entity.setMessageId(UUID.randomUUID().toString());
        entity.setMessageStatus(eu.domibus.common.MessageStatus.WAITING_FOR_RETRY);
        entity.setBackendMessageStatus(status);
        entity.setOriginalSender(originalSender);
        entity.setFinalRecipient(finalRecipient);
        entity.setSendAttempts(1);
        entity.setSendAttemptsMax(3);
        entity.setNextAttempt(yesterday());
        if (localDateTime != null) {
            entity.setCreationTime(getDate(localDateTime));
        }
        return entity;
    }


    private Date getDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
    }

    private Date yesterday() {
        return Date.from(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant());
    }

}
