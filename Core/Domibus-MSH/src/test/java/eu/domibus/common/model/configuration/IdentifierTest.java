package eu.domibus.common.model.configuration;

import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
public class IdentifierTest {

    @Tested
    Identifier identifier;

    @Test
    public void equals() {
        identifier = new Identifier();
        identifier.setPartyId("domibus-blue");


        Identifier identifier1 = new Identifier();
        identifier1.setPartyId("domibus-BLUE");

        Assertions.assertFalse(identifier.equals(null));
        Assertions.assertTrue(identifier.equals(identifier));

        Assertions.assertTrue(identifier.equals(identifier1));
        Assertions.assertTrue(identifier1.equals(identifier));

        identifier.setPartyId(null);
        Assertions.assertFalse(identifier.equals(identifier1));
    }

    @Test
    public void equalsPartyIdType(@Injectable PartyIdType partyIdType) {
        identifier = new Identifier();
        identifier.setPartyId("domibus-blue");
        partyIdType.setName("partyTypeUrn");
        identifier.setPartyIdType(partyIdType);


        Identifier identifier1 = new Identifier();
        identifier1.setPartyId("domibus-BLUE");

        Assertions.assertFalse(identifier.equals(identifier1));
    }

}
