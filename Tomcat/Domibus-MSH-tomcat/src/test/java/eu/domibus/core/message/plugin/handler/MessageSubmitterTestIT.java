package eu.domibus.core.message.plugin.handler;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageType;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.api.plugin.BackendConnectorService;
import eu.domibus.core.message.MessagesLogServiceImpl;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.payload.persistence.filesystem.PayloadFileStorageProvider;
import eu.domibus.core.plugin.BackendConnectorProvider;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.BackendConnector;
import eu.domibus.plugin.Submission;
import eu.domibus.plugin.handler.MessageSubmitter;
import eu.domibus.test.common.SubmissionUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Transactional
public class MessageSubmitterTestIT extends AbstractIT {

    @Configuration
    static class ContextConfiguration {
        @Primary
        @Bean
        public BackendConnectorService backendConnectorProvider() {
            return Mockito.mock(BackendConnectorService.class);
        }
    }

    @Autowired
    protected SubmissionUtil submissionUtil;

    @Autowired
    BackendConnectorProvider backendConnectorProvider;

    @Autowired
    MessageSubmitter messageSubmitter;

    @Autowired
    MessagesLogServiceImpl messagesLogService;

    @Autowired
    UserMessageLogDao userMessageLogDao;

    @Autowired
    protected PayloadFileStorageProvider payloadFileStorageProvider;

    @BeforeEach
    public void before() {
        BackendConnector backendConnector = Mockito.mock(BackendConnector.class);
        Mockito.when(backendConnectorProvider.getBackendConnector(Mockito.any(String.class)))
                .thenReturn(backendConnector);
        payloadFileStorageProvider.initialize();
    }

    @Test
    public void submit() throws MessagingProcessingException, IOException {
        Submission submission = submissionUtil.createSubmission();
        uploadPMode();
        final String messageId = messageSubmitter.submit(submission, "mybackend");

        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId, MSHRole.SENDING);
        assertNotNull(userMessageLog);

        final HashMap<String, Object> filters = new HashMap<>();
        filters.put("receivedTo", new Date());
        messagesLogService.countAndFindPaged(MessageType.USER_MESSAGE, 0, 10, "received", false, filters);
    }

    @Test
    public void submitWithNoFromPartyId() throws MessagingProcessingException, IOException {
        Submission submission = submissionUtil.createSubmission();
        submission.getFromParties().clear();

        uploadPMode();
        try {
            messageSubmitter.submit(submission, "mybackend");
            Assertions.fail("Messaging exception should have been thrown");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof MessagingProcessingException);
            Assertions.assertTrue(e.getMessage().contains("Mandatory field From PartyId is not provided"));
        }
    }

    @Test
    public void submitWithNoToPartyId() throws MessagingProcessingException, IOException {
        Submission submission = submissionUtil.createSubmission();
        submission.getToParties().clear();

        uploadPMode();
        try {
            messageSubmitter.submit(submission, "mybackend");
            Assertions.fail("Messaging exception should have been thrown");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof MessagingProcessingException);
            Assertions.assertTrue(e.getMessage().contains("ValueInconsistent detail: Mandatory field To PartyId is not provided"));
        }
    }


}
