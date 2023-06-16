package eu.domibus.core.message;

import eu.domibus.api.datasource.AutoCloseFileDataSource;
import eu.domibus.api.model.*;
import eu.domibus.api.model.splitandjoin.MessageFragmentEntity;
import eu.domibus.api.model.splitandjoin.MessageGroupEntity;
import eu.domibus.api.usermessage.domain.CollaborationInfo;
import eu.domibus.api.usermessage.domain.MessageInfo;
import eu.domibus.api.usermessage.domain.PartProperties;
import eu.domibus.api.usermessage.domain.PayloadInfo;
import eu.domibus.core.message.dictionary.*;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.activation.DataHandler;
import java.io.File;
import java.util.Date;
import java.util.HashSet;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class UserMessageDefaultFactoryTest {

    @Tested
    UserMessageDefaultFactory userMessageDefaultFactory;

    @Injectable
    protected PartPropertyDictionaryService partPropertyDictionaryService;
    @Injectable
    protected MessagePropertyDictionaryService messagePropertyDictionaryService;
    @Injectable
    protected PartyIdDictionaryService partyIdDictionaryService;
    @Injectable
    protected PartyRoleDictionaryService partyRoleDictionaryService;
    @Injectable
    protected AgreementDictionaryService agreementDictionaryService;
    @Injectable
    protected ServiceDictionaryService serviceDictionaryService;
    @Injectable
    protected ActionDictionaryService actionDictionaryService;

    @Injectable
    MpcDictionaryService mpcDictionaryService;



    @Test
    public void createUserMessageFragmentTest(@Injectable UserMessage sourceMessage,
                                              @Injectable MessageInfo messageInfo,
                                              @Injectable MessageGroupEntity messageGroupEntity,
                                              @Injectable MessageFragmentEntity messageFragmentEntity,
                                              @Injectable ActionEntity actionEntity,
                                              @Injectable AgreementRefEntity agreementRef,
                                              @Injectable PartyInfo partyInfo,
                                              @Injectable MessageProperty messageProperty
    ) {
        Long fragmentNumber = 1L;
        String fragmentFile = "fragmentFile";
        HashSet<MessageProperty> messageProperties = new HashSet<>();
        messageProperties.add(messageProperty);
        new Expectations(userMessageDefaultFactory) {{

            sourceMessage.getMessageId();
            result = "messageId";

            sourceMessage.getRefToMessageId();
            result = "refToMessageId";

            sourceMessage.getTimestamp();
            result = new Date();

            sourceMessage.getActionValue();
            result = "action";

            sourceMessage.getConversationId();
            result = "conversationId";

            sourceMessage.getAgreementRef();
            result = agreementRef;

            agreementRef.getValue();
            result = "agreementRef";

            agreementRef.getType();
            result = "agreementType";
            agreementDictionaryService.findOrCreateAgreement("agreementRef", "agreementType");
            result = agreementRef;

            actionDictionaryService.findOrCreateAction("action");
            result = actionEntity;

            sourceMessage.getService().getValue();
            result = "service";

            sourceMessage.getService().getType();
            result = "serviceType";

            serviceDictionaryService.findOrCreateService("service", "serviceType");

            sourceMessage.getPartyInfo();
            result = partyInfo;
            sourceMessage.getMessageProperties();
            result = messageProperties;

            userMessageDefaultFactory.createPartyInfo(partyInfo);
            result = partyInfo;

            userMessageDefaultFactory.createMessageProperties(messageProperties);
            result = messageProperties;
        }};

        userMessageDefaultFactory.createUserMessageFragment(sourceMessage, messageGroupEntity, fragmentNumber, fragmentFile);

    }

    @Test
    public void cloneUserMessageFragmentTest(@Injectable UserMessage userMessageFragment,
                                             @Injectable MessageInfo messageInfo,
                                             @Injectable CollaborationInfo collaborationInfo,
                                             @Injectable PartyInfo partyInfo,
                                             @Injectable MessageProperty messageProperty) {
        HashSet<MessageProperty> msgProperties = new HashSet<>();
        msgProperties.add(messageProperty);

        new Expectations(userMessageDefaultFactory) {{
            userMessageFragment.getPartyInfo();
            result = partyInfo;
            userMessageFragment.getMessageProperties();
            result = msgProperties;
        }};

        userMessageDefaultFactory.cloneUserMessageFragment(userMessageFragment);

        new Verifications() {};

    }

    @Test
    public void createMessageFragmentEntityTest(@Injectable MessageGroupEntity messageGroupEntity) {
        Long fragmentNumber = 1L;

        Assertions.assertNotNull(userMessageDefaultFactory.createMessageFragmentEntity(messageGroupEntity, fragmentNumber));
    }

    @Test
    public void createPayloadInfoTest(@Injectable PayloadInfo payloadInfo,
                                      @Injectable PartInfo partInfo,
                                      @Injectable DataHandler dataHandler,
                                      @Injectable AutoCloseFileDataSource autoCloseFileDataSource,
                                      @Mocked File file,
                                      @Injectable PartProperties partProperties,
                                      @Injectable Property property) {
        Long fragmentNumber = 1L;
        String fragmentFile = "fragmentFile";

        new Expectations(userMessageDefaultFactory) {{
            new File(fragmentFile).length();
            result = 2L;
        }};

        Assertions.assertNotNull(userMessageDefaultFactory.createMessageFragmentPartInfo(fragmentFile, fragmentNumber));
    }

    @Test
    public void createPartyInfoTest(@Injectable PartyInfo source,
                                    @Injectable From from,
                                    @Injectable PartyId party,
                                    @Injectable To to,
                                    @Injectable PartyRole partyRole) {

        new Expectations(userMessageDefaultFactory) {{
            source.getFrom();
            result = from;

            from.getRoleValue();
            result = "FromRole";

            partyRoleDictionaryService.findOrCreateRole("FromRole");
            result = partyRole;

            from.getFromPartyId();
            result = party;

            party.getValue();
            result = "partyValue";

            party.getType();
            result = "PartyType";

            partyIdDictionaryService.findOrCreateParty("partyValue", "PartyType");
            result = party;

            source.getTo();
            result = to;

            to.getRoleValue();
            result = "ToRole";

            partyRoleDictionaryService.findOrCreateRole("ToRole");
            result = partyRole;

            to.getToPartyId();
            result = party;

            partyIdDictionaryService.findOrCreateParty("partyValue", "PartyType");
            result = party;

        }};

        Assertions.assertNotNull(userMessageDefaultFactory.createPartyInfo(source));
    }

}
