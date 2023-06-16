package eu.domibus.web.rest;

import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.api.party.PartyService;
import eu.domibus.core.converter.PartyCoreMapper;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.core.monitoring.ConnectionMonitoringService;
import eu.domibus.core.plugin.handler.MessageSubmitterImpl;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.web.rest.ro.ConnectionMonitorRO;
import eu.domibus.web.rest.ro.TestServiceRequestRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.collections.map.HashedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author Tiago Miguel
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class TestServiceResourceTest {

    @Tested
    TestServiceResource testServiceResource;

    @Injectable
    TestService testService;

    @Injectable
    PartyService partyService;

    @Injectable
    MessageSubmitterImpl messageSubmitter;

    @Injectable
    PModeProvider pModeProvider;

    @Injectable
    ConnectionMonitoringService connectionMonitoringService;

    @Injectable
    PartyCoreMapper partyCoreMapper;

    @Test
    public void testGetTestParties() {
        // Given
        List<String> testPartiesList = new ArrayList<>();
        testPartiesList.add("testParty1");
        testPartiesList.add("testParty2");

        new Expectations() {{
            partyService.findPushToPartyNamesForTest();
            result = testPartiesList;
        }};

        // When
        List<String> testParties = testServiceResource.getTestParties();

        // Then
        Assertions.assertEquals(testPartiesList, testParties);
    }

    @Test
    public void testSubmitTest() throws IOException, MessagingProcessingException {
        // Given
        TestServiceRequestRO testServiceRequestRO = new TestServiceRequestRO();
        testServiceRequestRO.setSender("sender");
        testServiceRequestRO.setReceiver("receiver");

        new Expectations() {{
            testService.submitTest(testServiceRequestRO.getSender(), testServiceRequestRO.getReceiver());
            result = "test";
        }};

        // When
        String submitTest = testServiceResource.submitTest(testServiceRequestRO);

        // Then
        Assertions.assertEquals("test", submitTest);
    }

    @Test
    public void testSubmitTestDynamicDiscoveryMessage() throws IOException, MessagingProcessingException {
        // Given
        TestServiceRequestRO testServiceRequestRO = new TestServiceRequestRO();
        testServiceRequestRO.setSender("sender");
        testServiceRequestRO.setReceiver("receiver");
        testServiceRequestRO.setReceiverType("receiverType");
        testServiceRequestRO.setServiceType("servicetype");

        new Expectations() {{
            testService.submitTestDynamicDiscovery(testServiceRequestRO.getSender(), testServiceRequestRO.getReceiver(), testServiceRequestRO.getReceiverType());
            result = "dynamicdiscovery";
        }};

        // When
        String submitTestDynamicDiscovery = testServiceResource.submitTestDynamicDiscovery(testServiceRequestRO);

        // Then
        Assertions.assertEquals("dynamicdiscovery", submitTestDynamicDiscovery);
    }
}
