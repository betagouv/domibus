package eu.domibus.web.rest;

import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.util.DateUtil;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.message.MessagesLogService;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.core.message.testservice.TestServiceException;
import eu.domibus.web.rest.ro.LatestIncomingMessageRequestRO;
import eu.domibus.web.rest.ro.LatestOutgoingMessageRequestRO;
import eu.domibus.web.rest.ro.TestServiceMessageInfoRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class MessageLogResourceTest {

    @Tested
    MessageLogResource messageLogResource;

    @Injectable
    TestService testService;

    @Injectable
    DateUtil dateUtil;

    @Injectable
    MessagesLogService messagesLogService;

    @Injectable
    DomibusConfigurationService domibusConfigurationService;

    @Injectable
    CsvServiceImpl csvServiceImpl;

    @Injectable
    RequestFilterUtils requestFilterUtils;

    @Test
    public void testGetLastTestSent(@Injectable TestServiceMessageInfoRO testServiceMessageInfoResult) throws TestServiceException {
        // Given
        String partyId = "test";
        String senderPartyId = "senderPartyId";
        new Expectations() {{
            testService.getLastTestSentWithErrors(senderPartyId, partyId);
            result = testServiceMessageInfoResult;
        }};

        // When
        ResponseEntity<TestServiceMessageInfoRO> lastTestSent = messageLogResource.getLastTestSent(
                new LatestOutgoingMessageRequestRO() {{
                    setPartyId(partyId);
                    setSenderPartyId(senderPartyId);
                }});
        // Then
        TestServiceMessageInfoRO testServiceMessageInfoRO = lastTestSent.getBody();
        Assertions.assertEquals(testServiceMessageInfoResult.getPartyId(), testServiceMessageInfoRO.getPartyId());
    }

    @Test
    void testGetLastTestSent_NotFound() throws TestServiceException {
        // Given
        String partyId = "partyId";
        String senderPartyId = "senderPartyId";
        new Expectations() {{
            testService.getLastTestSentWithErrors(senderPartyId, partyId);
            result = new TestServiceException("No User Message found. Error Details in error log");
        }};

        // When
        Assertions.assertThrows(TestServiceException. class,() -> messageLogResource.getLastTestSent(
                new LatestOutgoingMessageRequestRO() {{
                    setPartyId(partyId);
                    setSenderPartyId(senderPartyId);
                }}));
    }

    @Test
    public void testGetLastTestReceived(@Injectable TestServiceMessageInfoRO testServiceMessageInfoResult, @Injectable Party party) throws TestServiceException {
        // Given
        String partyId = "partyId";
        String userMessageId = "userMessageId";
        String senderPartyId = "senderPartyId";
        new Expectations() {{
            testService.getLastTestReceivedWithErrors(senderPartyId, partyId, userMessageId);
            result = testServiceMessageInfoResult;
        }};

        // When
        ResponseEntity<TestServiceMessageInfoRO> lastTestReceived = messageLogResource.getLastTestReceived(
                new LatestIncomingMessageRequestRO() {{
                    setPartyId(partyId);
                    setSenderPartyId(senderPartyId);
                    setUserMessageId(userMessageId);
                }});
        // Then
        TestServiceMessageInfoRO testServiceMessageInfoRO = lastTestReceived.getBody();
        Assertions.assertEquals(testServiceMessageInfoRO.getPartyId(), testServiceMessageInfoResult.getPartyId());
        Assertions.assertEquals(testServiceMessageInfoRO.getAccessPoint(), party.getEndpoint());
    }

    @Test
    void testGetLastTestReceived_NotFound() throws TestServiceException {
        // Given
        String partyId = "partyId";
        String senderPartyId = "senderPartyId";
        String userMessageId = "userMessageId";
        new Expectations() {{
            testService.getLastTestReceivedWithErrors(senderPartyId, partyId, userMessageId);
            result = new TestServiceException("Error Details in error log");
        }};

        Assertions.assertThrows(TestServiceException. class,() -> messageLogResource.getLastTestReceived(
                new LatestIncomingMessageRequestRO() {{
                    setPartyId(partyId);
                    setSenderPartyId(senderPartyId);
                    setUserMessageId(userMessageId);
                }}));
    }
}
