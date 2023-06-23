package eu.domibus.core.pmode;

import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.message.pull.MpcService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.core.message.MessageExchangeConfiguration;
import eu.domibus.api.model.UserMessage;
import eu.domibus.messaging.XmlProcessingException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class PModeDefaultServiceTest {

    @Tested
    PModeDefaultService pModeDefaultService;

    @Injectable
    UserMessageDao userMessageDao;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private MessageExchangeService messageExchangeService;

    @Injectable
    PModeValidationHelper pModeValidationHelper;

    @Injectable
    MpcService mpcService;

    @Test
    public void testGetLegConfiguration(@Injectable final UserMessage userMessage,
                                        @Injectable final eu.domibus.common.model.configuration.LegConfiguration legConfigurationEntity) throws Exception {
        final Long messageEntityId = 1L;
        final MessageExchangeConfiguration messageExchangeConfiguration = new MessageExchangeConfiguration("1", ",", "", "", "", "");
        new Expectations() {{
            userMessageDao.findByEntityId(messageEntityId);
            result = userMessage;

            pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, anyBoolean);
            result = messageExchangeConfiguration;

            pModeProvider.getLegConfiguration(messageExchangeConfiguration.getPmodeKey());
            result = legConfigurationEntity;

        }};

        pModeDefaultService.getLegConfiguration(messageEntityId);

        new Verifications() {{
            pModeDefaultService.convert(legConfigurationEntity);
        }};
    }

    @Test
    public void testUploadPModesXmlProcessingWithErrorException() throws XmlProcessingException {
        // Given
        byte[] file = new byte[]{1, 0, 1};
        XmlProcessingException xmlProcessingException = new XmlProcessingException("UnitTest1");
        xmlProcessingException.getErrors().add("error1");

        new Expectations() {{
            pModeProvider.updatePModes((byte[]) any, anyString);
            result = xmlProcessingException;

            //noinspection ThrowableNotThrown
            pModeValidationHelper.getPModeValidationException(xmlProcessingException, "Failed to upload the PMode file due to: ");
            result = new PModeValidationException("Failed to upload the PMode file due to: ", null);
        }};

        // When
        try {
            pModeDefaultService.updatePModeFile(file, "description");
            Assertions.fail();
        } catch (PModeValidationException ex) {
            Assertions.assertEquals("[DOM_003]:Failed to upload the PMode file due to: ", ex.getMessage());
        }

    }
}
