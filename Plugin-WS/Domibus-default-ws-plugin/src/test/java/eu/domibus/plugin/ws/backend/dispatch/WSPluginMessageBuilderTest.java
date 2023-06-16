package eu.domibus.plugin.ws.backend.dispatch;

import eu.domibus.common.MSHRole;
import eu.domibus.ext.services.XMLUtilExtService;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import eu.domibus.plugin.ws.connector.WSPluginImpl;
import eu.domibus.plugin.ws.exception.WSPluginException;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import eu.domibus.plugin.ws.webservice.ExtendedPartInfo;
import eu.domibus.webservice.backend.generated.*;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.domibus.plugin.ws.backend.dispatch.WSPluginMessageBuilder.PAYLOAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class WSPluginMessageBuilderTest {

    public static final String MESSAGE_ID = "messageId";
    public static final long MESSAGE_ENTITY_ID = 555;
    public static final long ENTITY_ID = 123;
    public static final String ORIGINAL_SENDER = "sender";
    public static final String FINAL_RECIPIENT = "recipient";
    public static final String HREF = "HREF";
    public static final String VALUE_FOUND = "VALUE_FOUND";
    public static final String MIME_TYPE = "MIME_TYPE";
    @Tested
    private WSPluginMessageBuilder wsPluginMessageBuilder;

    @Injectable
    private XMLUtilExtService xmlUtilExtService;

    @Injectable
    private JAXBContext jaxbContextWebserviceBackend;

    @Injectable
    private WSPluginImpl wsPlugin;

    @Injectable
    private UserMessageMapper userMessageMapper;

    @Test
    public void getJaxbElement_MessageStatusChange(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.MESSAGE_STATUS_CHANGE;

            wsPluginMessageBuilder.getChangeStatus(messageLogEntity);
            result = new MessageStatusChange();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(MessageStatusChange.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    @Test
    public void getJaxbElement_sendSuccess(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.SEND_SUCCESS;

            wsPluginMessageBuilder.getSendSuccess(messageLogEntity);
            result = new SendSuccess();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(SendSuccess.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    @Test
    public void getJaxbElement_deleteBatch(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.DELETED_BATCH;

            wsPluginMessageBuilder.getDeleteBatch(messageLogEntity);
            result = new DeleteBatch();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(DeleteBatch.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    public void getJaxbElement_delete(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.DELETED;

            wsPluginMessageBuilder.getDelete(messageLogEntity);
            result = new Delete();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(Delete.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    @Test
    public void getJaxbElement_receiveSuccess(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.RECEIVE_SUCCESS;

            wsPluginMessageBuilder.getReceiveSuccess(messageLogEntity);
            result = new ReceiveSuccess();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(ReceiveSuccess.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    @Test
    public void getJaxbElement_receiveFail(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.RECEIVE_FAIL;

            wsPluginMessageBuilder.getReceiveFailure(messageLogEntity);
            result = new ReceiveFailure();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(ReceiveFailure.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    @Test
    public void getJaxbElement_sendFailure(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations(wsPluginMessageBuilder) {{
            messageLogEntity.getType();
            result = WSBackendMessageType.SEND_FAILURE;

            wsPluginMessageBuilder.getSendFailure(messageLogEntity);
            result = new SendFailure();
        }};

        Object jaxbElement = wsPluginMessageBuilder.getBody(messageLogEntity);

        assertEquals(SendFailure.class, jaxbElement.getClass());

        new FullVerifications() {
        };
    }

    @Test
    public void getSendSuccess(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;
        }};
        SendSuccess sendSuccess = wsPluginMessageBuilder.getSendSuccess(messageLogEntity);
        assertEquals(MESSAGE_ID, sendSuccess.getMessageID());
        new FullVerifications() {
        };
    }

    @Test
    public void getSDelete(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;
        }};
        Delete delete = wsPluginMessageBuilder.getDelete(messageLogEntity);
        assertEquals(MESSAGE_ID, delete.getMessageID());
        new FullVerifications() {
        };
    }

    @Test
    public void getDeleteBatch(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;
        }};
        DeleteBatch sendSuccess = wsPluginMessageBuilder.getDeleteBatch(messageLogEntity);
        assertEquals(MESSAGE_ID, sendSuccess.getMessageIds().get(0));
        new FullVerifications() {
        };
    }

    @Test
    void getDeleteBatch_empty(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = "";
        }};
        Assertions.assertThrows(WSPluginException.class, () -> wsPluginMessageBuilder.getDeleteBatch(messageLogEntity));
    }

    @Test
    public void getSendFailure(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;
        }};
        SendFailure sendFailure = wsPluginMessageBuilder.getSendFailure(messageLogEntity);
        assertEquals(MESSAGE_ID, sendFailure.getMessageID());
        new FullVerifications() {
        };
    }

    @Test
    void getSubmitMessage_MessageNotFoundException(@Injectable WSBackendMessageLogEntity messageLogEntity) throws MessageNotFoundException {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;

            messageLogEntity.getMessageEntityId();
            result = MESSAGE_ENTITY_ID;

            wsPlugin.browseMessage(MESSAGE_ENTITY_ID, (UserMessage) any);
            result = new MessageNotFoundException();
        }};

        Assertions.assertThrows(WSPluginException. class,() -> wsPluginMessageBuilder.buildSOAPMessageSubmit(messageLogEntity));

        new FullVerifications() {
        };
    }

    @Test
    public void getReceiveFailure(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;
        }};
        ReceiveFailure sendFailure = wsPluginMessageBuilder.getReceiveFailure(messageLogEntity);
        assertEquals(MESSAGE_ID, sendFailure.getMessageID());
        new FullVerifications() {
        };
    }

    @Test
    public void getReceiveSuccess(@Injectable WSBackendMessageLogEntity messageLogEntity) {
        new Expectations() {{
            messageLogEntity.getMessageId();
            result = MESSAGE_ID;
        }};
        ReceiveSuccess sendFailure = wsPluginMessageBuilder.getReceiveSuccess(messageLogEntity);
        assertEquals(MESSAGE_ID, sendFailure.getMessageID());
        new FullVerifications() {
        };
    }

    @Test
    public void fillInfoPartsForLargeFiles_noPayloadInfo(@Injectable SubmitRequest submitMessage, @Injectable UserMessage userMessage) {
        new Expectations() {{
            userMessage.getPayloadInfo();
            result = null;

            userMessage.getMessageInfo();
            result = null;
        }};
        wsPluginMessageBuilder.fillInfoPartsForLargeFiles(submitMessage, userMessage);

        new FullVerifications() {
        };
    }

    @Test
    public void fillInfoPartsForLargeFiles_noPartInfo_empty(@Injectable SubmitRequest submitMessage, @Injectable UserMessage userMessage) {
        new Expectations() {{
            userMessage.getPayloadInfo().getPartInfo();
            result = new ArrayList<>();

            userMessage.getMessageInfo();
            result = null;
        }};
        wsPluginMessageBuilder.fillInfoPartsForLargeFiles(submitMessage, userMessage);

        new FullVerifications() {
        };
    }

    @Test
    public void fillInfoPartsForLargeFiles_noPartInfo_empty2(@Injectable SubmitRequest submitMessage, @Injectable UserMessage userMessage) {
        new Expectations() {{
            userMessage.getPayloadInfo().getPartInfo();
            result = new ArrayList<>();

            userMessage.getMessageInfo();
            result = new MessageInfo();
        }};
        wsPluginMessageBuilder.fillInfoPartsForLargeFiles(submitMessage, userMessage);

        new FullVerifications(wsPluginMessageBuilder) {
        };
    }

    @Test
    public void fillInfoPartsForLargeFiles_noPartInfo_null(@Injectable SubmitRequest submitMessage, @Injectable UserMessage userMessage) {
        new Expectations() {{
            userMessage.getPayloadInfo().getPartInfo();
            result = null;

            userMessage.getMessageInfo();
            result = null;
        }};
        wsPluginMessageBuilder.fillInfoPartsForLargeFiles(submitMessage, userMessage);

        new FullVerifications() {
        };
    }

    @Test
    public void fillInfoPartsForLargeFiles(@Injectable SubmitRequest submitMessage,
                                           @Injectable UserMessage userMessage,
                                           @Injectable ExtendedPartInfo partInfo1,
                                           @Injectable ExtendedPartInfo partInfo2) {
        new Expectations(wsPluginMessageBuilder) {{
            userMessage.getPayloadInfo().getPartInfo();
            result = Arrays.asList(partInfo1, partInfo2);

            wsPluginMessageBuilder.fillInfoPart(submitMessage, partInfo1);
            times = 1;

            wsPluginMessageBuilder.fillInfoPart(submitMessage, partInfo2);
            times = 1;
        }};

        wsPluginMessageBuilder.fillInfoPartsForLargeFiles(submitMessage, userMessage);

        new FullVerifications() {
        };
    }

    @Test
    public void fillInPart_inBody(@Injectable SubmitRequest submitMessage,
                                  @Injectable ExtendedPartInfo partInfo) {

        new Expectations(wsPluginMessageBuilder) {{
            partInfo.getPayloadDatahandler();
            result = null;

            wsPluginMessageBuilder.getAnyPropertyValue(partInfo, WSPluginMessageBuilder.MIME_TYPE);
            result = WSPluginMessageBuilder.MIME_TYPE;
            wsPluginMessageBuilder.getAnyPropertyValue(partInfo, PAYLOAD_NAME);
            result = PAYLOAD_NAME;

            partInfo.isInBody();
            result = true;

        }};

        wsPluginMessageBuilder.fillInfoPart(submitMessage, partInfo);

        new FullVerifications() {{
            LargePayloadType largePayloadType;
            submitMessage.setBodyload(largePayloadType = withCapture());

            assertEquals(WSPluginMessageBuilder.MIME_TYPE, largePayloadType.getMimeType());
            assertEquals(PAYLOAD_NAME, largePayloadType.getPayloadName());
            assertNull(largePayloadType.getPayloadId());

        }};
    }

    @Test
    @Disabled("EDELIVERY-10727")
    public void fillInPart_notInBody(@Injectable SubmitRequest submitMessage,
                                     @Injectable ExtendedPartInfo partInfo,
                                     @Injectable DataHandler dataHandler) {

        List<LargePayloadType> largePayloadTypes = new ArrayList<>();

        new Expectations(wsPluginMessageBuilder) {{
            partInfo.getPayloadDatahandler();
            result = dataHandler;

            dataHandler.getContentType();
            result = "contentType";

            wsPluginMessageBuilder.getAnyPropertyValue(partInfo, WSPluginMessageBuilder.MIME_TYPE);
            result = WSPluginMessageBuilder.MIME_TYPE;

            wsPluginMessageBuilder.getAnyPropertyValue(partInfo, PAYLOAD_NAME);
            result = PAYLOAD_NAME;

            partInfo.isInBody();
            result = false;

            submitMessage.getPayload();
            result = largePayloadTypes;

            partInfo.getHref();
            result = HREF;

        }};
        wsPluginMessageBuilder.fillInfoPart(submitMessage, partInfo);

        new FullVerifications() {
        };

        assertEquals(1, largePayloadTypes.size());
        assertEquals(dataHandler, largePayloadTypes.get(0).getValue());
        assertEquals(HREF, largePayloadTypes.get(0).getPayloadId());
        assertEquals(PAYLOAD_NAME, largePayloadTypes.get(0).getPayloadName());
    }

    @Test
    public void getAnyPropertyValue_empty(@Injectable ExtendedPartInfo extPartInfo, @Injectable String mimeType) {

        new Expectations() {{
            extPartInfo
                    .getPartProperties()
                    .getProperty();
            result = new ArrayList<>();
        }};
        String anyPropertyValue = wsPluginMessageBuilder.getAnyPropertyValue(extPartInfo, mimeType);
        assertNull(anyPropertyValue);
        new FullVerifications() {
        };
    }

    @Test
    public void getAnyPropertyValue_ok(@Injectable ExtendedPartInfo extPartInfo,
                                       @Injectable Property property1,
                                       @Injectable Property property2) {

        new Expectations() {{
            extPartInfo
                    .getPartProperties()
                    .getProperty();
            result = Arrays.asList(property1, property2);

            property1.getName();
            result = "NOPE";

            property2.getName();
            result = MIME_TYPE;

            property2.getValue();
            result = VALUE_FOUND;
        }};
        String anyPropertyValue = wsPluginMessageBuilder.getAnyPropertyValue(extPartInfo, MIME_TYPE);
        assertEquals(VALUE_FOUND, anyPropertyValue);
        new FullVerifications() {
        };
    }

    @Test
    public void buildSOAPMessage(@Injectable WSBackendMessageLogEntity messageLogEntity,
                                 @Injectable SendSuccess jaxbElement,
                                 @Injectable SOAPMessage soapMessage) {
        new Expectations(wsPluginMessageBuilder) {{
            wsPluginMessageBuilder.getBody(messageLogEntity);
            result = jaxbElement;
            wsPluginMessageBuilder.createSOAPMessage(jaxbElement, null);
            result = soapMessage;
        }};
        SOAPMessage result = wsPluginMessageBuilder.buildSOAPMessage(messageLogEntity);
        assertEquals(soapMessage, result);
    }

    @Test
    @Disabled("EDELIVERY-10727")
    public void createSOAPMessage(@Injectable SendSuccess sendSuccess,
                                  @Injectable SOAPMessage soapMessage,
                                  @Injectable SOAPBody soapBody) throws SOAPException, JAXBException {
        new Expectations() {{
            xmlUtilExtService.getMessageFactorySoap12().createMessage();
            result = soapMessage;

            soapMessage.getSOAPBody();
            result = soapBody;
        }};

        wsPluginMessageBuilder.createSOAPMessage(sendSuccess, null);

        new FullVerifications() {{
            jaxbContextWebserviceBackend.createMarshaller().marshal(sendSuccess, soapBody);
            times = 1;

            soapMessage.saveChanges();
            times = 1;
        }};

    }

    @Test
    @Disabled("EDELIVERY-10727")
    public void createSOAPMessage_exception(@Injectable SendSuccess sendSuccess,
                                            @Injectable SOAPMessage soapMessage,
                                            @Injectable SOAPBody soapBody) throws SOAPException {
        new Expectations() {{
            xmlUtilExtService.getMessageFactorySoap12().createMessage();
            result = soapMessage;

            soapMessage.getSOAPBody();
            result = new SOAPException();
        }};

        Assertions.assertThrows(WSPluginException. class,() -> wsPluginMessageBuilder.createSOAPMessage(sendSuccess, null));

        new FullVerifications() {
        };

    }

    @Test
    public void createSOAPMessage(@Injectable UserMessage userMessage,
                                  @Injectable eu.domibus.webservice.backend.generated.UserMessage userMessageBack) {

        new Expectations() {{
            userMessageMapper.userMessageDTOToUserMessage(userMessage);
            result = userMessageBack;
        }};

        SOAPMessage soapMessage = wsPluginMessageBuilder.createSOAPMessage(
                wsPluginMessageBuilder.getSubmitMessage(userMessage),
                wsPluginMessageBuilder.getMessaging(userMessage));
    }
}
