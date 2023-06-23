package eu.domibus.common.model.configuration;

import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Ebms3PartyIdTypeTest {

    @Tested
    PartyIdType partyIdType;

    @Test
    public void equals() {
        partyIdType = new PartyIdType();
        partyIdType.setName("partyTypeUrn");


        PartyIdType partyIdType1 = new PartyIdType();
        partyIdType1.setName("PARTYTYPEURN");

        Assertions.assertFalse(partyIdType.equals(null));
        Assertions.assertTrue(partyIdType.equals(partyIdType1));
        Assertions.assertTrue(partyIdType1.equals(partyIdType));
        Assertions.assertTrue(partyIdType.equals(partyIdType));
        partyIdType.setName(null);
        Assertions.assertFalse(partyIdType.equals(partyIdType1));
    }

}
