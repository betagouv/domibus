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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
class ConnectionMonitoringHelperEnabledPartiesPropertyTest {

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
        List<String> senderPartyIds = Collections.singletonList("domibus-blue");

        new Expectations() {{
            pModeProvider.findAllParties();
            result = knownParties;
            partyService.findPushToPartyNamesForTest();
            result = testablePartyIds;
            partyService.getGatewayPartyIdentifiers();
            result = senderPartyIds;
        }};
    }

    @ParameterizedTest
    @ValueSource(strings = {"domibus-blue>domibus-red, domibus-blue>domibus-blue",
            "domibus-blue>domibus-blue, domibus-blue>domibus-blue",
            " ,, domibus-blue>domibus-blue  , ",
            "domibus-blue>Domibus-BLUE"})
    void propertyValueChanged_valid(String value) {
        connectionMonitoringHelper.validateEnabledPartiesValue(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"#$%%$^&",
            "<foo val=“bar” />",
            "１２３",
            "domibus-unknowncolor,domibus-blue",
            "domibus-blue,domibus-red,domibus-green",
            "domibus-bluish",
            "domibus-blue2>domibus-red2, domibus-blue>domibus-blue"})
    void propertyValueChanged_notValid(String value) {
        try {
            connectionMonitoringHelper.validateEnabledPartiesValue(value);

            Assertions.fail("[" + value + "] property value shouldn't have been accepted");
        } catch (DomibusPropertyException ex) {
            LOG.info("Exception thrown as expected when trying to set invalid property value: [{}]", value, ex);
        }
    }
}
