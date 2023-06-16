package eu.domibus.core.message;


import eu.domibus.ITTestsService;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.BackendConnector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author idragusa
 * @since 5.0
 */
@Transactional
public class DeleteSentSuccessMessageIT extends DeleteMessageAbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DeleteSentSuccessMessageIT.class);

    @Autowired
    private ITTestsService itTestsService;

    @Autowired
    private UserMessageDao userMessageDao;
    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Transactional
    @BeforeEach
    public void updatePmodeForAcknowledged() throws IOException, XmlProcessingException {
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("security=\"eDeliveryAS4Policy\"", "security=\"noSigNoEnc\"");
        uploadPMode(SERVICE_PORT, toReplace);
    }

    /**
     * Test to delete a sent success message
     */
    @Test
    public void testDeleteSentMessage() throws MessagingProcessingException {
        BackendConnector backendConnector = Mockito.mock(BackendConnector.class);
        Mockito.when(backendConnectorProvider.getBackendConnector(Mockito.any(String.class))).thenReturn(backendConnector);

        String messageId = itTestsService.sendMessageWithStatus(MessageStatus.ACKNOWLEDGED);

        LOG.info("Message Id to delete: [{}]", messageId);
        UserMessage byMessageId = userMessageDao.findByMessageId(messageId);
        Assertions.assertNotNull(byMessageId);

        Assertions.assertNotNull(userMessageDao.findByEntityId(byMessageId.getEntityId()));
        Assertions.assertNotNull(userMessageLogDao.findByEntityIdSafely(byMessageId.getEntityId()));

        deleteAllMessages(messageId);

        Assertions.assertNull(userMessageDao.findByMessageId(messageId));
        Assertions.assertNull(userMessageLogDao.findByMessageId(messageId, MSHRole.SENDING));
    }
}
