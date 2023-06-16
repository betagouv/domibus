package eu.domibus.core.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.party.PartyService;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
// TODO: 14/06/2023 François GAUTIER  @RunWith(Parameterized.class)
@Disabled("EDELIVERY-6896")
public class ConnectionMonitoringHelperEnabledPartiesPropertyTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ConnectionMonitoringHelperEnabledPartiesPropertyTest.class);

    @Tested
    ConnectionMonitoringHelper connectionMonitoringHelper;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    PartyService partyService;

    @Injectable
    PModeProvider pModeProvider;

    @BeforeEach
    public void setupTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonParty1 = "{\"name\":\"blue_gw\", \"identifiers\":[{\"partyId\":\"domibus-blue\",\"partyIdType\":{\"name\":\"partyTypeUrn\"}}, {\"partyId\":\"domibus-bluish\",\"partyIdType\":{\"name\":\"partyTypeUrn2\"}}]}";
        String jsonParty2 = "{\"name\":\"red_gw\", \"identifiers\":[{\"partyId\":\"domibus-red\",\"partyIdType\":{\"name\":\"partyTypeUrn\"}}]}";
        String jsonParty3 = "{\"name\":\"green_gw\", \"identifiers\":[{\"partyId\":\"domibus-green\",\"partyIdType\":{\"name\":\"partyTypeUrn\"}}]}";

        Party party1 = mapper.readValue(jsonParty1, Party.class);
        Party party2 = mapper.readValue(jsonParty2, Party.class);
        Party party3 = mapper.readValue(jsonParty3, Party.class);

        List<Party> knownParties = Arrays.asList(party1, party2, party3);
        List<String> testablePartyIds = Arrays.asList("domibus-blue", "domibus-red");
        List<String> senderPartyIds = Arrays.asList("domibus-blue");

        new Expectations() {{
            pModeProvider.findAllParties();
            result = knownParties;
            partyService.findPushToPartyNamesForTest();
            result = testablePartyIds;
            partyService.getGatewayPartyIdentifiers();
            result = senderPartyIds;
        }};
    }

  //  @Parameterized.Parameter(0)
    public String value;

  //  @Parameterized.Parameter(1)
    public boolean valid;

    //todo fga @Parameterized.Parameters(name = "Test setting propertyValue=\"{0}\"")
    public static Collection<Object[]> testedValues() {
        return Arrays.asList(new Object[][]{
                // invalid values :
                {"#$%%$^&", false},
                {"<foo val=“bar” />", false},
                {"１２３", false},
                {"domibus-unknowncolor,domibus-blue", false},
                {"domibus-blue,domibus-red,domibus-green", false},
                {"domibus-bluish", false},
                {"domibus-blue2>domibus-red2, domibus-blue>domibus-blue", false},
                // valid values :
                {"domibus-blue>domibus-red, domibus-blue>domibus-blue", true},
                {"domibus-blue>domibus-blue, domibus-blue>domibus-blue", true},
                {" ,, domibus-blue>domibus-blue  , ", true},
                {"domibus-blue>Domibus-BLUE", true},
        });
    }

    @Test
    public void propertyValueChanged() {
        try {
            connectionMonitoringHelper.validateEnabledPartiesValue(value);

            if (!valid) {
                Assertions.fail("[" + value + "] property value shouldn't have been accepted");
            }
        } catch (DomibusPropertyException ex) {
            if (!valid) {
                LOG.info("Exception thrown as expected when trying to set invalid property value: [{}]", value, ex);
            } else {
                LOG.error("Unexpected exception thrown when trying to set valid property value: [{}]", value, ex);
                Assertions.fail("[" + value + "] property value should have been accepted");
            }
        }
    }
}
