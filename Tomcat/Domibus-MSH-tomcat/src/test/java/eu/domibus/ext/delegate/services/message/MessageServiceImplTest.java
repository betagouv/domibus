package eu.domibus.ext.delegate.services.message;

import eu.domibus.api.util.DomibusStringUtil;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Sebastian-Ion TINCU
 */
public class MessageServiceImplTest {

    @Injectable
    private DomibusStringUtil domibusStringUtil;

    private MessageServiceImpl messageService = new MessageServiceImpl(domibusStringUtil);

    @Test
    public void trimsTheMessageIdWhenCleaningIt() {
        String messageId = " \n\t -Dom137--  \t\n ";

        String trimmedMessageId = messageService.cleanMessageIdentifier(messageId);

        Assertions.assertEquals("-Dom137--", trimmedMessageId, "Should have trimmed control characters at the beginning and the end of the message identifier when cleaning it");
    }

    @Test
    public void doesNothingToTheMessageIdMissingControlCharactersWhenCleaningIt() {
        String messageId = "-Dom138--";

        String trimmedMessageId = messageService.cleanMessageIdentifier(messageId);

        Assertions.assertEquals("-Dom138--", trimmedMessageId, "Should have returned the message as is when cleaning it if the message does not contain control characters");
    }

}
