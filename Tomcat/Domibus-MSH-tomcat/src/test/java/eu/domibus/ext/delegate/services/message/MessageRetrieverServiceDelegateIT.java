package eu.domibus.ext.delegate.services.message;

import eu.domibus.AbstractIT;
import eu.domibus.common.ErrorResult;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.messaging.DuplicateMessageException;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.Submission;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class MessageRetrieverServiceDelegateIT extends AbstractIT {
    private static final String MESS_ID = UUID.randomUUID().toString();
    private static final String MESS_1 = "msg_ack_100";

    @Autowired
    private MessageRetrieverServiceDelegate messageRetrieverServiceDelegate;

    @Test
    public void testDownloadMessageAuthUserNok() {

        try {
            messageRetrieverServiceDelegate.checkMessageAuthorization(MESS_ID);
            Assert.fail("It should throw AuthenticationException");
        } catch (eu.domibus.api.messaging.MessageNotFoundException adEx) {
            assertTrue(adEx.getMessage().contains("[DOM_009]:Message [" + MESS_ID + "] does not exist"));
        }


    }

    @Test
    public void testGetStatus() throws MessageNotFoundException, DuplicateMessageException {

        // When
        final MessageStatus status = messageRetrieverServiceDelegate.getStatus("not_found");
        Assert.assertEquals(MessageStatus.NOT_FOUND, status);

    }

    @Test
    public void testGetStatus_found() throws MessageNotFoundException, DuplicateMessageException {

        // When
        final MessageStatus status = messageRetrieverServiceDelegate.getStatus(MESS_1);

        Assert.assertEquals(MessageStatus.ACKNOWLEDGED, status);
    }

    @Test
    public void testGetStatus_mshrole_found() throws MessageNotFoundException {

        // When
        final MessageStatus status = messageRetrieverServiceDelegate.getStatus(MESS_1, MSHRole.RECEIVING);

        Assert.assertEquals(MessageStatus.ACKNOWLEDGED, status);
    }

    @Test
    public void testGetStatus_found2() throws MessageNotFoundException {

        // When
        final MessageStatus status = messageRetrieverServiceDelegate.getStatus(230412100000000001L);

        Assert.assertEquals(MessageStatus.ACKNOWLEDGED, status);
    }

    @Test
    public void download() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.downloadMessage(230412100000000001L, false);

        Assert.assertNotNull(msg);
    }

    @Test
    public void download2() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.downloadMessage(MESS_1, false);

        Assert.assertNotNull(msg);
    }

    @Test
    public void download3() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.downloadMessage(230412100000000001L);

        Assert.assertNotNull(msg);
    }

    @Test
    public void download4() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.downloadMessage(MESS_1);

        Assert.assertNotNull(msg);
    }

    @Test
    @Transactional
    public void markMessageAsDownloaded() {

        // When
        messageRetrieverServiceDelegate.markMessageAsDownloaded(MESS_1);
    }

    @Test
    public void browseMessage() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.browseMessage(MESS_1, MSHRole.RECEIVING);

        Assert.assertNotNull(msg);
    }

    @Test
    public void browseMessage2() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.browseMessage(230412100000000001L);

        Assert.assertNotNull(msg);
    }

    @Test
    public void browseMessage3() throws MessageNotFoundException {

        // When
        final Submission msg = messageRetrieverServiceDelegate.browseMessage(MESS_1);

        Assert.assertNotNull(msg);
    }

    @Test
    public void getErrorsForMessage() throws MessageNotFoundException, DuplicateMessageException {

        // When
        List<? extends ErrorResult> errorsForMessage = messageRetrieverServiceDelegate.getErrorsForMessage(MESS_1);

        Assert.assertEquals(0, errorsForMessage.size());
    }
    @Test
    public void getErrorsForMessage2() throws MessageNotFoundException {

        // When
        List<? extends ErrorResult> errorsForMessage = messageRetrieverServiceDelegate.getErrorsForMessage(MESS_1, MSHRole.RECEIVING);

        Assert.assertEquals(0, errorsForMessage.size());
    }


}
