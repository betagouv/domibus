package eu.domibus.core.message.nonrepudiation;

import eu.domibus.AbstractIT;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.SignalMessage;
import eu.domibus.common.MessageDaoTestUtil;
import eu.domibus.core.message.signal.SignalMessageDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author François Gautier
 * @since 5.0
 */
@Transactional
public class SignalMessageRawServiceIT extends AbstractIT {

    public static final String RAW_XML = "TEST";
    @Autowired
    private SignalMessageRawService signalMessageRawService;

    @Autowired
    private MessageDaoTestUtil messageDaoTestUtil;
    @Autowired
    private SignalMessageDao signalMessageDao;
    @Autowired
    protected SignalMessageRawEnvelopeDao signalMessageRawEnvelopeDao;


    @Test
    @Disabled("EDELIVERY-11795")
    public void noSignalFound() {
        try {
            signalMessageRawService.saveSignalMessageRawService("", 1L);
            Assertions.fail();
        } catch (DomibusCoreException e) {
            //OK
        }
    }

    @Test
    @Transactional
    public void SignalFoundNoRaw() {
        messageDaoTestUtil.createSignalMessageLog("msg1", new Date());
        SignalMessage msg1 = signalMessageDao.findByUserMessageIdWithUserMessage("msg1", MSHRole.SENDING);

        signalMessageRawService.saveSignalMessageRawService(RAW_XML, msg1.getEntityId());

        Assertions.assertEquals(RAW_XML, signalMessageRawEnvelopeDao.findSignalMessageByUserMessageId("msg1", MSHRole.SENDING).getRawXmlMessage());
    }
}
