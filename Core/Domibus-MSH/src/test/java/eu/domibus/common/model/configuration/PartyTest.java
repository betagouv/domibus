package eu.domibus.common.model.configuration;

import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class PartyTest {

    @Test
    public void testEqualsIsCaseInsensitive() {
        Party party1 = createParty("POP000004");
        Party party2 = createParty("pop000004");
        Assertions.assertEquals(party1, party2);
    }

    private Party createParty(String partyName) {
        Party party1 = new Party();
        party1.setName(partyName);
        final Identifier identifier = new Identifier();
        identifier.setPartyId(partyName);

        final String partyTypeValue = "urn:fdc:peppol.eu:2017:identifiers:ap";
        PartyIdType partyType = new PartyIdType();
        partyType.setValue(partyTypeValue);
        identifier.setPartyIdType(partyType);
        party1.getIdentifiers().add(identifier);
        return party1;
    }


}
